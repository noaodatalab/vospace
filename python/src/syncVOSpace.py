import os
import time
import pwd
from string import Template
import sys
from httplib2 import Http
from urlparse import urlparse
import config as cfg

nodeTemp = '''<node xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="$type" xmlns="http://www.ivoa.net/xml/VOSpace/v2.0" uri="$uri" busy="false"><properties><property uri="ivo://ivoa.net/vospace/core#date">$date</property><property readOnly="false" uri="ivo://ivoa.net/vospace/core#ispublic">true</property><property readOnly="false" uri="ivo://ivoa.net/vospace/core#groupread">NONE</property><property readOnly="false" uri="ivo://ivoa.net/vospace/core#groupwrite">NONE</property><property readOnly="true" uri="ivo://ivoa.net/vospace/core#quota">5000000</property><property uri="ivo://ivoa.net/vospace/core#MD5">$md5</property><property uri="ivo://ivoa.net/vospace/core#length">$size</property></properties></node>'''


class Sync():
  ''' A class to represent filespace synchronization activities '''

  def __init__(self, args):
    opt = self.parseArgs(args)
    self.rootnode = 'rootnode' in opt and opt['rootnode'] or 'vos://'
    self.host = 'host' in opt and opt['host'] or 'http://localhost:8080'
    self.md5 = 'md5' in opt and opt['md5'] or False
    self.path = 'path' in opt and opt['path'] or os.path.abspath('.')
    self.ignore = 'ignore' in opt and [f for f in open(opt['ignore']).read()] or []
    self.node = Template(nodeTemp)
    self.store = self.host[:4] == 'jdbc' and getStore(self.host) or None

    
  def getStore(self, url):
    parts = url.strip().split(':')
    dbtype = parts[1]
    host = parts[2][2:]
    port = parts[3][:parts[3].index('/')]
    dbname = parts[3][parts[3].index('/') + 1 : parts[3].index('?')]
    pairs = parts[3][parts[3].index('?') + 1:].split('&')
    params = {}
    for pair in pairs:
      key, value = pair.split('=')
      params[key] = value
    if dbtype == 'mysql':
      connection = PySQLPool.getNewConnection(host = "%s:%s" % (host, port), user = params['user'], password = params['password'], schema = dbname)
      store = PySQLPool.getNewQuery(connection = connection, commitOnEnd = True)
    elif dbtype == 'postgres':
      conf = {'database': dbname, 'user': params['user'], 'password': params['password'], 'host': host, 'port': port}
      conn = psycopg2.connect(**conf)
      store = conn.cursor()
    return store
    

  def parseArgs(self, args):
    ''' Parse the user-provided arguments '''
    opt = {}
    for i in range(1, len(args)):
      parts = args[i].strip().split("=")
      name = parts[0][0] == "-" and parts[0][2:] or parts[0]
      opt[name] = parts[1]
    return opt

  
  def getMetadata(self, file):
    ''' Get the metadata for the file from the local filesystem '''
    meta = {}
    s = os.stat(file)
    meta['date'] = time.strftime('%Y-%m-%dT%H:%M:%S', time.gmtime(s.st_mtime))
    meta['size'] = s.st_size
    meta['user'] = pwd.getpwuid(s.st_uid).pw_name
    meta['location'] = file
    return meta

  
  def getUri(self, name):
    ''' Return the VOSpace identifier associated with a file/dir '''
    vosid = self.rootnode + '/' + name
    return vosid
    

  def postService(self, node):
    ''' Post a create node call to the VOSpace service '''
    h = Http()
    resp, content = h.request(self.host, 'PUT', body = node, headers = {'Content-type': 'application/xml'})
    if resp['status'] != 201:
      print 'Error with node %s' % node
      sys.exit(-1)
                              

  def insertDb(self, node, conf):
    ''' Insert the node into the VOSpace metastore '''
    query = '''insert into nodes (identifier, type, view, status, owner, location, creationDate, node) values('%s', %s, '%s', '%s', '%s', '%s', now(), '%s')''' % (conf['uri'], cfg.NODESTYPES[conf['type']], '', 0, conf['owner'], conf['location'], node)
    resp = self.store.query(query)
    if resp != 1:
      print 'Error with node %s' % node
      sys.exit(-1)

          
  def register(self, name, type):
    ''' Register the file/dir with the VOSpace '''
    conf = self.getMetadata(name)
    conf['type'] = type
    conf['uri'] = self.getUri(name)
    conf['md5'] = self.md5 and getmd5(name) or ''
    node = self.node.safe_substitute(conf)
    print self.host
    if self.host[:4] == 'http':
      self.postService(node)
    elif self.host[:4] == 'jdbc':
      self.insertDb(node, conf)
    else:
      print conf, node
    
    
  def run(self):
    ''' Run the data synchronizer '''
    for root, dirs, files in os.walk(self.path, topdown = True):
      dirs[:] = [d for d in dirs if d not in self.ignore and not d[0] == '.']
      files[:] = [f for f in files if f not in self.ignore and not f[0] == '.']
      for name in dirs: # Do directories first
        self.register(os.path.join(root, name), 'vos://ContainerNode')
      for name in files: # then files
        self.register(os.path.join(root, name), 'vos://DataNode')

  
if __name__ == "__main__":
  if len(sys.argv) > 1:
    sync = Sync(sys.argv)
    sync.run()

