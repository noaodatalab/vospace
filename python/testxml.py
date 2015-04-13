from testclient import XMLMatcher
from lxml import etree
from node import *
a = '''<root><a></a><b n1="" n2=""/></root>''' 
b = '''<root><a/><b n2 = "" n1 = ""/></root>'''
c = '''<root><b n1="" n2=""/><a/></root>'''
xm = XMLMatcher()
#ax = etree.fromstring(a)
#bx = etree.fromstring(b)
#xm.assert_xml_tree(ax, bx)
a = LinkNode().tostring()
c = open('test/linknode.xml').read()
xm.assert_xml_strings(a, b)

