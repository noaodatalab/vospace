#!/usr/bin/python -u
# 2010/03/09
# v0.1
#
# uws.py
# Python code to handle UWS transactions

from config import UWS_NS, XLINK_NS, XSI_NS

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

class Job():
  
  def __init__(self, job = None):
    '''
    Create a new Job or one around the specified Element.
    '''
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
      if isinstance(job, str): job = etree.fromstring(job)
      print etree.tostring(job)
      self.jobId = job.find('{%s}jobId' % UWS_NS).text
      self.ownerId = job.find('{%s}ownerId' % UWS_NS).text 
      self.phase = job.find('{%s}phase' % UWS_NS).text
      self.startTime = job.find('{%s}startTime' % UWS_NS).text
      self.endTime = job.find('{%s}endTime' % UWS_NS).text 
      self.executionDuration = int(job.find('{%s}executionDuration' % UWS_NS).text)
      parameters = job.find('{%s}parameters' % UWS_NS)
      for param in parameters:
        self.parameters[param.get('id')] = param.text
      jobInfo = job.find('{%s}jobInfo' % UWS_NS)
      if jobInfo != None and len(jobInfo) > 0:
        self.jobInfo = etree.tostring(jobInfo.getchildren()[0])

  def tostring(self):
    """
    Get a string representation of the Job
    """
    job = etree.fromstring(blankJob)
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
      message = etree.SubElement(errorSummary, '{%s}message')
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
    self.jobInfo = jobInfo  
  
  def getResultsAsXml(self):
    results = etree.Element('{%s}results' % UWS_NS)
    for res in self.results:
      result = etree.SubElement(results, '{%s}result' % UWS_NS)
      result.set('id', res)
      result.set('{%s}href' % XLINK_NS, self.results[res])
    return results
