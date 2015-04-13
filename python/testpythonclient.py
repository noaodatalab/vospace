#!/usr/bin/python -u
# 2010/04/21 - v0.1: Original version
# 2014/10/24 - v0.2: Amended to be generic VOSpace test client - use schema validation
#                    instead of direct XML document matching
#
# testclient.py
# Python code to (unit) test VOSpace operations - note that this does not test data transfer
# per se but just the exchanges with the service to mediate this

from datetime import datetime
import hashlib
import httplib2 # Need to hack this to handle cookies on redirects
from resources import *	
from StringIO import StringIO
from time import sleep
import unittest
from urllib import urlencode
import uuid

httplib2.debuglevel=4

try:
  from lxml import etree, html, objectify
except ImportError:
  try:
    # Python 2.5
    import xml.etree.cElementTree as etree
  except ImportError:
    try:
      # Python 2.5
      import xml.etree.ElementTree as etree
    except ImportError:
      try:
        # normal cElementTree install
        import cElementTree as etree
      except ImportError:
        try:
          # normal ElementTree install
          import elementtree.ElementTree as etree
        except ImportError:
          print("Failed to import ElementTree from any known place")

#BASE_URI = 'http://localhost:8000/'
#ROOT_NODE = 'vos://nvo.caltech!vospace'
BASE_URI = 'http://vaovmsrv1.tuc.noao.edu:8000/'
ROOT_NODE = 'vos://datalab.noao.edu!vospace'
XSI_NS = 'http://www.w3.org/2001/XMLSchema-instance'
NODE = 'vos:Node'
DATANODE =  'vos:DataNode'
LINKNODE = 'vos:LinkNode'
UNSTRUCTUREDDATANODE = 'vos:UnstructuredDataNode'
STRUCTUREDDATANODE = 'vos:StructuredDataNode'
CONTAINERNODE = 'vos:ContainerNode'

#SCHEMA_ROOT = 'http://www.ivoa.net/xml/VOSpace/VOSpace-v2.0.xsd'
SCHEMA_ROOT = '/Users/mjg/Projects/noao/vospace/vospace-2.0/python/VOSpace-v2.0.xsd'

UWS_NS = "http://www.ivoa.net/xml/UWS/v1.0"
VOS_NS = "http://www.ivoa.net/xml/VOSpace/v2.0"
XLINK_NS = "http://www.w3.org/1999/xlink"
NAMESPACES = {"uws": UWS_NS, "vos": VOS_NS}

