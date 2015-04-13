#!/usr/bin/python -u
# 2010/03/09
# v0.1
#
# uws.py
# Python code to handle resource representations

from config import *

try:
  from lxml import etree
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

# ----------------------------------------------------------
def toBoolean(val):
  """
  Get the boolean value of the specified value.
  """
  if val is True or val is False:
    return val

  falseItems = ["false", "f", "no", "n", "none", "0", "[]", "{}", ""]

  return not str(val).strip().lower() in falseItems

# ----------------------------------------------------------
class Job():

  blankJob = '''<uws:job xsi:schemaLocation="http://www.ivoa.net/xml/UWS/v1.0 UWS.xsd " xmlns:xml="http://www.w3.org/XML/1998/namespace" xmlns:uws="http://www.ivoa.net/xml/UWS/v1.0" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <uws:jobId/>
  <uws:ownerId xsi:nil="true"/>
  <uws:phase/>
  <uws:startTime xsi:nil="true"/>
  <uws:endTime xsi:nil="true"/>
  <uws:executionDuration/>
  <uws:destruction xsi:nil="true"/>
  <uws:parameters/>
  <uws:results/>
  <uws:errorSummary type="transient" hasDetail="true"/>
  <uws:jobInfo/>
</uws:job>'''
  
  def __init__(self, job = None):
    """
    Create a new Job or one around the specified Element.
    """
    self.parameters = {}
    self.results = {}
    self.errorSummary = ''
    self.jobInfo = ''
    if job == None:
      self.jobId = ''
      self.ownerId = ''
      self.phase = ''
      self.startTime = ''
      self.endTime = ''
      self.executionDuration = 0
    else:
      if isinstance(job, str) or isinstance(job, unicode): job = etree.fromstring(job)
      self.jobId = job.find('{%s}jobId' % UWS_NS).text
      self.ownerId = job.find('{%s}ownerId' % UWS_NS).text 
      self.phase = job.find('{%s}phase' % UWS_NS).text
      self.startTime = job.find('{%s}startTime' % UWS_NS).text
      self.endTime = job.find('{%s}endTime' % UWS_NS).text 
      self.executionDuration = int(job.find('{%s}executionDuration' % UWS_NS).text)
      parameters = job.find('{%s}parameters' % UWS_NS)
      if parameters != None and len(parameters) > 0:
        for param in parameters:
          self.parameters[param.get('id')] = param.text
      jobInfo = job.find('{%s}jobInfo' % UWS_NS)
      if jobInfo != None and len(jobInfo) > 0:
        self.jobInfo = etree.tostring(jobInfo.getchildren()[0])
      errorSummary = job.find('{%s}errorSummary' % UWS_NS)
      if errorSummary != None and len(errorSummary) > 0:
        self.errorSummary = errorSummary.getchildren()[0].text
      results = job.find('{%s}results' % UWS_NS)
      if results != None and len(results) > 0:
        for res in results.getchildren():
          self.results[res.get('id')] = res.get('{%s}href' % XLINK_NS)

  def tostring(self):
    """
    Get a string representation of the Job
    """
    job = etree.fromstring(self.blankJob)
    job.find('{%s}jobId' % UWS_NS).text = self.jobId
    job.find('{%s}ownerId' % UWS_NS).text = self.ownerId
    job.find('{%s}phase' % UWS_NS).text = self.phase
    startTime = job.find('{%s}startTime' % UWS_NS)
    if self.startTime != '' and self.startTime != None:
      startTime.text = self.startTime
      del startTime.attrib['{%s}nil' % XSI_NS]
    endTime = job.find('{%s}endTime' % UWS_NS)
    if self.endTime != '' and self.endTime != None:
      endTime.text = self.endTime
      del endTime.attrib['{%s}nil' % XSI_NS]
    job.find('{%s}executionDuration' % UWS_NS).text = str(self.executionDuration)
    parameters = job.find('{%s}parameters' % UWS_NS)
    for params in self.parameters:
      param = etree.SubElement(parameters, '{%s}parameter' % UWS_NS)
      param.set('id', params)
      param.text = self.parameters[params]
    results = job.find('{%s}results' % UWS_NS)
    for res in self.results:
      result = etree.SubElement(results, '{%s}result' % UWS_NS)
      result.set('id', res)
      result.set('{%s}href' % XLINK_NS, self.results[res])
    errorSummary = job.find('{%s}errorSummary' % UWS_NS)
    if self.errorSummary != '':
      message = etree.SubElement(errorSummary, '{%s}message' % UWS_NS)
      message.text = self.errorSummary
    else:
      job.remove(errorSummary)
    jobInfo = job.find('{%s}jobInfo' % UWS_NS)
    if self.jobInfo != '':
      jobInfo.append(etree.fromstring(self.jobInfo)) 
    else:
      job.remove(jobInfo)
    return etree.tostring(job)

  def set_job_id(self, jobid):
    self.jobId = jobid

  def set_owner_id(self, ownerid):
    self.ownerId = ownerid 
  
  def set_phase(self, phase):
    self.phase = phase
 
  def set_start_time(self, startTime):
    self.startTime = startTime  
  
  def set_end_time(self, endTime):
    self.endTime = endTime 
  
  def set_execution_duration(self, executionDuration):
    self.executionDuration = executionDuration 
  
  def set_parameters(self, parameters):
    self.parameters = parameters  
  
  def add_parameter(self, parameter, value):
    self.parameter[parameter] = value

  def set_results(self, results):
    self.results = results  
  
  def add_result(self, result, value):
    self.results[result] = value

  def set_error_summary(self, errorSummary):
    self.errorSummary = errorSummary 
  
  def set_job_info(self, jobInfo):
    if isinstance(jobInfo, str):
      self.jobInfo = jobInfo  
    else:
      self.jobInfo = etree.tostring(jobInfo)
  
  def getResultsAsXml(self):
    results = etree.Element('{%s}results' % UWS_NS)
    for res in self.results:
      result = etree.SubElement(results, '{%s}result' % UWS_NS)
      result.set('id', res)
      result.set('{%s}href' % XLINK_NS, self.results[res])
    return results

