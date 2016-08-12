#!/usr/bin/python -u
# 2010/03/08
# v0.1
#
# controller.py
# A reference implementation of VOSpace 2.0

import cgi
import cherrypy
from cherrypy.lib.static import serve_file
from datetime import datetime
import sys, string, os, re, errno, time, stat
from os import path, environ
from socket import gethostbyaddr, gethostname, gethostbyname
from string import Template
from subprocess import Popen, PIPE
from time import localtime, strftime, asctime, gmtime
from urllib import unquote

from admin import NodeManager, TransferManager, JobManager
from config import *
from datetime import datetime
from store import LocalStoreManager
import thread
from time import sleep
import uuid
from resources import *

try:
  from lxml import etree
  print("running with lxml.etree")
except ImportError:
  try:
    # Python 2.5
    import xml.etree.cElementTree as etree
    print("running with cElementTree on Python 2.5+")
  except ImportError:
    try:
      # Python 2.5
      import xml.etree.ElementTree as etree
      print("running with ElementTree on Python 2.5+")
    except ImportError:
      try:
        # normal cElementTree install
        import cElementTree as etree
        print("running with cElementTree")
      except ImportError:
        try:
          # normal ElementTree install
          import elementtree.ElementTree as etree
          print("running with ElementTree")
        except ImportError:
          print("Failed to import ElementTree from any known place")

# ----------------------------------------------------------
def noBodyProcess():
  cherrypy.request.body = cherrypy.request.rfile
  cherrypy.request.process_request_body = False

cherrypy.tools.noBodyProcess = cherrypy.Tool('before_request_body', 
                                             noBodyProcess)