def suite():
  suite = unittest.TestSuite()
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(NodeTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(UWSTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(TransferTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(ProtocolTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(GetProtocolsTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(GetViewsTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(GetPropertiesTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(CreateNodeTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(MoveNodeTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(CopyNodeTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(DeleteNodeTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(SetNodeTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(GetNodeTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(FindNodesTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(PushToVoSpaceTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(PullToVoSpaceTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(PullFromVoSpaceTestCase))
  suite.addTest(unittest.TestLoader().loadTestsFromTestCase(PushFromVoSpaceTestCase))
  return suite

def set_node_uri(node, uri):
  """
  Set the uri on a node to the specified value
  """
  root = node.getroot()
  root.set('uri', uri)

def set_node_type(node, type):
  """
  Set the type of a node to the specified value
  """
  root = node.getroot()
  root.set('{%s}type' % XSI_NS, type)

def set_node_property(node, prop, value):
  """
  Set the property of a node to the specified value 
  """
  properties = node.xpath('vos:properties', namespaces = NAMESPACES)
  properties = len(properties) == 0 and etree.SubElement(node, 'properties') or properties[0]
  property = etree.SubElement(properties, 'property')
  property.set('uri', prop)
  property.text = value

def set_transfer_target(transfer, target):
  """
  Set the target of a transfer to the specified value
  """
  elem = transfer.find('{%s}target' % VOS_NS)
  elem.text = target

def set_transfer_direction(transfer, direction):
  """
  Set the direction of a transfer to the specified value
  """
  elem = transfer.find('{%s}direction' % VOS_NS)
  elem.text = direction

def set_transfer_view(transfer, view):
  """
  Set the view of a transfer to the specified value
  """
  elem = transfer.find('{%s}view' % VOS_NS)
  elem.set('uri', view)

def set_transfer_protocol(transfer, protocol, endpoint = None):
  """
  Set the protocol of a transfer to the specified value
  """
  elem = transfer.find('{%s}protocol' % VOS_NS)
  if elem is None:
    elem = etree.SubElement(transfer.getroot(), '{%s}protocol' % VOS_NS)
  elem.set('uri', protocol)
  if endpoint is not None:
    end = transfer.find('{%s}endpoint' % VOS_NS)
    if end is None:
      end = etree.SubElement(elem, '{%s}endpoint' % VOS_NS)
    end.text = endpoint    

def set_transfer_keepBytes(transfer, keepbytes):
  """
  Set the keepBytes of a transfer to the specified value
  """
  elem = transfer.find('{%s}keepBytes' % VOS_NS)
  elem.text = str(keepbytes)

def get_error_message(content):
  """
  Extract the error message from the HTML chunk
  """
  page = html.fromstring(content)
  message = page.xpath('//p')[0].text
  return message

def assert_xml_equal(doc1, doc2):
  """
  Assert that the two XML documents are equal
  """
  str1 = objectify.fromstring(doc1)
  str2 = objectify.fromstring(doc2)
  xml1 = etree.tostring(str1)
  xml2 = etree.tostring(str2)
  assert xml1 == xml2

def assert_contains(list, elem):
  """
  Assert that a list contains a particular element
  """
  assert elem in list

def assert_not_contains(list, elem):
  """
  Assert that a list does not contain a particular element
  """
  assert elem not in list

def test_start_uws(h, resource, body):
  """
  Test the start process of a UWS job (up to execution)
  """
  # Submit job
  resp, content = h.request(BASE_URI + resource, 'POST', body = body)
  assert int(resp.previous['status']) == 303 
  assert int(resp['status']) == 200
  job = Job(content)
  assert job.phase == 'PENDING'
  # Run job
  data = dict(PHASE = "RUN")
  resp, content = h.request(BASE_URI + '%s/%s/phase' % (resource, job.jobId), 'POST', urlencode(data))
  prevstatus = resp.previous['status']
  assert int(prevstatus) == 303
  assert int(resp['status']) == 200
  job = Job(content)
  assert job.phase == 'QUEUED'
  return job.jobId

def test_uws(h, resource, body, fail = False, summary = ''):
  """
  Test the UWS machinery
  """
  # Submit job
  jobid = test_start_uws(h, resource, body)
  content = ''
  while content not in ['COMPLETED', 'ERROR']:
    resp, content = h.request(BASE_URI + '%s/%s/phase' % (resource, jobid))
    assert int(resp['status']) == 200
    sleep(1)
  resp, content = h.request(BASE_URI + '%s/%s' % (resource, jobid))
  assert int(resp['status']) == 200
  job = Job(content)
  if fail:
    assert job.phase == 'ERROR'
    assert job.errorSummary == summary
  else:
    assert job.phase == 'COMPLETED'

def md5(filename):
  """
  Compute md5 hash of the specified file
  """
  m = hashlib.md5()
  try:
    fd = open(filename, 'rb')
  except IOError:
    print "Unable to open the file in readmoe:", filename
    return
  line = fd.readline()
  while line:
    m.update(line)
    line = fd.readline()
  fd.close()
  return m.hexdigest()


class XMLMatcher():

  def _xml_to_tree(self, xml, forgiving=False):
    self._xml = xml

    if not isinstance(xml, basestring):
      self._xml = str(xml)
      return xml

    if '<html' in xml[:200]:
      parser = etree.HTMLParser(recover=forgiving)
      return etree.HTML(str(xml), parser)
    else:
      parser = etree.XMLParser(recover=forgiving)
      return etree.XML(str(xml))

  def assert_xml(self, xml, xpath, **kw):
    """
    Check that a given extent of XML or HTML contains a given XPath, and return its first node
    """
    if hasattr(xpath, '__call__'):
      return self.assert_xml_tree(xml, xpath, **kw)

    tree = self._xml_to_tree(xml, forgiving=kw.get('forgiving', False))
    nodes = tree.xpath(xpath, namespaces = self.namespace)
    self.assertTrue(len(nodes) > 0, xpath + ' should match ' + self._xml)
    node = nodes[0]
    if kw.get('verbose', False):  self.reveal_xml(node)
    return node

  def assert_xml_tree(self, sample, block, **kw):
    doc = block
    doc_order = -1
    
    for node in doc.xpath('//*'):
      nodes = [self._node_to_predicate(a) for a in node.xpath('ancestor-or-self::*')]
      path = '//' + '/descendant::'.join(nodes)
      node = self.assert_xml(sample, path, **kw)
      location = len(node.xpath('preceding::*'))
      self.assertTrue(doc_order <= location, 'Node out of order!' + path)
      assert doc_order <= location
      doc_order = location

  def assert_xml_strings(self, str1, str2):
    xml1 = etree.fromstring(self._clean(str1))
    xml2 = etree.fromstring(self._clean(str2))
    self.namespace = isinstance(xml1, etree._ElementTree) and xml1.getroot().nsmap or xml1.nsmap
    if None in self.namespace:
      self.namespace['myns0'] = self.namespace[None]
      del self.namespace[None]
    self.namespace.update(isinstance(xml2, etree._ElementTree) and xml2.getroot().nsmap or xml2.nsmap)
    if None in self.namespace:
      self.namespace['myns1'] = self.namespace[None]
      del self.namespace[None]
    self.prefixes = dict((v, k) for k, v in self.namespace.iteritems())
    self.assert_xml_tree(xml1, xml2)

  def _node_to_predicate(self, node):
    path = self.ns_fix(node.tag)

    for key, value in node.attrib.items():
      path += '[ contains(@%s, "%s") ]' % (self.ns_fix(key), value)
          
    if node.text:
      path += '[ contains(text(), "%s") ]' % node.text

    return path

  def ns_fix(self, node):
    if node[0] == '{':
      ns = node[1:node.find('}')]
      if self.prefixes[ns] == None:
        node = node.replace('{' + ns + '}', '')
      else:
        node = node.replace('{' + ns + '}', self.prefixes[ns] + ':')
    return node

  def assertTrue(self, expr, message):
    if not expr: raise AssertionError(message)

  def _clean(self, str):
    text = ''.join([x.strip() for x in str.splitlines()])
    return text

class VOSpaceTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    pass

  def tearDown(self):
    """
    Tidy up after the test case
    """
    pass

class NodeTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    self.node = etree.parse('newNode.xml')
    self.xm = XMLMatcher()
  
  def test_node(self):
    test = Node()
    self.xm.assert_xml_strings(test.tostring(), open('test/node.xml').read())
 
  def test_datanode(self):
    test = DataNode()
    self.xm.assert_xml_strings(test.tostring(), open('test/datanode.xml').read())
 
  def test_containernode(self):
    test = ContainerNode()
    self.xm.assert_xml_strings(test.tostring(), open('test/containernode.xml').read())
 
  def test_linknode(self):
    test = LinkNode()
#    assert_xml_equal(test.tostring(), open('test/linknode.xml').read())
    self.xm.assert_xml_strings(test.tostring(), open('test/linknode.xml').read())
 
  def test_structureddatanode(self):
    test = StructuredDataNode()
    self.xm.assert_xml_strings(test.tostring(), open('test/structureddatanode.xml').read())
 
  def test_unstructureddatanode(self):
    test = UnstructuredDataNode()
    self.xm.assert_xml_strings(test.tostring(), open('test/unstructureddatanode.xml').read())

  def test_add_property(self):
    pass

  def test_add_capability(self):
    pass

  def test_add_accepts(self):
    pass

  def test_add_provides(self):
    pass

  def test_set_busy(self):
    pass

  def test_add_nodes(self):
    pass
                     
  def test_set_target(self):
    pass

class UWSTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    self.xm = XMLMatcher()

  def test_job(self):
    test = Job()
    job = open('test/job.xml').read()
    self.xm.assert_xml_strings(job, test.tostring())

  def test_job_from_file(self):
    test = etree.parse('test/job.xml')
    job = Job(test)
    self.xm.assert_xml_strings(job.tostring(), open('test/job.xml').read())

  def test_set_job_id(self):
    job = Job()
    jobid = uuid.uuid4().hex
    job.set_job_id(jobid)
    test = etree.fromstring(job.tostring())
    testid = test.xpath("/uws:job/uws:jobId", namespaces = NAMESPACES)
    self.assertEqual(jobid, testid[0].text)

  def test_set_owner_id(self):
    job = Job()
    owner_id = uuid.uuid4().hex
    job.set_owner_id(owner_id)
    test = etree.fromstring(job.tostring())
    testid = test.xpath("/uws:job/uws:ownerId", namespaces = NAMESPACES)
    self.assertEqual(owner_id, testid[0].text)

  def test_set_phase(self):
    job = Job()
    phase = 'PENDING'
    job.set_phase(phase)
    test = etree.fromstring(job.tostring())
    testid = test.xpath("/uws:job/uws:phase", namespaces = NAMESPACES)
    self.assertEqual(phase, testid[0].text)
 
  def test_set_start_time(self):
    job = Job()
    time = datetime.utcnow().isoformat()
    job.set_start_time(time)
    test = etree.fromstring(job.tostring())
    testid = test.xpath("/uws:job/uws:startTime", namespaces = NAMESPACES)
    self.assertEqual(time, testid[0].text)

  def test_set_end_time(self):
    job = Job()
    time = datetime.utcnow().isoformat()
    job.set_end_time(time)
    test = etree.fromstring(job.tostring())
    testid = test.xpath("/uws:job/uws:endTime", namespaces = NAMESPACES)
    self.assertEqual(time, testid[0].text)

  def test_set_execution_duration(self):
    job = Job()
    duration = 100
    job.set_execution_duration(duration)
    test = etree.fromstring(job.tostring())
    testid = test.xpath("/uws:job/uws:executionDuration", namespaces = NAMESPACES)
    self.assertEqual(duration, int(testid[0].text))

  def test_set_parameters(self):
    pass

  def test_add_parameter(self):
    pass

  def test_set_results(self):
    pass

  def test_add_result(self):
    job = Job()
    result = 'testInfo'
    value = 'http://localhost:8000/transfers/123456789abcdef/results/details'
    job.add_result(result, value)
    test = etree.fromstring(job.tostring())
    testid = test.xpath("/uws:job/uws:results/uws:result", namespaces = NAMESPACES)
    self.assertEqual(result, testid[0].get('id'))
    self.assertEqual(value, testid[0].get('{%s}href' % XLINK_NS))

  def test_set_error_summary(self):
    job = Job()
    message = 'This is a test error message.'
    job.set_error_summary(message)
    test = etree.fromstring(job.tostring())
    testid = test.xpath("/uws:job/uws:errorSummary/uws:message", namespaces = NAMESPACES)
    self.assertEqual(message, testid[0].text)

  def test_set_job_info(self):
    pass


class TransferTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    self.xm = XMLMatcher()

  def test_transfer(self):
    test = Transfer()
    transfer = open('test/transfer.xml').read()
    self.xm.assert_xml_strings(transfer, test.tostring())

  def test_transfer_from_file(self):
    test = etree.parse('test/transfer.xml')
    transfer = Transfer(test)
    self.xm.assert_xml_strings(transfer.tostring(), open('test/transfer.xml').read())


class ProtocolTestCase(unittest.TestCase):
  
  def setUp(self):
    """
    Initialize the test case
    """
    self.xm = XMLMatcher()

  def test_protocol(self):
    test = Protocol()
    protocol = open('test/protocol.xml').read()
    self.xm.assert_xml_strings(protocol, test.tostring())

  def test_protocol_from_file(self):
    test = etree.parse('test/protocol.xml')
    protocol = Protocol(test)
    self.xm.assert_xml_strings(protocol.tostring(), open('test/protocol.xml').read())


class GetProtocolsTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    h = httplib2.Http()
    self.resp, self.content = h.request(BASE_URI + 'protocols')

  def test_status_code(self):
    self.assertEqual(int(self.resp['status']), 200)


class GetViewsTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    h = httplib2.Http()
    self.resp, self.content = h.request(BASE_URI + 'views')

  def test_status_code(self):
    self.assertEqual(int(self.resp['status']), 200)


class GetPropertiesTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    h = httplib2.Http()
    self.resp, self.content = h.request(BASE_URI + 'properties')

  def test_status_code(self):
    self.assertEqual(int(self.resp['status']), 200)


class CreateNodeTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    self.h = httplib2.Http()
    self.xm = XMLMatcher()
    schema_doc = etree.parse(SCHEMA_ROOT)
    self.schema = etree.XMLSchema(schema_doc)

  def test_create_node(self):
    """
    Test creating a node: node1
    """
    node = Node()
    node.uri = ROOT_NODE + '/node1'
    resp, content = self.h.request(BASE_URI + 'nodes/node1', 'PUT', body = node.tostring(), headers={'Content-type': 'application/xml'})
    self.assertEqual(int(resp['status']), 201)
    test = Node(content)
    self.schema.assertValid(etree.parse(StringIO(content)))
    self.xm.assert_xml_strings(content, test.tostring())

  def test_create_datanode(self):
    """
    Test creating a data node: node2
    """
    node = DataNode()
    node.uri = ROOT_NODE + '/node2'
    resp, content = self.h.request(BASE_URI + 'nodes/node2', 'PUT', body = node.tostring(), headers={'Content-type': 'application/xml'})
    self.assertEqual(int(resp['status']), 201)
    test = DataNode(content)
    self.schema.assertValid(etree.parse(StringIO(content)))

  def test_create_linknode(self):
    """
    Test creating a link node: node3 -> node 1
    """
    node = etree.parse('test/node.xml')
    set_node_uri(node, ROOT_NODE + '/node3')
    set_node_type(node, LINKNODE)
    target = etree.SubElement(node.getroot(), 'target')
    target.text = ROOT_NODE + '/node1'
    resp, content = self.h.request(BASE_URI + 'nodes/node3', 'PUT', body = etree.tostring(node), headers={'Content-type': 'application/xml'})
    self.assertEqual(int(resp['status']), 201)
    test = LinkNode(content)
    self.schema.assertValid(etree.parse(StringIO(content)))
#    self.xm.assert_xml_strings(content, test.tostring())

  def test_create_unstructureddatanode(self):
    """
    Test creating an unstructured data node: node4
    """
    node = UnstructuredDataNode()
    node.uri = ROOT_NODE + '/node4'
    resp, content = self.h.request(BASE_URI + 'nodes/node4', 'PUT', body = node.tostring(), headers={'Content-type': 'application/xml'})
    self.assertEqual(int(resp['status']), 201)
    test = UnstructuredDataNode(content)
    self.schema.assertValid(etree.parse(StringIO(content)))
#    self.xm.assert_xml_strings(content, test.tostring())

  def test_create_structureddatanode(self):
    """
    Test creating a structured data node: node5
    """
    node = StructuredDataNode()
    node.uri = ROOT_NODE + '/node5'
    resp, content = self.h.request(BASE_URI + 'nodes/node5', 'PUT', body = node.tostring(), headers={'Content-type': 'application/xml'})
    self.assertEqual(int(resp['status']), 201)
    test = StructuredDataNode(content)
    self.schema.assertValid(etree.parse(StringIO(content)))
#    self.xm.assert_xml_strings(content, test.tostring())

  def test_create_containernode(self):
    """
    Test creating a container: node6
    """
    node = ContainerNode()
    node.uri = ROOT_NODE + '/node6'
    resp, content = self.h.request(BASE_URI + 'nodes/node6', 'PUT', body = node.tostring(), headers={'Content-type': 'application/xml'})
    self.assertEqual(int(resp['status']), 201)
    test = ContainerNode(content)
    self.schema.assertValid(etree.parse(StringIO(content)))
#    self.xm.assert_xml_strings(content, test.tostring())

  def test_create_unsupported_node(self):
    """
    Test creating a node of unsupported type
    """    
    node = etree.parse('test/node.xml')
    set_node_uri(node, ROOT_NODE + '/node7')
    set_node_type(node, 'vos:JunkNode')
    resp, content = self.h.request(BASE_URI + 'nodes/node7', 'PUT', body = etree.tostring(node), headers={'Content-type': 'application/xml'})
    self.assertEqual(int(resp['status']), 400) # should be 400
#    self.assertEqual(get_error_message(content), 'Node type not supported.')    

  def test_create_node_with_readonly_property(self):
    """
    Test creating a node with a read-only property: node8
    """
    node = etree.parse('test/node.xml')
    set_node_uri(node, ROOT_NODE + '/node8')
    set_node_type(node, 'vos:DataNode')
    set_node_property(node, 'ivo://ivoa.net/vospace/core#availableSpace', '300')
    resp, content = self.h.request(BASE_URI + 'nodes/node8', 'PUT', body = etree.tostring(node), headers={'Content-type': 'application/xml'})
    self.assertEqual(int(resp['status']), 401)
    self.assertEqual(get_error_message(content), 'User does not have permissions to set a readonly property.')
    

class MoveNodeTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    node = DataNode()
    node.uri = ROOT_NODE + '/node10'
    self.h = httplib2.Http()
    resp, content = self.h.request(BASE_URI + 'nodes/node10', 'PUT', body = node.tostring(), headers={'Content-type': 'application/xml'})
    self.xm = XMLMatcher()

  def test_move_basic_node(self):
    """
    Test moving a known node: node10 -> nodem1
    """
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node10')
    set_transfer_direction(transfer, ROOT_NODE + '/nodem1')
    set_transfer_keepBytes(transfer, False)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer))
    # Check move
    resp, content = self.h.request(BASE_URI + 'nodes/node10')
    self.assertEqual(int(resp['status']), 404)
    resp, content = self.h.request(BASE_URI + 'nodes/nodem1')
    self.assertEqual(int(resp['status']), 200)

  def test_move_basic_node_into_container(self):
    """
    Test moving a node into a container: node10 -> node6 => node6/node10
    """
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node10')
    set_transfer_direction(transfer, ROOT_NODE + '/node6')
    set_transfer_keepBytes(transfer, False)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer))
    # Check move
    resp, content = self.h.request(BASE_URI + 'nodes/node10')
    self.assertEqual(int(resp['status']), 404)
    resp, content = self.h.request(BASE_URI + 'nodes/node6')
    self.assertEqual(int(resp['status']), 200)
    # Check container listings
    nf = NodeFactory()
    container = nf.get_node(content)
    assert_contains(container.nodes, ROOT_NODE + "/node6/node10")
    resp, content = self.h.request(BASE_URI + 'nodes')
    self.assertEqual(int(resp['status']), 200)
    container = nf.get_node(content)
    assert_not_contains(container.nodes, ROOT_NODE + "/node10")

  def test_move_container(self):
    """
    Test moving a container: node6 -> nodem6
    """
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node6')
    set_transfer_direction(transfer, ROOT_NODE + '/nodem6')
    set_transfer_keepBytes(transfer, False)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer))
    # Check move
    resp, content = self.h.request(BASE_URI + 'nodes/node6')
    self.assertEqual(int(resp['status']), 404)
    resp, content = self.h.request(BASE_URI + 'nodes/nodem6')
    self.assertEqual(int(resp['status']), 200)
    # Check container listings
    nf = NodeFactory()
    container = nf.get_node(content)
    assert_contains(container.nodes, ROOT_NODE + "/nodem6/node10")
    resp, content = self.h.request(BASE_URI + 'nodes')
    self.assertEqual(int(resp['status']), 200)
    container = nf.get_node(content)
    assert_not_contains(container.nodes, ROOT_NODE + "/node6")
    assert_contains(container.nodes, ROOT_NODE + "/nodem6")
    
  def test_move_container_into_container(self):
    """
    Test moving a container into a container: nodem6 -> node11 => node11/nodem6/
    """
    # Create new container
    node = ContainerNode()
    node.uri = ROOT_NODE + '/node11'
    resp, content = self.h.request(BASE_URI + 'nodes/node11', 'PUT', body = node.tostring())
    self.assertEqual(int(resp['status']), 201)
    # Move container
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/nodem6')
    set_transfer_direction(transfer, ROOT_NODE + '/node11')
    set_transfer_keepBytes(transfer, False)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer))
    # Check move
    resp, content = self.h.request(BASE_URI + 'nodes/nodem6')
    self.assertEqual(int(resp['status']), 404)
    resp, content = self.h.request(BASE_URI + 'nodes/node11/nodem6')
    self.assertEqual(int(resp['status']), 200)
    # Check container listings
    nf = NodeFactory()
    container = nf.get_node(content)
    assert_contains(container.nodes, ROOT_NODE + "/node11/nodem6/node10")
    resp, content = self.h.request(BASE_URI + 'nodes')
    self.assertEqual(int(resp['status']), 200)
    container = nf.get_node(content)
    assert_not_contains(container.nodes, ROOT_NODE + "/nodem6")
    assert_contains(container.nodes, ROOT_NODE + "/node11")

  def test_move_node_to_existing_node(self):
    """
    Test moving a known node to an existing node (non-container): node10 -> node3
    """
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node10')
    set_transfer_direction(transfer, ROOT_NODE + '/node3')
    set_transfer_keepBytes(transfer, False)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer), fail = True, summary = 'A Node already exists with the requested URI.')

  def test_move_to_reserved_uri(self):
    """
    Test moving a known node to an autogenerated node (non-container): node10 -> .auto
    """
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node10')
    set_transfer_direction(transfer, ROOT_NODE + '/.auto')
    set_transfer_keepBytes(transfer, False)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer))
    # Check that node has been moved
    resp, content = self.h.request(BASE_URI + 'nodes')
    self.assertEqual(int(resp['status']), 200)
    nf = NodeFactory()
    container = nf.get_node(content)
    assert_not_contains(container.nodes, ROOT_NODE + "/node10")

  def test_move_node_to_null_uri(self):
    """
    Test moving a known node to the null node (non-container): node10 -> .null
    """
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node10')
    set_transfer_direction(transfer, ROOT_NODE + '/.null')
    set_transfer_keepBytes(transfer, False)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer))
    # Check that node has been deleted
    resp, content = self.h.request(BASE_URI + 'nodes')
    self.assertEqual(int(resp['status']), 200)
    nf = NodeFactory()
    container = nf.get_node(content)
    assert_not_contains(container.nodes, ROOT_NODE + "/node10")

  def test_move_container_to_null_uri(self):
    """
    Test moving a known container to the null node (non-container): node2 -> .null
    """
    pass

class CopyNodeTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    node = DataNode()
    node.uri = ROOT_NODE + '/node12'
    self.h = httplib2.Http()
    resp, content = self.h.request(BASE_URI + 'nodes/node12', 'PUT', body = node.tostring())
    self.xm = XMLMatcher()
    self.nf = NodeFactory()
#    self.assertEqual(int(resp['status']), 201)

#  def tearDown(self):
#    """
#    Tidy up after test
#    """
#    resp, content = self.h.request(BASE_URI + 'nodes/node12', 'DELETE')
#    self.assertEqual(int(resp['status']), 200)

  def test_copy_basic_node(self):
    """
    Test copying a basic node: node12 -> nodec1
    """
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node12')
    set_transfer_direction(transfer, ROOT_NODE + '/nodec1')
    set_transfer_keepBytes(transfer, True)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer))
    # Check copy
    resp, content = self.h.request(BASE_URI + 'nodes/node12')
    self.assertEqual(int(resp['status']), 200)
    resp, content = self.h.request(BASE_URI + 'nodes/nodec1')
    self.assertEqual(int(resp['status']), 200)

  def test_copy_basic_node_into_container(self):
    """
    Test copying a node into a container: node12 -> node11 => node11/node12
    """
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node12')
    set_transfer_direction(transfer, ROOT_NODE + '/node11')
    set_transfer_keepBytes(transfer, True)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer))
    # Check copy
    resp, content = self.h.request(BASE_URI + 'nodes/node12')
    self.assertEqual(int(resp['status']), 200)
    resp, content = self.h.request(BASE_URI + 'nodes/node11/node12')
    self.assertEqual(int(resp['status']), 200)
    # Check container listings
    resp, content = self.h.request(BASE_URI + 'nodes/node11')
    self.assertEqual(int(resp['status']), 200)
    container = self.nf.get_node(content)
    assert_contains(container.nodes, ROOT_NODE + "/node11/node12")

  def test_copy_container(self):
    """
    Test copying a container: node11 -> nodec11
    """
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node11')
    set_transfer_direction(transfer, ROOT_NODE + '/nodec11')
    set_transfer_keepBytes(transfer, True)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer))
    # Check copy
    resp, content = self.h.request(BASE_URI + 'nodes/node11')
    self.assertEqual(int(resp['status']), 200)
    resp, content = self.h.request(BASE_URI + 'nodes/nodec11')
    self.assertEqual(int(resp['status']), 200)
    # Check container listings
    container = self.nf.get_node(content)
    assert_contains(container.nodes, ROOT_NODE + "/nodec11/node12")
    assert_contains(container.nodes, ROOT_NODE + "/nodec11/nodem6")
    resp, content = self.h.request(BASE_URI + 'nodes/nodec11/nodem6')
    self.assertEqual(int(resp['status']), 200)
    container = self.nf.get_node(content)
    assert_contains(container.nodes, ROOT_NODE + "/nodec11/nodem6/node10")
    
  def test_copy_container_into_container(self):
    """
    Test copying a container into a container: node11 -> node13
    """
    # Create new container
    node = ContainerNode()
    node.uri = ROOT_NODE + '/node13'
    resp, content = self.h.request(BASE_URI + 'nodes/node13', 'PUT', body = node.tostring())
    self.assertEqual(int(resp['status']), 201)
    # Move container
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node11')
    set_transfer_direction(transfer, ROOT_NODE + '/node13')
    set_transfer_keepBytes(transfer, True)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer))
    # Check copy
    resp, content = self.h.request(BASE_URI + 'nodes/node11')
    self.assertEqual(int(resp['status']), 200)
    resp, content = self.h.request(BASE_URI + 'nodes/node13/node11')
    self.assertEqual(int(resp['status']), 200)
    # Check container listings
    container = self.nf.get_node(content)
    assert_contains(container.nodes, ROOT_NODE + "/node13/node11/nodem6")
    assert_contains(container.nodes, ROOT_NODE + "/node13/node11/node12")
    resp, content = self.h.request(BASE_URI + 'nodes/node13/node11/nodem6')
    self.assertEqual(int(resp['status']), 200)
    container = self.nf.get_node(content)
    assert_contains(container.nodes, ROOT_NODE + "/node13/node11/nodem6/node10")

  def test_copy_to_existing_node(self):
    """
    Test copying a known node to an existing node (non-container): node12 -> node3
    """
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node12')
    set_transfer_direction(transfer, ROOT_NODE + '/node3')
    set_transfer_keepBytes(transfer, True)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer), fail = True, summary = 'A Node already exists with the requested URI.')

  def test_copy_to_reserved_uri(self):
    """
    Test copying a known node to an autogenerated node (non-container): node12 -> .auto
    """
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, ROOT_NODE + '/node12')
    set_transfer_direction(transfer, ROOT_NODE + '/.auto')
    set_transfer_keepBytes(transfer, True)
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(transfer))
    # Check copy
    resp, content = self.h.request(BASE_URI + 'nodes')
    self.assertEqual(int(resp['status']), 200)
    nodes = self.nf.get_node(content)
    self.assertEqual(len([x for x in nodes.nodes if len(x) - x.rfind('/') == 33]), 2)


class DeleteNodeTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    self.h = httplib2.Http()
    
  def test_delete_node(self):
    """
    Test deleting a known node: node9
    """
    node = Node()
    node.uri = ROOT_NODE + '/node9'
    resp, content = self.h.request(BASE_URI + 'nodes/node9', 'PUT', body = node.tostring())
    self.assertEqual(int(resp['status']), 201)
    resp, content = self.h.request(BASE_URI + 'nodes/node9', 'DELETE')
    self.assertEqual(int(resp['status']), 200)

  def test_delete_unknown_node(self):
    """
    Test deleting an unknown node: nodexx
    """
    resp, content = self.h.request(BASE_URI + 'nodes/nodexx', 'DELETE')
    self.assertEqual(int(resp['status']), 404)
    self.assertEqual(get_error_message(content), 'The specified node does not exist.')

  def test_delete_container(self):
    """
    Test deleting a container: nodec1
    """
    resp, content = self.h.request(BASE_URI + 'nodes/nodec1', 'DELETE')
    self.assertEqual(int(resp['status']), 200)
    

class GetNodeTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    self.h = httplib2.Http()
    self.nf = NodeFactory()
    self.xm = XMLMatcher()
    schema_doc = etree.parse(SCHEMA_ROOT)
    self.schema = etree.XMLSchema(schema_doc)
    node = open('test/fullnode.xml').read()
    resp, content = self.h.request(BASE_URI + 'nodes/node16', 'PUT', body = node)
    self.assertEqual(int(resp['status']), 201)

  def tearDown(self):
    """
    Tidy up after test
    """
    resp, content = self.h.request(BASE_URI + 'nodes/node16', 'DELETE')
    self.assertEqual(int(resp['status']), 200)

  def test_get_node(self):
    """
    Test getting a node with no arguments: node16
    """
    resp, content = self.h.request(BASE_URI + 'nodes/node16')
    self.assertEqual(int(resp['status']), 200)
    self.schema.assertValid(etree.parse(StringIO(content)))