# ----------------------------------------------------------
class JobList():

  blankJobs = '''<uws:jobs xsi:schemaLocation="http://www.ivoa.net/xml/UWS/v1.0 UWS.xsd " xmlns:xml="http://www.w3.org/XML/1998/namespace" xmlns:uws="http://www.ivoa.net/xml/UWS/v1.0" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
</uws:jobs>'''
  
  def __init__(self, job = None):
    """
    Create a new Job or one around the specified Element.
    """
    self.jobs = {}

  def add_job(self, job, phase):
    """
    Add a job to the list
    """
    self.jobs[job] = phase

  def tostring(self):
    """
    Return a string representation of the JobList
    """
    joblist = etree.fromstring(self.blankJobs)
    for job in self.jobs:
      jobref = etree.SubElement(joblist, 'jobref')
      jobref.set('id', job)
      phase = etree.SubElement(jobref, 'phase')
      phase.text = self.jobs[job]
    return etree.tostring(joblist)

# ----------------------------------------------------------
class NodeFactory():

  def get_node(self, xml):
    if isinstance(xml, str): xml = etree.fromstring(xml)
    type = xml.get('{%s}type' % XSI_NS)
    if type == 'vos:Node' or type == None:
      return Node(node = xml)
    elif type == 'vos:DataNode':
      return DataNode(node = xml)
    elif type == 'vos:ContainerNode':
      return ContainerNode(node = xml)
    elif type == 'vos:UnstructuredDataNode':
      return UnstructuredDataNode(node = xml)
    elif type == 'vos:StructuredDataNode':
      return StructuredDataNode(node = xml)
    elif type == 'vos:LinkNode':
      return LinkNode(node = xml)
    else:
      raise VOSpaceError(400, "Node type not supported.")

