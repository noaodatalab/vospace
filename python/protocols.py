#!/usr/bin/python -u
# 2010/03/30
# v0.1
#
# admin.py
# Python code to handle transfer protocols

import cgi
import cherrypy
import os
import pycurl
import shutil
import sys
import tempfile
import uuid

class HttpHandler():
  
  def __init__(self, base_url, data_dir):
    self.LOCATION_ENDPOINT = base_url
    self.DATA_DIR = data_dir

  def get_endpoint(self):
    """
    Get a valid transfer endpoint 
    """
    return self.LOCATION_ENDPOINT + "/" + uuid.uuid4().hex

  def manage_file(self, location, request):
    """
    Write the uploaded file (from the HTTP request) to disk
    """
    location = open(location, 'w')
    try:
      cherrypy.response.timeout = 3600
      lcHDRS = {}
      for key, val in cherrypy.request.headers.iteritems():
        lcHDRS[key.lower()] = val
      formFields = cgi.FieldStorage(fp=cherrypy.request.rfile,
                                    headers=lcHDRS,
                                    environ={'REQUEST_METHOD':'PUT'},
                                    keep_blank_values=True)
      location.write(formFields.file.read())
      location.close()
    except Exception, e:
      print "HttpHandler:", e
      return False
    return True

  def load_data(self, endpoint, location):
    """
    Download the specified file and save to the specified location
    """
    fp = tempfile.NamedTemporaryFile(delete = False)
    curl = pycurl.Curl()
    curl.setopt(pycurl.URL, endpoint)
    curl.setopt(pycurl.WRITEFUNCTION, fp.write)
    curl.perform()
    curl.close()
    fp.close()
    shutil.move(fp.name, location)
    return True
      
  def send_data(self, endpoint, location):
    """
    Send the specified file to the specified endpoint
    """
    curl = pycurl.Curl()
    curl.setopt(pycurl.URL, endpoint)
    curl.setopt(pycurl.UPLOAD, 1)
    curl.setopt(pycurl.READFUNCTION, open(location, 'rb').read)
    filesize = os.path.getsize(location)
    curl.setopt(pycurl.INFILESIZE, filesize)
    curl.perform()
    curl.close()
    return True