#    self.xm.assert_xml_strings(content, open('test/fullnode.xml').read())

  def test_get_min_detail(self):
    """
    Test getting a node with minimum detail: node16
    """
    resp, content = self.h.request(BASE_URI + 'nodes/node16?detail=min')
    self.assertEqual(int(resp['status']), 200)
    self.schema.assertValid(etree.parse(StringIO(content)))
    self.xm.assert_xml_strings(content, open('test/fullnode_min.xml').read())

  def test_get_max_detail(self):
    """
    Test getting a node with maximum detail: node16
    """
    resp, content = self.h.request(BASE_URI + 'nodes/node16?detail=max')
    self.assertEqual(int(resp['status']), 200)
    self.schema.assertValid(etree.parse(StringIO(content)))
#    self.xm.assert_xml_strings(content, open('test/fullnode.xml').read())

  def test_get_properties(self):
    """
    Test getting a node with just properties: node16
    """
    resp, content = self.h.request(BASE_URI + 'nodes/node16?detail=properties')
    self.assertEqual(int(resp['status']), 200)
    self.schema.assertValid(etree.parse(StringIO(content)))
    self.xm.assert_xml_strings(content, open('test/fullnode_props.xml').read())

  def test_get_protocols(self):
    """
    Test getting the protocols for a node: node16
    """
    protocol = Protocol()
    resp, content = self.h.request(BASE_URI + 'nodes/node16', 'POST', body = protocol.tostring())
    self.xm.assert_xml_strings(content, open('test/protocols.xml').read())

  def test_get_container(self):
    """
    Test getting a container: nodecon
    """
    node = ContainerNode()
    node.uri = ROOT_NODE + '/nodecon'
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon', 'PUT', body = node.tostring())
    node = Node()
    node.uri = ROOT_NODE + '/nodecon/nodea'
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon/nodea', 'PUT', body = node.tostring())
    node.uri = ROOT_NODE + '/nodecon/nodeb'
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon/nodeb', 'PUT', body = node.tostring())
    node.uri = ROOT_NODE + '/nodecon/nodec'
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon/nodec', 'PUT', body = node.tostring())
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon')
    self.assertEqual(int(resp['status']), 200)
    container = self.nf.get_node(content)
    assert_contains(container.nodes, ROOT_NODE + "/nodecon/nodea")
    assert_contains(container.nodes, ROOT_NODE + "/nodecon/nodeb")
    assert_contains(container.nodes, ROOT_NODE + "/nodecon/nodec")
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon', 'DELETE')
    self.assertEqual(int(resp['status']), 200)
    
  def test_get_container_with_offset(self):
    """
    Test getting a container with an offset: nodecon
    """
    node = ContainerNode()
    node.uri = ROOT_NODE + '/nodecon'
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon', 'PUT', body = node.tostring())
    node = Node()
    node.uri = ROOT_NODE + '/nodecon/nodea'
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon/nodea', 'PUT', body = node.tostring())
    node.uri = ROOT_NODE + '/nodecon/nodeb'
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon/nodeb', 'PUT', body = node.tostring())
    node.uri = ROOT_NODE + '/nodecon/nodec'
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon/nodec', 'PUT', body = node.tostring())
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon?uri=vos://nvo.caltech!vospace/nodecon/nodeb&offset=1')
    self.assertEqual(int(resp['status']), 200)
    container = self.nf.get_node(content)
    assert_contains(container.nodes, ROOT_NODE + "/nodecon/nodeb")
    resp, content = self.h.request(BASE_URI + 'nodes/nodecon', "DELETE")
    self.assertEqual(int(resp['status']), 200)

class SetNodeTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    self.h = httplib2.Http()
    self.nf = NodeFactory()
    node = StructuredDataNode()
    node.uri = ROOT_NODE + '/node16'
    node.add_property(DESCRIPTION, "Test value")
    resp, content = self.h.request(BASE_URI + 'nodes/node16', 'PUT', body = node.tostring())
    self.assertEqual(int(resp['status']), 201)

  def tearDown(self):
    """
    Tidy up after test
    """
    resp, content = self.h.request(BASE_URI + 'nodes/node16', 'DELETE')
    self.assertEqual(int(resp['status']), 200)

  def test_set_node(self):
    """
    Test setting a basic node: node16
    """
    node = StructuredDataNode()
    node.uri = ROOT_NODE + '/node16'
    node.add_property(DESCRIPTION, "My award winning image")
    resp, content = self.h.request(BASE_URI + 'nodes/node16', 'POST', body = node.tostring())
    self.assertEqual(int(resp['status']), 200)
    newnode = self.nf.get_node(content)
    self.assertEqual(newnode.properties[DESCRIPTION], "My award winning image")

  def test_set_empty_value(self):
    """
    Test setting a property to empty: node16
    """
    node = StructuredDataNode()
    node.uri = ROOT_NODE + '/node16'
    node.add_property(DESCRIPTION, "")
    resp, content = self.h.request(BASE_URI + 'nodes/node16', 'POST', body = node.tostring())
    self.assertEqual(int(resp['status']), 200)
    newnode = self.nf.get_node(content)
    self.assertEqual(newnode.properties[DESCRIPTION], None)

  def test_delete_property(self):
    """
    Test deleting a property: node16
    """
    node = StructuredDataNode()
    node.uri = ROOT_NODE + '/node16'
    node.add_property(DESCRIPTION, "")
    nodexml = etree.fromstring(node.tostring())
    prop = nodexml.find('.//{%s}property' % VOS_NS)
    prop.set('{%s}nil' % XSI_NS, 'true')
    resp, content = self.h.request(BASE_URI + 'nodes/node16', 'POST', body = etree.tostring(nodexml))
    self.assertEqual(int(resp['status']), 200)
    newnode = self.nf.get_node(content)
    assert DESCRIPTION not in newnode.properties

  def test_set_node_type(self):
    """
    Test setting the node type: node16
    """
    node = UnstructuredDataNode()
    node.uri = ROOT_NODE + '/node16'
    resp, content = self.h.request(BASE_URI + 'nodes/node16', 'POST', body = node.tostring())
    self.assertEqual(int(resp['status']), 200)
    newnode = self.nf.get_node(content)
    assert isinstance(newnode, StructuredDataNode)

  def test_set_views(self):
    """
    Test setting the view: node16
    """
    node = StructuredDataNode()
    node.uri = ROOT_NODE + '/node16'
    node.add_accepts('urn:local:myview')
    node.add_provides('urn:local"myview')
    resp, content = self.h.request(BASE_URI + 'nodes/node16', 'POST', body = node.tostring())
    self.assertEqual(int(resp['status']), 200)
    newnode = self.nf.get_node(content)
    assert 'urn:local:myview' not in newnode.accepts
    assert 'urn:local:myview' not in newnode.provides

  def test_set_children(self):
    """
    Test setting children: node5
    """
    node = ContainerNode()
    node.uri = ROOT_NODE + '/node11'
    node.add_node(ROOT_NODE + '/node20')
    resp, content = self.h.request(BASE_URI + 'nodes/node11', 'POST', body = node.tostring())
    self.assertEqual(int(resp['status']), 200)
    newnode = self.nf.get_node(content)
    assert_not_contains(newnode.nodes, ROOT_NODE + '/node20')