# ----------------------------------------------------------
class Node():

  blankNode = '''<node xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns="%s" 
  xmlns:vos="%s"
  xsi:type="" uri="" />''' % (VOSPACE_NS, VOSPACE_NS)

  TYPE = 'vos:Node'
  
  def __init__(self, node = None):
    """
    Creates a new Node or one around the specified element.
    """
    self.uri = ''
    self.properties = {}
    self.capabilities = []
    if node != None:
      if isinstance(node, str): node = etree.fromstring(node)
      self.uri = node.get('uri')
      properties = node.find('{%s}properties' % VOSPACE_NS)
      if properties is not None:
        for property in properties:
          if property.text in READ_ONLY_PROPERTIES:
            raise VOSpaceError(401, "User does not have permissions to set a readonly property.")
          else:
            self.properties[property.get('uri')] = property.text
      capabilities = node.find('{%s}capabilities' % VOSPACE_NS)
      if capabilities is not None:
        for capability in capabilities:
          self.capabilities.append(capability.get('uri'))

  def tostring(self, detail = 'max'):
    node = etree.fromstring(self.blankNode)
    self.print_node(node)
    if detail != 'min': self.print_properties(node)
    if detail == 'max': self.print_capabilities(node)
    return etree.tostring(node)

  def set_uri(self, uri):
    self.uri = uri

  def add_property(self, property, value):
    if property in READ_ONLY_PROPERTIES:
      raise VOSpaceError(401, "User does not have permissions to set a readonly property.", summary = PERMISSION_DENIED)
    else:
      self.properties[property] = value

  def add_capability(self, uri):
    self.capabilities.append(uri)

  def clear_capabilities(self):
    self.capabilities = []

  def print_node(self, node):
    node.set('uri', self.uri)
    node.set('{%s}type' % XSI_NS, self.TYPE)

  def print_properties(self, node):
    properties = node.find('{%s}properties' % VOSPACE_NS)
    if properties is None:
      properties = etree.SubElement(node, '{%s}properties' % VOSPACE_NS)
    for property in self.properties:
      prop = etree.SubElement(properties, '{%s}property' % VOSPACE_NS)
      prop.set('uri', property)
      prop.text = self.properties[property]

  def print_capabilities(self, node):
    capabilities = node.find('{%s}capabilities' % VOSPACE_NS)
    if capabilities is None:
      capabilities = etree.SubElement(node, '{%s}capabilities' % VOSPACE_NS)
    for capability in self.capabilities:
      view = etree.SubElement(capabilities, '{%s}capability' % VOSPACE_NS)
      view.set('uri', capability)

# ----------------------------------------------------------
class DataNode(Node):

  TYPE = 'vos:DataNode'

  def __init__(self, node = None):
    """
    Creates a new DataNode or one around the specified element.
    """
    Node.__init__(self, node)
    self.accepts = []
    self.provides = []
    self.busy = False
    if node != None:
      if isinstance(node, str): node = etree.fromstring(node)
      accepts = node.find('{%s}accepts' % VOSPACE_NS)
      if accepts is not None:
        for view in accepts:
          self.accepts.append(view.get('uri'))
      provides = node.find('{%s}provides' % VOSPACE_NS)
      if provides is not None:
        for view in provides:
          self.provides.append(view.get('uri'))
      self.busy = toBoolean(node.get('busy'))

  def tostring(self, detail = 'max'):
    node = etree.fromstring(self.blankNode)
    self.print_node(node)
    if detail != 'min': self.print_properties(node)
    if detail == 'max':
      if self.busy:  
        node.set('busy', 'true')
      else:
        node.set('busy', 'false')
      self.print_capabilities(node)
      self.print_accepts(node)
      self.print_provides(node)
    return etree.tostring(node)

  def add_accepts(self, uri):
    self.accepts.append(uri)

  def add_provides(self, uri):
    self.provides.append(uri)

  def clear_accepts(self):
    self.accepts = []

  def clear_provides(self):
    self.provides = []

  def set_busy(self, busy):
    self.busy = busy

  def print_accepts(self, node):
    accepts = node.find('{%s}accepts' % VOSPACE_NS)
    if accepts is None:
      accepts = etree.SubElement(node, '{%s}accepts' % VOSPACE_NS)
    for accept in self.accepts:
      view = etree.SubElement(accepts, '{%s}view' % VOSPACE_NS)
      view.set('uri', accept)

  def print_provides(self, node):
    provides = node.find('{%s}provides' % VOSPACE_NS)
    if provides is None:
      provides = etree.SubElement(node, '{%s}provides' % VOSPACE_NS)
    for provide in self.provides:
      view = etree.SubElement(provides, '{%s}view' % VOSPACE_NS)
      view.set('uri', provide)

