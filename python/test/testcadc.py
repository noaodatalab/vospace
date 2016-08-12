from httplib2 import Http
from lxml import etree, html, objectify
from resources import *
from urllib import urlencode, unquote_plus

VOS_NS = "http://www.ivoa.net/xml/VOSpace/v2.0"

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
    # Need to insert protocol after view
    idx = transfer.getroot().index(transfer.find('{%s}view' % VOS_NS))
    transfer.getroot().insert(idx + 1, etree.Element('{%s}protocol' % VOS_NS))
    elem = transfer.find('{%s}protocol' % VOS_NS)
  elem.set('uri', protocol)
  if endpoint is not None:
    end = transfer.find('{%s}endpoint' % VOS_NS)
    if end is None:
      end = etree.SubElement(elem, '{%s}endpoint' % VOS_NS)
    end.text = endpoint
  print etree.tostring(transfer)

def set_transfer_keepBytes(transfer, keepbytes):
  """
  Set the keepBytes of a transfer to the specified value
  """
  elem = transfer.find('{%s}keepBytes' % VOS_NS)
  elem.text = str(keepbytes)

def run():
    h = Http()
    transfer = etree.parse('test/transfer.xml')
    set_transfer_target(transfer, 'vos://cadc.nrc.ca!vospace/lauren/temp/crap.txt')
    set_transfer_direction(transfer, 'pullFromVoSpace')
    set_transfer_view(transfer, 'ivo://ivoa.net/vospace/core#defaultview')
    set_transfer_protocol(transfer, 'ivo://ivoa.net/vospace/core#httpget')
    headers = {'Content-Type': 'application/x-www-form-urlencoded'}
    resp, content = h.request("http://www.canfar.phys.uvic.ca/vospace/" + 'transfers', 'POST', body = etree.tostring(transfer), headers = headers)
    print resp, content
    job = Job(content)
    data = dict(PHASE = "RUN")
    resp, content = h.request("http://www.canfar.phys.uvic.ca/vospace/transfers/" + '%s/phase' % (job.jobId), 'POST', urlencode(data), headers = headers)
    print content
    
#    location = resp['location']
#    assert int(resp['status']) == 302
#    while int(resp['status']) != 200:
#      sleep(1)
#      resp, content = self.h.request(location)
#    transfer = Transfer(content)
#    resp, content = self.h.request(transfer.protocols[0].endpoint)
#    file = open('test/check.vot', 'wb')
#    file.write(content)
#    file.close()


if __name__ == '__main__':
  run()