class FindNodesTestCase(unittest.TestCase):

  def test_findNodes(self):
    pass


class PushToVoSpaceTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    self.h = httplib2.Http()
    self.transfer = etree.parse('test/transfer.xml')

  def handle_job(self, transfer, fail = False, summary = None):
    """
    Handle a data transfer using pushToVoSpace
    """
    # Submit job
    jobid = test_start_uws(self.h, 'transfers', etree.tostring(transfer))
    content = 'QUEUED'
    while content not in ['COMPLETED', 'ERROR']:
      resp, content = self.h.request(BASE_URI + 'transfers/%s/phase' % jobid)
      self.assertEqual(int(resp['status']), 200)
      # Transfer data
      if content == 'EXECUTING':
        resp, content = self.h.request(BASE_URI + 'transfers/%s' % jobid)
        job = Job(content)
        assert len(job.results) > 0
        results = job.results['transferDetails']
        resp, content = self.h.request(results)
        transfer = Transfer(content)
        for protocol in transfer.protocols:
          if protocol.uri == 'ivo://ivoa.net/vospace/core#httpput':
            file = open('test/burbidge.vot').read()
            resp, content = self.h.request(protocol.endpoint, 'PUT', body = file)
        break
      sleep(1)
    # Check for job completing (wait for timeout on transfer completion check)
    while content not in ['COMPLETED', 'ERROR']:
      resp, content = self.h.request(BASE_URI + 'transfers/%s/phase' % jobid)
      self.assertEqual(int(resp['status']), 200)
      sleep(1)
    resp, content = self.h.request(BASE_URI + 'transfers/%s' % jobid)
    job = Job(content)
    if fail:
      assert job.phase == 'ERROR'
      assert job.errorSummary == summary
    else:
      assert job.phase == 'COMPLETED'

  def test_basic_push_to_vospace(self):
    """
    Test transferring data to the space: node12
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pushToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpput')
    self.handle_job(self.transfer)

  def test_push_to_vospace_new_node(self):
    """
    Test transferring data to a new node: node14
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node14')
    set_transfer_direction(self.transfer, 'pushToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpput')
    self.handle_job(self.transfer)

  def test_push_to_vospace_container(self):
    """
    Test transferring data to a container: node13
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node13')
    set_transfer_direction(self.transfer, 'pushToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpput')
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 500)
    self.assertEqual(get_error_message(content), 'Data cannot be uploaded to a container')

  def test_push_to_vospace_reserved_uri(self):
    """
    Test transferring data to an autogenerated node: .auto
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/.auto')
    set_transfer_direction(self.transfer, 'pushToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpput')
    self.handle_job(self.transfer)

  def test_quick_push_to_vospace(self):
    """
    Test transferring data to the space using the shortcut method: node12
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pushToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpput')
    # Submit job
    resp, content = self.h.request(BASE_URI + "sync", 'POST', body = etree.tostring(self.transfer))
    assert int(resp.previous['status']) == 303
    assert int(resp['status']) == 200
    jobid = resp['content-location'].split('/')[4]
    transfer = Transfer(content)
    for protocol in transfer.protocols:
      if protocol.uri == 'ivo://ivoa.net/vospace/core#httpput':
        file = open('test/burbidge.vot').read()
        resp, content = self.h.request(protocol.endpoint, 'PUT', body = file)
    # Check for job completing (wait for timeout on transfer completion check)
    while content not in ['COMPLETED', 'ERROR']:
      resp, content = self.h.request(BASE_URI + 'transfers/%s/phase' % jobid)
      self.assertEqual(int(resp['status']), 200)
      sleep(1)
    resp, content = self.h.request(BASE_URI + 'transfers/%s' % jobid)
    job = Job(content)
    assert job.phase == 'COMPLETED'

  def test_push_to_vospace_with_unsupported_view(self):
    """
    Test transferring data with an unsupported view
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pushToVoSpace')
    set_transfer_view(self.transfer, 'urn:local:myview')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpput')
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 500)
    self.assertEqual(get_error_message(content), 'Service does not support the requested View')

  def test_push_to_vospace_with_unsupported_protocol(self):
    """
    Test transferring data to an unsupported protocol
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pushToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'urn:local:myprotocol')
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 500)
    self.assertEqual(get_error_message(content), 'The service supports none of the requested Protocols')

  def test_push_to_vospace_with_multiple_protocols(self):
    pass

class PullToVoSpaceTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    self.h = httplib2.Http()
    self.transfer = etree.parse('test/transfer.xml')

  def test_basic_pull_to_vospace(self):
    """
    Test transferring data to the space: node12
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pullToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpget', endpoint = 'http://www.cacr.caltech.edu/~mjg/burbidge.vot')
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(self.transfer))

  def test_pull_to_vospace_new_node(self):
    """
    Test transferring data to the space: node12
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node15')
    set_transfer_direction(self.transfer, 'pullToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpget', endpoint = 'http://www.cacr.caltech.edu/~mjg/burbidge.vot')
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(self.transfer))

  def test_pull_to_vospace_container(self):
    """
    Test transferring data to the space: node13
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node13')
    set_transfer_direction(self.transfer, 'pullToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpget', endpoint = 'http://www.cacr.caltech.edu/~mjg/burbidge.vot')
    # Submit job
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 500)
    self.assertEqual(get_error_message(content), 'Data cannot be uploaded to a container')

  def test_pull_to_vospace_reserved_uri(self):
    """
    Test transferring data to the space: node13/.auto
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node13/.auto')
    set_transfer_direction(self.transfer, 'pullToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpget', endpoint = 'http://www.cacr.caltech.edu/~mjg/burbidge.vot')
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(self.transfer))

  def test_pull_to_vospace_invalid_destination(self):
    """
    Test transferring data to the space: node12
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node1s')
    set_transfer_direction(self.transfer, 'pullToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpget', endpoint = 'http:/some.server.com/some/data/here.vot')
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(self.transfer), fail = True, summary = 'Destination URI is invalid')

  def test_pull_to_vospace_with_unsupported_view(self):
    """
    Test transferring data to the space: node12
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pullToVoSpace')
    set_transfer_view(self.transfer, 'urn:local:myview')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpget', endpoint = 'http://www.cacr.caltech.edu/~mjg/burbidge.vot')
    # Submit job
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 500)
    self.assertEqual(get_error_message(content), 'Service does not support the requested View')

  def test_pull_to_vospace_with_unsupported_protocol(self):
    """
    Test transferring data to the space: node12
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pullToVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'urn:local:myprotocol', endpoint = 'http://www.cacr.caltech.edu/~mjg/burbidge.vot')
    # Submit job
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 500)
    self.assertEqual(get_error_message(content), 'The service supports none of the requested Protocols')


class PullFromVoSpaceTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    self.h = httplib2.Http()
    self.transfer = etree.parse('test/transfer.xml')

  def handle_job(self, transfer, fail = False, summary = None):
    """
    Handle a data transfer using pullFromVoSpace
    """
    # Submit job
    jobid = test_start_uws(self.h, 'transfers', etree.tostring(transfer))
    content = 'QUEUED'
    while content not in ['COMPLETED', 'ERROR']:
      resp, content = self.h.request(BASE_URI + 'transfers/%s/phase' % jobid)
      self.assertEqual(int(resp['status']), 200)
      # Transfer data
      if content == 'EXECUTING':
        resp, content = self.h.request(BASE_URI + 'transfers/%s' % jobid)
        job = Job(content)
        assert len(job.results) > 0
        results = job.results['transferDetails']
        resp, content = self.h.request(results)
        transfer = Transfer(content)
        for protocol in transfer.protocols:
          if protocol.uri == 'ivo://ivoa.net/vospace/core#httpget':
            resp, content = self.h.request(protocol.endpoint)
            file = open('test/check.vot', 'wb')
            file.write(content)
            file.close()
            # Verify files
            oldmd5 = md5('test/burbidge.vot')
            newmd5 = md5('test/check.vot')
            self.assertEqual(oldmd5, newmd5)
        break
      sleep(1)
    # Check for job completing (wait for timeout on transfer completion check)
    while content not in ['COMPLETED', 'ERROR']:
      resp, content = self.h.request(BASE_URI + 'transfers/%s/phase' % jobid)
      self.assertEqual(int(resp['status']), 200)
      sleep(1)
    resp, content = self.h.request(BASE_URI + 'transfers/%s' % jobid)
    job = Job(content)
    if fail:
      assert job.phase == 'ERROR'
      assert job.errorSummary == summary
    else:
      assert job.phase == 'COMPLETED'

  def test_basic_pull_from_vospace(self):
    """
    Test transferring data from the space: node12
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pullFromVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpget')
    self.handle_job(self.transfer)
    
  def test_quick_pull_from_vospace(self):
    """
    Test transferring data from the space: node12
    """
    resp, content = self.h.request(BASE_URI + "nodes/node12?view=data")
    file = open('test/check.vot', 'wb')
    file.write(content)
    file.close()
    # Verify files
    oldmd5 = md5('test/burbidge.vot')
    newmd5 = md5('test/check.vot')
    self.assertEqual(oldmd5, newmd5)

  def test_quick_pull_from_vospace_with_unknown_node(self):
    """
    Test transferring data from the space: node12
    """
    resp, content = self.h.request(BASE_URI + "nodes/node123?view=data")
    self.assertEqual(int(resp['status']), 404)
    self.assertEqual(get_error_message(content), 'A Node does not exist with the requested URI.') 

  def test_pull_from_vospace_unknown_node(self):
    """
    Test transferring data from the space: node123
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node123')
    set_transfer_direction(self.transfer, 'pullFromVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpget')
    # Submit job
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 409)
    self.assertEqual(get_error_message(content), 'A Node does not exist with the requested URI.')

  def test_pull_from_vospace_with_unsupported_view(self):
    """
    Test transferring data from the space with an unsupported view
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pullFromVoSpace')
    set_transfer_view(self.transfer, 'urn:local:myview')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpget')
    # Submit job
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 500)
    self.assertEqual(get_error_message(content), 'Service does not support the requested View')

  def test_pull_from_vospace_with_unsupported_protocol(self):
    """
    Test transferring data from the space with an unsupported protocol
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pullFromVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'urn:local:myprotocol')
    # Submit job
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 500)
    self.assertEqual(get_error_message(content), 'The service supports none of the requested Protocols')


