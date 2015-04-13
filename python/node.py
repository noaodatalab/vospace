#!/usr/bin/python -u
# 2010/03/11
# v0.1
#
# node.py
# Python code to handle VOSpace node representations

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

blankNode = '''<node xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
  xmlns = "http://www.ivoa.net/xml/VOSpace/v2.0"
  xsi:type="" uri="">
  <properties/>
  <capabilities/>
  <accepts/>
  <provides/>
</node>'''

class NodeFactory():

  def get_node(self, xml):
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


class Node():

  TYPE = 'vos:Node'
  
  def __init__(self, node = None):
    self.uri = ''
    self.properties = {}
    self.capabilities = []
    if node != None:
      if isinstance(node, str): node = etree.fromstring(node)
      self.uri = node.get('{%s}uri' % VOSPACE_NS)
      properties = node.find('{%s}properties' % VOSPACE_NS)
      for property in properties:
        if property.text in READ_ONLY_PROPERTIES:
          raise VOSpaceError(401, "User does not have permissions to set a readonly property.")
        else:
          self.properties[property.get('uri')] = property.text
      capabilities = node.find('{%s}capabilities' % VOSPACE_NS)
      for capability in capabilities:
        self.capabilities.append(capability.get('uri'))

  def tostring(self):
    node = etree.fromstring(blankNode)
    self.print_node(node)
    self.print_properties(node)
    self.print_capabilities(node)
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
    for property in self.properties:
      prop = etree.SubElement(properties, '{%s}property' % VOSPACE_NS)
      prop.set('uri', property)
      prop.text = self.properties[property]

  def print_capabilities(self, node):
    capabilities = node.find('{%s}capabilities' % VOSPACE_NS)
    for capability in self.capabilities:
      view = etree.SubElement(capabilities, '{%s}capability' % VOSPACE_NS)
      view.set('uri', capability)


class DataNode(Node):

  TYPE = 'vos:DataNode'

  def __init__(self, node = None):
    Node.__init__(self, node)
    self.accepts = []
    self.provides = []
    self.busy = 'false'
    if node != None:
      if isinstance(node, str): node = etree.fromstring(node)
      accepts = node.find('{%s}accepts' % VOSPACE_NS)
      for view in accepts:
        self.accepts.append(view.get('uri'))
      provides = node.find('{%s}provides' % VOSPACE_NS)
      for view in provides:
        self.provides(view.get('uri'))
      self.busy = node.get('busy')

  def tostring(self):
    node = etree.fromstring(blankNode)
    self.print_node(node)
    node.set('busy', self.busy)
    self.print_properties(node)
    self.print_accepts(node)
    self.print_provides(node)
    self.print_capabilities(node)
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
    for accept in self.accepts:
      view = etree.SubElement(accepts, '{%s}view' % VOSPACE_NS)
      view.set('uri', accept)

  def print_provides(self, node):
    provides = node.find('{%s}provides' % VOSPACE_NS)
    for provide in self.provides:
      view = etree.SubElement(provides, '{%s}view' % VOSPACE_NS)
      view.set('uri', provide)


class ContainerNode(DataNode):

  TYPE = 'vos:ContainerNode'

  def __init__(self, node = None):
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

  def tostring(self):
    node = etree.fromstring(blankNode)
    self.print_node(node)
    self.print_properties(node)
    self.print_accepts(node)
    self.print_provides(node)
    self.print_capabilities(node)
    self.print_nodes(node)
    return etree.tostring(node)

  def add_node(self, uri):
    self.nodes.append(uri)

  def clear_nodes(self):
    del self.nodes[:]

  def print_nodes(self, root):
    nodes = etree.SubElement(root, '{%s}nodes' % VOSPACE_NS)
    for child in self.nodes:
      node = etree.SubElement(nodes, '{%s}node' % VOSPACE_NS)
      node.set('uri', child)


class UnstructuredDataNode(DataNode):

  TYPE = 'vos:UnstructuredDataNode'


class StructuredDataNode(DataNode):

  TYPE = 'vos:StructuredDataNode'


class LinkNode(Node):

  TYPE = 'vos:LinkNode'

  def __init__(self, node = None):
    Node.__init__(self, node)
    self.target = ''
    if node != None:
      if isinstance(node, str): node = etree.fromstring(node)
      try:
        self.target = node.find('{%s}target' % VOSPACE_NS).text
      except:
        raise VOSpaceError(400, "No target is specified.", summary = MISSING_PARAMETER)

  def tostring(self):
    node = etree.fromstring(blankNode)
    self.print_node(node)
    target = etree.SubElement(node, '{%s}target' % VOSPACE_NS)
    target.text = self.target
    self.print_properties(node)
    self.print_capabilities(node)
    return etree.tostring(node)

  def set_target(self, target):
    self.target = target

  