# ----------------------------------------------------------
class Mapper():
  """
  Root class for VOSpace service
  """
  exposed = True
  _cp_config = {'tools.noBodyProcess.on': True}

  def __init__(self, resource = 'nodes'):
    self.resource = resource
    self.nf = NodeFactory()
    self.sm = LocalStoreManager()
    self.nm = NodeManager(self.sm)
    self.jm = JobManager(self.sm)
    self.tm = TransferManager(self.sm, self.nm, self.jm)
    thread.start_new_thread(self._check_transfers, (THREAD_POLLING_FREQ,))

  def _check_transfers(self, delay):
    """
    Check the transfers list every specified delay
    """
    while 1:
      sleep(delay)
      self.jm.check_jobs()

  def GET(self, *args, **kwargs):
    """
    Respond to HTTP GET requests
    """
    if not self._check_user(args): raise VOSpaceError(401, "User does not have permissions to perform the operation.", summary = PERMISSION_DENIED)
    try:
      resource = args[0]
      if resource == 'protocols':
        return self._get_protocols()
      elif resource == 'views':
        return self._get_views()
      elif resource == 'properties':
        return self._get_properties()
      elif resource == 'nodes':
        if 'view' in kwargs and kwargs['view'] == 'data':
          location = self.sm.get_node_location(ROOT_NODE + "/" + "/".join(args[1:]))
          if len(location) == 0: raise VOSpaceError(404, 'A Node does not exist with the requested URI.')
          return serve_file(location[0]['location'])
        else:
          return self._get_node(args[1:], kwargs)
      elif resource == 'transfers':
        return self._get_transfers(args[1:])
      elif resource == 'searches':
        return self._get_searches(args[1:])
      elif resource == 'data':
        if not self._check_endpoint(args[1]):
          raise cherrypy.HTTPError(404)
        else:
          location = self._get_location(args[1])
          self._complete_transfer(args[1])
          # Put in switch for correct content type based on view
          return serve_file(location)
      else:
        raise cherrypy.HTTPError(404)
    except VOSpaceError, e:
      raise cherrypy.HTTPError(e.code, e.value)

  def PUT(self, *args, **kwargs):
    """
    Respond to HTTP PUT requests
    """
    if not self._check_user(args): raise VOSpaceError(401, "User does not have permissions to perform the operation.", summary = PERMISSION_DENIED)
    resource = args[0]
    if resource == 'nodes':
      try:
        # Check endpoint does not exist
        if self._check_exists(args[1:]): raise VOSpaceError(409, "A Node already exists with the requested URI.", summary = DUPLICATE_NODE)
        dataLength = int(cherrypy.request.headers.get('Content-Length') or 0)
        data = cherrypy.request.rfile.read(dataLength)
        xmldata = etree.fromstring(data)
        # Check node URI and endpoint agree
        if xmldata.get("uri") != ROOT_NODE + '/' + '/'.join(args[1:]): raise VOSpaceError(500, "A specified URI is invalid", summary = INVALID_URI)
        node = self._create_node(xmldata)
        cherrypy.response.status = 201
        return etree.tostring(xmldata)
      except VOSpaceError, e:
        raise cherrypy.HTTPError(e.code, e.value)
      except SyntaxError, e:
        print e
        raise cherrypy.HTTPError(500)
    elif resource == 'data':
      # File upload
      if not self._check_endpoint(args[1]):
        raise cherrypy.HTTPError(404)
      else:
        location = self._get_location(args[1])
        success = SERVER_PROTOCOLS['ivo://ivoa.net/vospace/core#httpput'].manage_file(location, cherrypy.request)
        if not success: raise cherrypy.HTTPError(500)
    else:
      raise cherrypy.HTTPError(404)

  def POST(self, *args, **kwargs):
    """
    Respond to HTTP POST requests
    """
    if not self._check_user(args): raise VOSpaceError(401, "User does not have permissions to perform the operation.", summary = PERMISSION_DENIED)
    try:
      resource = args[0]
      dataLength = int(cherrypy.request.headers.get('Content-Length') or 0)
      data = cherrypy.request.rfile.read(dataLength)
      # XML request
      if data[0] == '<':
        request = etree.fromstring(data)
        if resource == 'transfers' and len(args) == 1:
          jobid = self._create_transfers(request)
          raise cherrypy.HTTPRedirect("%s/transfers/%s" % (HOST, jobid))
        elif resource == 'nodes':
          if 'protocol' in data:
            return self._get_protocol(args)
          else:
            return self._update_node(args, request)
        elif resource == 'searches' and len(args) == 1:
          jobid = self._find_nodes(request)
          raise cherrypy.HTTPRedirect("%s/searches/%s" % (HOST, jobid))
        elif resource == 'sync' and len(args) == 1:
          jobid = self._create_transfers(request, run = True)
          results = self.sm.get_results(jobid)
          raise cherrypy.HTTPRedirect("%s/transfers/%s/results/details" % (HOST, jobid))
        else:
          raise cherrypy.HTTPError(404)
      # PHASE= request
      elif data[0:5] == 'PHASE' and 'phase' in args:
        phase = data.split('=')[1]
        if resource == 'transfers':
          self._handle_transfers(args[1], phase)
          raise cherrypy.HTTPRedirect("%s/transfers/%s" % (HOST, args[1]))
        elif resource == 'searches':
          raise cherrypy.HTTPRedirect("%s/searches/%s" % (HOST, args[1]))
      # ACTION= request
      elif data[0:6] == 'ACTION' and len(args) == 1:
        action = data.split('=')[1]
        if resource == 'transfers':
          raise cherrypy.HTTPRedirect("%s/transfers" % HOST)
        elif resource == 'searches':
          raise cherrypy.HTTPRedirect("%s/searches" % HOST)
        else:
          raise cherrypy.HTTPError(404)
      else:
        raise cherrypy.HTTPError(404)
    except VOSpaceError, e:
      raise cherrypy.HTTPError(e.code, e.value)