class PushFromVoSpaceTestCase(unittest.TestCase):

  def setUp(self):
    """
    Initialize the test case
    """
    self.h = httplib2.Http()
    self.transfer = etree.parse('test/transfer.xml')

  def test_basic_push_from_vospace(self):
    """
    Test transferring data from the space: node12
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pushFromVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpput', endpoint = 'http://some.server.com/put/the/data/here')
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(self.transfer))  

  def test_push_from_vospace_unknown_node(self):
    """
    Test transferring data from the space: node123
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node123')
    set_transfer_direction(self.transfer, 'pushFromVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpput', endpoint = 'http://some.server.com/put/the/data/here')
    # Submit job
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 409)
    self.assertEqual(get_error_message(content), 'A Node does not exist with the requested URI.')

  def test_push_from_vospace_with_unsupported_view(self):
    """
    Test transferring data from the space with an unsupported view
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pushFromVoSpace')
    set_transfer_view(self.transfer, 'urn:local:myview')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpput', endpoint = 'http://some.server.com/put/the/data/here')
    # Submit job
#    test_uws(self.h, 'transfers', etree.tostring(transfer))  
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 500)
    self.assertEqual(get_error_message(content), 'Service does not support the requested View')

  def test_push_from_vospace_with_unsupported_protocol(self):
    """
    Test transferring data from the space with an unsupported protocol
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pushFromVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'urn:local:myprotocol', endpoint = 'http://some.server.com/put/the/data/here')
    # Submit job
    resp, content = self.h.request(BASE_URI + 'transfers', 'POST', body = etree.tostring(self.transfer))
    self.assertEqual(int(resp['status']), 500)
    self.assertEqual(get_error_message(content), 'The service supports none of the requested Protocols')

  def test_push_from_vospace_with_invalid_destination(self):
    """
    Test transferring data from the space to an invalid destination
    """
    set_transfer_target(self.transfer, ROOT_NODE + '/node12')
    set_transfer_direction(self.transfer, 'pushFromVoSpace')
    set_transfer_view(self.transfer, 'ivo://ivoa.net/vospace/core#votable')
    set_transfer_protocol(self.transfer, 'ivo://ivoa.net/vospace/core#httpput', endpoint = 'http://some.server.com/put/the/data/here')
    # Submit job
    test_uws(self.h, 'transfers', etree.tostring(self.transfer), fail = True, summary = 'Destination URI is invalid')  



if __name__ == '__main__':
  suite = suite()
  unittest.TextTestRunner(verbosity = 2).run(suite)
