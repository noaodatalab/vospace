from resources import *
from urllib import urlencode
import httplib2
from time import sleep
h = httplib2.Http()
h.follow_redirects = False
#---
#resp, content = h.request("http://localhost:8080/vospace-2.0/vospace/nodes")
t = Transfer()
t.target = "vos://nvo.caltech!vospace/data2"
t.direction = "pullFromVoSpace"
v = View('''<vos:view xmlns:vos="http://www.ivoa.net/xml/VOSpace/v2.0" uri="ivo://ivoa.net/vospace/views#votable-1.1"/>''')
t.set_view(v)
p = Protocol('''<vos:protocol xmlns:vos="http://www.ivoa.net/xml/VOSpace/v2.0" uri="ivo://ivoa.net/xml/vospace/core#httpget"/>''')
t.add_protocol(p)
p = Protocol('''<vos:protocol xmlns:vos="http://www.ivoa.net/xml/VOSpace/v2.0" uri="ivo://ivoa.net/xml/vospace/core#ftpget"/>''')
t.add_protocol(p)
t.keepBytes = False
print t.tostring()
resp, content = h.request("http://localhost:8080/vospace-2.0/vospace/transfers", "POST", body = t.tostring())
print resp, content
location = resp['location']
headers = {'Cookie': resp['set-cookie']}
resp, content = h.request(location, headers = headers)
# Launch job 
data = dict(PHASE = "RUN")
headers['content-type'] = "application/x-www-form-urlencoded"
resp, content = h.request('%s/phase' % location, 'POST', urlencode(data), headers = headers)
# Poll for status
del headers['content-type']
sleep(2)
resp, content = h.request(location, headers = headers)
print "A:", content
job = Job(content)
results = job.results['transferDetails']
resp, content = h.request(results, headers = headers)
print "B: ",content
transfer = Transfer(content)
resp, content = h.request(transfer.protocols[0].endpoint)
print "C:",resp, content
sleep(5)
resp, content = h.request(location, headers = headers)
print "D:",content


#---
#resp,content = h.request("http://localhost:8080/vospace-2.0/vospace/nodes/data1")
#print resp, content
#node = StructuredDataNode()
#node.set_uri("vos://nvo.caltech!vospace/data1")
#node.add_property("urn:local:myprop1", "haddock")
#node = '''<vos:node xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:vos="http://www.ivoa.net/xml/VOSpace/v2.0" xsi:type="vos:UnstructuredDataNode" uri="vos://nvo.caltech!vospace/data1" busy="false"><vos:properties/><vos:accepts/><vos:provides/><vos:capabilities/></vos:node>'''
#resp, content = h.request("http://localhost:8080/vospace-2.0/vospace/nodes/data1", "PUT", body = node.tostring(), headers = {'content-type': 'application/xml'})
#print resp, content
#node.add_property("urn:local:myprop1", "turbot")
#resp, content = h.request("http://localhost:8080/vospace-2.0/vospace/nodes/data1", "POST", body = node.tostring(), headers = {'content-type': 'application/xml'})
#print resp, content
