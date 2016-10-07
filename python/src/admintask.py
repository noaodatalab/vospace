# 

from syncVOSpace import Sync
from store import LocalStoreManager
from node import NodeFactory
import config as cfg
from lineparser import Task, Option
from dateutil import parser 
from dateutil.relativedelta import relativedelta
import os
import pytz
from string import Template
from lxml import etree
import requests

utc = pytz.UTC

containerTemp = '''<node xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="vos:ContainerNode" uri="$uri" xmlns="http://www.ivoa.net/xml/VOSpace/v2.0" busy="false"><properties><property readOnly="false" uri="ivo://ivoa.net/vospace/core#ispublic">$ispublic</property><property readOnly="false" uri="ivo://ivoa.net/vospace/core#groupread">$group</property><property readOnly="false" uri="ivo://ivoa.net/vospace/core#groupwrite">$group</property></properties><accepts/><provides/><capabilities/><nodes/></node>'''


def registerNode(vosurl, token, node, location):
    '''Register a node with the VOSpace service '''
    node = node.replace('~', '!')
    url = "%s?location=%s" % (vosurl, location)
    r = requests.put(url, data = node, headers = {'X-DL-AuthToken': token, 'Content-type': 'application/xml'})
    if r.status_code != 201:
      print("Error registering %s" % location)
    else:
      print("%s registered" % location)
#    if resp['status'] != 201:
#      print 'Error with node %s' % node
#      sys.exit(-1)

      
class Init(Task):
  '''Initialize a VOSpace'''
  def __init__(self, admin):
    Task.__init__(self, admin, 'init', 'initialize a VOSpace')
    self.addOption('token', Option('token', '', 'security token to access VOSpace', required = True))
    self.addOption('basedir', Option('basedir', '', 'base directory for VOSpace', required = True))

  def run(self):
    # Create users directory (with mode 755)
    usersdir = "%s/users" % self.basedir 
    if not(os.path.exists(usersdir)):
      os.mkdir(usersdir)
    # Create space working directory (with mode 755)
    tmpdir = "%s/tmp" % self.basedir
    if not(os.path.exists(tmpdir)):
      os.mkdir(tmpdir)

    print("VOSpace initialized")

class AddUser(Task):
  '''Add a user to the VOSpace'''
  def __init__(self, admin):
    Task.__init__(self, admin, 'adduser', 'add a user on the server side to VOSpace')
    self.addOption('token', Option('token', '', 'security token to access VOSpace', required = True))
    self.addOption('basedir', Option('basedir', '', 'base directory for VOSpace', required = True))
    self.addOption('vosroot', Option('vosroot', '', 'root node of VOSpace', required = True, default = 'vos://datalab.noao.edu!vospace'))
    self.addOption('vosurl', Option('vosurl', '', 'VOSpace endpoint', required = True, default = 'http://localhost:8080/vospace-2.0/vospace'))
    self.addOption('name', Option('name', '', 'user name', required = True))
    self.addOption('withdata', Option('withdata', '', 'path to contents to load into user area', required = False))
    self.addOption('quota', Option('quota', '', 'quota for user in VOSpace', required = False))
    self.container = Template(containerTemp)

  def run(self):
    # Create user directory with mode 700
    userdir = "%s/users/%s" % (self.basedir, self.name)
    if not(os.path.exists(userdir)):
      os.mkdir(userdir)
      uri = '%s/%s' % (self.vosroot, self.name)
      url = '%s/register/%s' % (self.vosurl, self.name)
      conf = {'uri': uri, 'ispublic': 'false', 'group': ''}
      node = self.container.safe_substitute(conf)
      registerNode(url, self.token, node, userdir)
    else:
      print "User directory %s already exists" % userdir

    # Create user working directory with mode 700
    tmpdir = "%s/users/%s/tmp" % (self.basedir, self.name)
    if not(os.path.exists(tmpdir)):
      os.mkdir(tmpdir)
      uri = '%s/%s/tmp' % (self.vosroot, self.name)
      url = '%s/register/%s/tmp' % (self.vosurl, self.name)
      conf = {'uri': uri, 'ispublic': 'false', 'group': ''}
      node = self.container.safe_substitute(conf)
      registerNode(url, self.token, node, tmpdir)
    else:
      print "User working directory %s already exists" % tmpdir
    
    # Create user public directory with mode 755
    pubdir = "%s/users/%s/public" % (self.basedir, self.name)
    if not(os.path.exists(pubdir)):
      os.mkdir(pubdir)
      uri = '%s/%s/public' % (self.vosroot, self.name)
      url = '%s/register/%s/public' % (self.vosurl, self.name)
      conf = {'uri': uri, 'ispublic': 'true', 'group': self.name}
      node = self.container.safe_substitute(conf)
      registerNode(url, self.token, node, pubdir)
    else:
      print "User public directory %s already exists" % pubdir

    # Loop over tree if appropriate
    if self.withdata != '':
      pass
      
      
class Validate(Task):
  '''Validate metadata db from disk truth'''
  def __init__(self, admin):
    Task.__init__(self, admin, 'validate', 'validate the contents of the metadata db against disk truth')
    self.addOption('db', Option('db', '', 'JDBC URI for metadata db', required = False))
    self.addOption('start', Option('start', '', 'starting VOSpace node', required = True))
    self.addOption('fix', Option('fix', False, 'correct metadata if wrong, required = False', default = False))
    
  def run(self):
    sync = Sync([])
    nf = NodeFactory()
    store = LocalStoreManager()
    # Get data from metadata db
    start = self.start.value.replace('~', '!') # Both characters are allowed
    query = "select identifier, location, node from nodes where identifier like '%s%%' order by identifier" % start
    res = store.query(query)
    # Check through nodes under starting point
    for record in res:
      vosid = record['identifier']
      location = record['location']
      # Get file metadata
      meta = sync.getMetadata(location[7:]) # Remove file:// scheme
      # File existence
      if len(meta) == 0:
        print("The file %s is missing" % location)
        if self.fix.value: # Resolve by deleting record
          store.delete_node(vosid)
          continue
      node = nf.get_node(etree.fromstring(record['node']))
      # File size
      if cfg.LENGTH not in node.properties:
        print("The file size is missing for: %s" % vosid)
      elif node.properties[cfg.LENGTH] != meta['size']:
        print("The sizes for %s and %s do not match" % (vosid, location))
        if self.fix.value:
          node.properties[cfg.LENGTH] = meta['size']
          store.update_node(vosid, node, vosid)
      # Date
      if relativedelta(parser.parse(node.properties[cfg.DATE]) - utc.localize(parser.parse(meta['date']))).seconds > 1: # Current tolerance is 1s.
        print("The dates for %s and %s do not match: %s %s" % (vosid, location, node.properties[cfg.DATE], meta['date']))
        if self.fix.value:
          node.properties[cfg.DATE] = meta['size']
          store.update_node(vosid, node, vosid)
        
        
      

class Register(Task):
  '''Backend file registration'''
  def __init__(self, admin):
    Task.__init__(self, admin, 'register', 'register a file with VOSpace')

  def run(self):
    pass

