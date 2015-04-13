#!/usr/bin/python -u
# 2010/03/11
# v0.1
#
# config.py
# Configurables

from protocols import *

# Server
PORT = 8000
HOST_URL = 'http://localhost'
HOST = '%s:%s' % (HOST_URL, PORT)
BASE_DIR = '/Users/mjg/Projects/noao/vospace/vospace-2.0/python/site'

# Namespaces
UWS_NS = 'http://www.ivoa.net/xml/UWS/v1.0'
VOSPACE_NS = 'http://www.ivoa.net/xml/VOSpace/v2.0'
XLINK_NS = 'http://www.w3.org/1999/xlink'
XSI_NS = 'http://www.w3.org/2001/XMLSchema-instance'

# VOSpace
ROOT_NODE = 'vos://nvo.caltech!vospace'

# Reserved URIs
AUTO = '.auto'
NULL = '.null'

# Node types
NODE = 1
DATA_NODE = 2
CONTAINER_NODE = 3
UNSTRUCTURED_NODE = 4
STRUCTURED_NODE = 5
LINK_NODE = 6
NODETYPES = {'vos:Node': NODE, 'vos:DataNode': DATA_NODE, 'vos:ContainerNode': CONTAINER_NODE, 'vos:UnstructuredDataNode': UNSTRUCTURED_NODE, 'vos:StructuredDataNode': STRUCTURED_NODE, 'vos:LinkNode': LINK_NODE}

# Views
ANY_VIEW = 'ivo://ivoa.net/vospace/core#anyview'
BINARY_VIEW = 'ivo://ivoa.net/vospace/core#binaryview'
DEFAULT_VIEW = 'ivo://ivoa.net/vospace/core#defaultview'
VOTABLE_VIEW = 'ivo://ivoa.net/vospace/core#votable'
SERVICE_VIEWS = [VOTABLE_VIEW]
PROVIDES_VIEWS = {VOTABLE_VIEW: [VOTABLE_VIEW]} # Indicate which views will be provided based on what is accepted

# Properties
TITLE = 'ivo://ivoa.net/vospace/core#title'
CREATOR = 'ivo://ivoa.net/vospace/core#creator'
SUBJECT = 'ivo://ivoa.net/vospace/core#subject'
DESCRIPTION = 'ivo://ivoa.net/vospace/core#description'
PUBLISHER = 'ivo://ivoa.net/vospace/core#publisher'
CONTRIBUTOR = 'ivo://ivoa.net/vospace/core#contributor'
DATE = 'ivo://ivoa.net/vospace/core#date'
TYPE = 'ivo://ivoa.net/vospace/core#type'
FORMAT = 'ivo://ivoa.net/vospace/core#format'
IDENTIFIER = 'ivo://ivoa.net/vospace/core#identifier'
SOURCE = 'ivo://ivoa.net/vospace/core#source'
LANGUAGE = 'ivo://ivoa.net/vospace/core#language'
RELATION = 'ivo://ivoa.net/vospace/core#relation'
COVERAGE = 'ivo://ivoa.net/vospace/core#coverage'
RIGHTS = 'ivo://ivoa.net/vospace/core#rights'
AVAILABLE_SPACE = 'ivo://ivoa.net/vospace/core#availableSpace'
ACCEPTS_PROPERTIES = [DESCRIPTION]
PROVIDES_PROPERTIES = [AVAILABLE_SPACE]  
READ_ONLY_PROPERTIES = [AVAILABLE_SPACE]

# Transfers
TRANSFER_ENDPOINT = 'http://localhost:8000/data'
STORAGE_LOCATION = '/Users/mjg/Projects/noao/vospace/vospace-2.0/python/data'
hh = HttpHandler(TRANSFER_ENDPOINT, STORAGE_LOCATION)
CLIENT_PROTOCOLS = {'ivo://ivoa.net/vospace/core#httpget': hh, 'ivo://ivoa.net/vospace/core#httpput': hh}
SERVER_PROTOCOLS = {'ivo://ivoa.net/vospace/core#httpget': hh, 'ivo://ivoa.net/vospace/core#httpput': hh}

# Jobs
THREAD_POLLING_FREQ = 1

# Persistence store
class CONFIG(object):

  HOST = 'localhost'
  DATABASE = 'vospace'
  USER = 'dba'
  PASSWORD = 'dba'
  PORT = 3306
  
  CHARSET = 'utf8'
  UNICODE = True
  WARNINGS = True

  BUFFERED = True
  
  @classmethod
  def dbinfo(cls):
    return {'host': cls.HOST, 'database': cls.DATABASE, 'user': cls.USER, 'password': cls.PASSWORD, 'charset': cls.CHARSET, 'use_unicode': cls.UNICODE, 'get_warnings': cls.WARNINGS, 'buffered': cls.BUFFERED,}

# Error summaries
INTERNAL_FAULT = "Internal Fault"
PERMISSION_DENIED = "Permission Denied"
NODE_NOT_FOUND = "Node Not Found"
DUPLICATE_NODE = "Duplicate Node"
INVALID_URI = "Invalid URI"
MISSING_PARAMETER = "Missing Parameter"
VIEW_NOT_SUPPORTED = "View Not Supported"
PROTOCOL_NOT_SUPPORTED = "Protocol Not Supported"

# VOSpaceError
class VOSpaceError(Exception):
  """
  A class to represent VOSpace-specific errors.
  """

  def __init__(self, code, value, summary = None):
      self.code = code
      self.value = value
      if type != None: self.summary = summary

  def __str__(self):
      return repr(self.value)