# ----------------------------------------------------------
class ContainerNode(DataNode):

  TYPE = 'vos:ContainerNode'

  def __init__(self, node = None):
    """
    Creates a new ContainerNode or one around the specified element.
    """
    DataNode.__init__(self, node)
    self.nodes = []
    if node != None:
      if isinstance(node, str): node = etree.fromstring(node)
      nodes = node.find('{%s}nodes' % VOSPACE_NS)
      if nodes != None:
        for n in nodes:
          self.nodes.append(n.get('uri'))
      else:
        raise VOSpaceError(400, "There is no nodes element.", summary = MISSING_PARAMETER)

  def tostring(self, detail = 'max'):
    node = etree.fromstring(self.blankNode)
    self.print_node(node)
    if detail != 'min': self.print_properties(node)
    if detail == 'max':
      if self.busy:  
        node.set('busy', 'true')
      else:
        node.set('busy', 'false')
      self.print_capabilities(node)
      self.print_accepts(node)
      self.print_provides(node)
      self.print_nodes(node)
    return etree.tostring(node)

  def add_node(self, uri):
    self.nodes.append(uri)

  def clear_nodes(self):
    del self.nodes[:]

  def print_nodes(self, root):
    nodes = etree.SubElement(root, '{%s}nodes' % VOSPACE_NS)
    if nodes is None:
      nodes = etree.SubElement(node, '{%s}nodes' % VOSPACE_NS)
    for child in self.nodes:
      node = etree.SubElement(nodes, '{%s}node' % VOSPACE_NS)
      node.set('uri', child)

# ----------------------------------------------------------
class UnstructuredDataNode(DataNode):

  TYPE = 'vos:UnstructuredDataNode'

# ----------------------------------------------------------
class StructuredDataNode(DataNode):

  TYPE = 'vos:StructuredDataNode'

# ----------------------------------------------------------
class LinkNode(Node):

  TYPE = 'vos:LinkNode'

  def __init__(self, node = None):
    """
    Creates a new LinkNode or one around the specified element.
    """
    Node.__init__(self, node)
    self.target = ''
    if node != None:
      if isinstance(node, str): node = etree.fromstring(node)
      try:
        self.target = node.find('{%s}target' % VOSPACE_NS).text
      except:
        raise VOSpaceError(400, "No target is specified.", summary = MISSING_PARAMETER)

  def tostring(self, detail = 'max'):
    node = etree.fromstring(self.blankNode)
    self.print_node(node)
    if detail != 'min': self.print_properties(node)
    if detail == 'max':
      self.print_capabilities(node)
      target = etree.SubElement(node, '{%s}target' % VOSPACE_NS)
      target.text = self.target
    return etree.tostring(node)

  def set_target(self, target):
    self.target = target