#    except SyntaxError:
#      raise cherrypy.HTTPError(500)
    
  def DELETE(self, *args, **kwargs):
    """
    Respond to HTTP DELETE requests
    """
    if not self._check_user(args): raise VOSpaceError(401, "User does not have permissions to perform the operation.", summary = PERMISSION_DENIED)
    try:
      resource = args[0]
      # Node
      if resource == 'nodes':
        uri = ROOT_NODE + "/" + args[1]
        self.nm.delete_node(uri)
      # Transfer
      elif resource == 'transfers':
        raise cherrypy.HTTPRedirect("%s/transfers" % HOST)
      # Search
      elif resource == 'searches':
        raise cherrypy.HTTPRedirect("%s/searches" % HOST)
    except VOSpaceError, e:
      raise cherrypy.HTTPError(e.code, e.value)
    except SyntaxError:
      raise cherrypy.HTTPError(500)

  def _get_protocol(self, args):
    """
    Return the list of supported protocols for the specified node
    """
    # Specific protocols can be added here for specific nodes
    return self._get_protocols()

  def _get_protocols(self):
    """
    Return the list of supported protocols
    """
    baseResponse = '''<protocols xmlns="%s">\n %s %s</protocols>\n'''
    accepts = '<accepts>\n'
    for prop in SERVER_PROTOCOLS:
      accepts += '''<protocol uri="%s"/>\n''' % prop
    accepts += '</accepts>\n'
    provides = '<provides>\n'
    for prop in CLIENT_PROTOCOLS:
      provides += '''<protocol uri="%s"/>\n''' % prop
    provides += '</provides>\n'
    return baseResponse % (VOSPACE_NS, accepts, provides)

  def _get_properties(self):
    """
    Return the list of supported properties
    """
    baseResponse = '''<properties>\n %s %s %s</properties>\n'''
    accepts = '<accepts>\n'
    for prop in ACCEPTS_PROPERTIES:
      accepts += '''<property uri="%s"/>\n''' % prop
    accepts += '</accepts>\n'
    provides = '<provides>\n'
    for prop in PROVIDES_PROPERTIES:
      provides += '''<property uri="%s"/>\n''' % prop
    provides += '</provides>\n'
    props = self.sm.get_properties()
    contains = '<contains>\n'
    for prop in props:
      contains += '''<property uri="%s"/>\n''' % prop[0]
    contains += '</contains>\n'
    return baseResponse % (accepts, provides, contains)

  def _get_views(self):
    """
    Return the list of supported views
    """
    baseResponse = '''<views>\n %s %s</views>'''
    accepts = '<accepts>\n'
    for view in SERVICE_VIEWS:
      accepts += '''<view uri="%s"/>\n''' % view
    accepts += '</accepts>\n'
    provides = '<provides>\n'
    allviews = [set(PROVIDES_VIEWS[x]) for x in PROVIDES_VIEWS]
    allviews = set.union(*allviews)
    for view in list(allviews):
      provides += '''<view uri="%s"/>\n''' % view
    provides += '</provides>\n'
    return baseResponse % (accepts, provides)
    
  def _create_node(self, xmldata):
    """
    Create a node from the specified data
    """
    node = self.nm.create_node(xmldata)
    return node

  def _get_node(self, args, kwargs):
    """
    Get the specified node
    """
    uri = len(args) > 0 and (ROOT_NODE + "/" + "/".join(args)) or ROOT_NODE
    res = self.sm.get_node(uri)
    if len(res) > 0:
      node = self.nf.get_node(res[0]['node'])
      if node.TYPE == 'vos:ContainerNode':
        children = self.sm.get_children(uri)
        if 'uri' in kwargs:
          match = False
          count = 0
          for child in children:
            if child == kwargs['uri']: match = True
            if match and count < int(kwargs['offset']):
              node.add_node(child)
              count += 1
        else:
          for child in children:
            node.add_node(child)
      if 'detail' in kwargs:
        return node.tostring(detail = kwargs['detail'])
      else:
        return node.tostring()
    else:
      raise VOSpaceError(404, "The specified node does not exist.") 

  def _update_node(self, args, request):
    """
    Update the specified node with the provided details
    """
    node = self.nm.update_node(ROOT_NODE + "/" + "/".join(args[1:]), request)
    return node

  def _get_transfers(self, args):
    """
    Get the list of transfers
    """
    if 'results' in args:
      if 'details' in args:
        results = self.sm.get_results(args[0])
        xml = len(results) > 0 and results[0]['details'] or ''
      else:
        xml = self.job.getResultsAsXml()
      return xml
    elif 'phase' in args:
      phase = self.sm.get_phase(args[0])[0]['phase']
      return phase
    else:
      if len(args) > 0:
        job = self.sm.get_job(args[0])[0]['job']
      else:
        job = self.jm.get_jobs(type = 'transfers')
      return job

  def _create_transfers(self, transfer, run = False):
    """
    Create the specified transfer
    """
    if not(self._parse_transfer(transfer)): raise VOSpaceError()
    jobid = self.tm.add_transfer(transfer, run)
    return jobid

  def _handle_transfers(self, id, phase):
    """
    Manage the specified transfer
    """
    res = self.sm.get_job(id)
    if len(res) > 0:
      job = Job(res[0]['job'])
      if phase == 'RUN':
        if job.phase == 'PENDING': job.set_phase('QUEUED')
      elif phase == 'ABORT':
        job.set_phase('ABORTED')
      self.sm.update_job(job = job)
    else:
      raise VOSpaceException(404, 'The specified transfer does not exist.')

  def _parse_transfer(self, transfer):
    """
    Check the syntax of the transfer resource
    """
    try:
      transfer = Transfer(transfer)
    except:
      return False
    return True
    
  def _get_searches(self, args):
    """
    Get the list of searches
    """
    if 'results' in args:
      xml = etree.parse('results.xml')
      return etree.tostring(xml)
    else:
      job = self.sm.get_job(args[0])[0]['job']
      return job

  def _find_nodes(self, search):
    """
    Search for the specified nodes
    """
    self.job = Job(search)
    self.job.set_start_time(datetime.utcnow().isoformat())
    self.job.set_phase('COMPLETED')
    jobid = self._get_job_id()
    self.job.set_job_id(jobid)
    self.job.add_result('searchDetails', 'http://localhost:8000/searches/%s/results/listing1' % jobid)
    self.job.set_end_time(datetime.utcnow().isoformat())
    return jobid

  def _check_endpoint(self, uri):
    """
    Check that the file endpoint is valid: it is unique and has not expired
    """
    result = self.sm.get_endpoint(uri)
    if len(result) != 1: return False
    if result[0]['completed'] is not None: return False
    if (datetime.now() - result[0]['created']).seconds / 3600 > 0: return False
    return True

  def _get_location(self, endpoint):
    """
    Get the physical location corresponding to the transfer endpoint
    """
    result = self.sm.get_endpoint(endpoint)
    jobid = result[0]['jobid']
    job = self.sm.get_transfer(jobid)
    transfer = Transfer(job.jobInfo)
    location = self.sm.get_location(transfer.target)
    return location[0]['location']

  def _complete_transfer(self, endpoint):
    """
    Set the completion time on the transfer
    """
    result = self.sm.get_endpoint(endpoint)
    jobid = result[0]['jobid']
    self.sm.complete_transfers(jobid)

  def _get_job_id(self):
    """
    Create a globally unique identifier for the job
    """
    return uuid.uuid4().hex

  def _check_user(self, args):
    """
    Check if the user is valid
    """
    return True

  def _check_exists(self, args):
    """
    Check whether the specified node exists
    """
    return self.sm.already_exists('nodes', ROOT_NODE + '/' + '/'.join(args))    


# Expose objects --------------------------------------------
root = Mapper()

if __name__ == '__main__':
  cherrypy.config.update({'environment': 'production',
			  'server.socket_host': '0.0.0.0',
			  'server.socket_port': PORT,
                          'log.error_file': 'site.log',
                          'log.screen': True})
  conf = {"/": {"tools.staticdir.root": BASE_DIR},
          "/scripts": {"tools.staticdir.on": True,
                       "tools.staticdir.dir": "scripts"},
          "/protocols": {"request.dispatch": cherrypy.dispatch.MethodDispatcher()},
          "/views": {"request.dispatch": cherrypy.dispatch.MethodDispatcher()},
          "/properties": {"request.dispatch": cherrypy.dispatch.MethodDispatcher()},
          "/transfers": {"request.dispatch": cherrypy.dispatch.MethodDispatcher()},
          "/searches": {"request.dispatch": cherrypy.dispatch.MethodDispatcher()},
          "/nodes": {"request.dispatch": cherrypy.dispatch.MethodDispatcher()},
          "/data": {"request.dispatch": cherrypy.dispatch.MethodDispatcher()},
          "/sync": {"request.dispatch": cherrypy.dispatch.MethodDispatcher()}
          }
  cherrypy.quickstart(root, config = conf)