# ------------------------------------------------------------  
class Transfer():

  blankTransfer = '''<vos:transfer xmlns:vos = "%s">
  <vos:target/>
  <vos:direction/>
  <vos:keepBytes/>
  </vos:transfer>''' % VOSPACE_NS

  def __init__(self, transfer = None):
    """
    Creates a new Transfer or one around the specified element.
    """
    self.protocols = []
    self.keepBytes = True
    if transfer == None:
      self.target = ''
      self.direction = ''
      self.view = View()
    else:
      try:
        if isinstance(transfer, str): transfer = etree.fromstring(transfer)
        self.target = transfer.find('{%s}target' % VOSPACE_NS).text
        self.direction = transfer.find('{%s}direction' % VOSPACE_NS).text
        view = transfer.find('{%s}view' % VOSPACE_NS)
        if view is not None:
          self.view = View(view)
        keepBytes = transfer.find('{%s}keepBytes' % VOSPACE_NS)
        if keepBytes is not None:
          self.keepBytes = toBoolean(keepBytes.text)
        for protocol in transfer.xpath('vos:protocol', namespaces = {'vos': VOSPACE_NS}):
          self.protocols.append(Protocol(protocol = protocol))
      except:
        raise VOSpaceError(500, "One of the specified parameters is invalid.")

  def tostring(self):
    transfer = etree.fromstring(self.blankTransfer)
    target = transfer.find('{%s}target' % VOSPACE_NS)
    target.text = self.target
    direction = transfer.find('{%s}direction' % VOSPACE_NS)
    direction.text = self.direction
    transfer.insert(2, etree.fromstring(self.view.tostring()))
    keepBytes = transfer.find('{%s}keepBytes' % VOSPACE_NS)
    keepBytes.text = str(self.keepBytes)
    for protocol in self.protocols:
      transfer.append(etree.fromstring(protocol.tostring()))
    return etree.tostring(transfer)

  def set_protocols(self, protocols):
    self.protocols = protocols

  def set_view(self, view):
    self.view = view

  def add_protocol(self, protocol):
    self.protocols.append(protocol)


# ------------------------------------------------------------  
class Protocol():

  blankProtocol = '''<vos:protocol xmlns:vos="%s" uri=""/>''' % VOSPACE_NS

  def __init__(self, protocol = None):
    """
    Creates a new Protocol or one around the specified element.
    """
    self.uri = ""
    self.endpoint = ""
    self.params = {}
    if protocol != None:
      if isinstance(protocol, str): protocol = etree.fromstring(protocol)
      if isinstance(protocol, etree._ElementTree): protocol = protocol.getroot()
      self.uri = protocol.get('uri')
      endpoint = protocol.find('{%s}endpoint' % VOSPACE_NS)
      if endpoint is not None: self.endpoint = endpoint.text
      for param in protocol.xpath('vos:param', namespaces = {'vos': VOSPACE_NS}):
        self.params[param.get('uri')] = param.text

  def add_param(self, uri, value):
    self.params[uri] = value

  def remove_param(self, uri):
    del self.params[uri]

  def set_endpoint(self, uri):
    self.endpoint = uri

  def tostring(self):
    protocol = etree.fromstring(self.blankProtocol)
    protocol.set('uri', self.uri)
    if self.endpoint != "":
      endpoint = etree.SubElement(protocol, '{%s}endpoint' % VOSPACE_NS)
      endpoint.text = self.endpoint
    for param in self.params:
      par = etree.SubElement(protocol, '{%s}param' % VOSPACE_NS)
      par.set('uri', param)
      par.text = self.params[par]
    return etree.tostring(protocol)

# ------------------------------------------------------------  
class View():

  blankView = '''<vos:view xmlns:vos="%s" uri=""/>''' % VOSPACE_NS

  def __init__(self, view = None):
    """
    Creates a new View or one around the specified element.
    """
    self.uri = ""
    self.original = True
    self.params = {}
    if view != None:
      if isinstance(view, str): view = etree.fromstring(view)
      if isinstance(view, etree._ElementTree): view = view.getroot()
      uri = view.get('uri')
      if uri is not None: self.uri = uri
      original = view.get('original')
      if original is not None: self.original = original
      for param in view.xpath('vos:param', namespaces = {'vos': VOSPACE_NS}):
        self.params[param.get('uri')] = param.text

  def add_param(self, uri, value):
    self.params[uri] = value

  def remove_param(self, uri):
    del self.params[uri]

  def set_original(self, value):
    self.original = value

  def tostring(self):
    view = etree.fromstring(self.blankView)
    view.set('uri', self.uri)
    view.set('original', str(self.original))
    for param in self.params:
      par = etree.SubElement(view, '{%s}param' % VOSPACE_NS)
      par.set('uri', param)
      par.text = self.params[param]
    return etree.tostring(view)
