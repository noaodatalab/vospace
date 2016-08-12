#!/usr/bin/python -u
# 2010/03/30
# v0.1
#
# store.py
# Python code to handle persistant store transactions

from config import CONFIG
#import mysql.connector
import PySQLPool
from resources import *

class LocalStoreManager():
  """
  Class to handle interactions with a local database
  """

  def __init__(self):
    config = CONFIG.dbinfo().copy()
    self.db = PySQLPool.getNewConnection(host = config['host'], user = config['user'], password = config['password'], schema = 'vospace')

  def query(self, sqlQuery):
    query = PySQLPool.getNewQuery(connection = self.db, commitOnEnd = True)
    query.Query(sqlQuery)
    return query.record

  def get_properties(self):
    query = 'select distinct property from properties'
    return self.query(query)

  def create_node(self, node, identifier, type, view = None, status = None, owner = None, location = None):
    view = (view == None) and '' or view
    status = (status == None) and '' or status
    owner = (owner == None) and '' or owner
    location = (location == None) and '' or location
    query = '''insert into nodes (identifier, type, view, status, owner, location, creationDate, node) values('%s', %s, '%s', '%s', '%s', '%s', now(), '%s')''' % (identifier, type, view, status, owner, location, node)
    self.query(query)

  def get_node(self, uri):
    query = '''select node from nodes where identifier = "%s"''' % uri
    return self.query(query)

  def delete_node(self, uri):
    query = 'delete from nodes where identifier like "%s%%"' % uri
    self.query(query) 
    query = 'delete from properties where identifier like "%s%%"' % uri
    self.query(query)

  def update_node(self, target, destination, node):
    query = '''update nodes set identifier = "%s", node = '%s' where identifier = "%s"''' % (destination, node, target)
    self.query(query)

  def get_job(self, id, type = 'transfers', phase = None):
    query = '''select job from jobs where identifier like "%s" and type = "%s"''' % (id, type)
    if phase is not None:
      query += ''' and phase = "%s"''' % phase
    return self.query(query)

  def get_aborted_jobs(self, type = 'transfers'):
    query = '''select identifier from jobs where phase = "ABORTED" and type = "%s"''' % type
    return self.query(query)

  def get_jobs(self, type = 'transfers'):
    query = '''select identifier, phase from jobs where type = "%s"''' % type
    return self.query(query)

  def get_phase(self, identifier):
    query = '''select phase from jobs where identifier = "%s"''' % identifier
    return self.query(query)

  def already_exists(self, type, uri):
    query = '''select count(*) from %s where identifier = "%s"''' % (type, uri)
    rows = self.query(query)
    if rows[0]['count(*)'] > 0:
      return True
    else:
      return False

  def get_node_type(self, uri):
    query = '''select type from nodes where identifier = "%s"''' % (uri)
    return self.query(query)

  def get_children(self, uri):
    query = '''select identifier from nodes where identifier like "%s%%"''' % (uri)
    rows = self.query(query)
    children = [row['identifier'] for row in rows if row['identifier'] != uri and '/' not in row['identifier'][len(uri) + 1:]]
    return children

  def get_all_children(self, uri):
    query = '''select identifier from nodes where identifier like "%s%%"''' % (uri)
    rows = self.query(query)
    children = [row['identifier'] for row in rows if row['identifier'] != uri]
    return children

  def register_properties(self, identifier, properties):
    props = [(identifier, p, properties[p]) for p in properties]
    query = '''insert into properties values (%s, %s, %s)'''
    cursor = PySQLPool.getNewQuery(connection = self.db)
    cursor.executeMany(query, tuple(props))

  def register_job(self, job, identifier, phase = 'QUEUED', userid = None, completed = None, resultid = None, method = None):
    userid = (userid == None) and '' or userid
    resultid = (resultid == None) and '' or resultid
    method = (method == None) and '' or method
    if completed is None:
      query = '''insert into jobs(identifier, type, phase, userid, method, completed, resultid, job) values('%s', 'transfers', '%s', '%s', '%s', null, '%s', '%s')''' % (identifier, phase, userid, method, resultid, job)
    else:
      query = '''insert into jobs(identifier, type, phase, userid, method, completed, resultid, job) values('%s', 'transfers', '%s', '%s', '%s', '%s', '%s', '%s')''' % (identifier, phase, userid, method, completed, resultid, job)
    self.query(query)

  def register_transfer(self, identifier, endpoint):
    query = '''insert into transfers(jobid, endpoint, created) values('%s', '%s', now())''' % (identifier, endpoint)
    self.query(query)

  def register_details(self, identifier, details):
    query = '''insert into results(identifier, details) values ('%s', '%s')''' % (identifier, details)
    self.query(query)

  def get_transfer(self, jobid):
    query = '''select job from jobs where identifier = "%s"''' % jobid
    rows = self.query(query)
    xmljob = etree.fromstring(rows[0]['job'])
    return Job(job = xmljob)

  def update_job(self, job = None, completed = False):
    if completed:
      query = '''update jobs set job = '%s', phase = 'COMPLETED', completed = now() where identifier = "%s"''' % (job.tostring(), job.jobId)
    else: 
      query = '''update jobs set job = '%s', phase = '%s' where identifier = "%s"''' % (job.tostring(), job.phase, job.jobId)
    self.query(query)

  def get_job_details(self, jobid):
    query = '''select type, resultid, method from jobs j where identifier = "%s"''' % jobid
    return self.query(query)

  def get_job_type(self, jobid):
    query = '''select type from jobs where identifier = "%s"''' % jobid
    return self.query(query)

  def get_location(self, identifier):
    query = '''select location from nodes where identifier = "%s"''' % identifier
    return self.query(query)

  def get_results(self, identifier):
    query = '''select details from results where identifier = "%s"''' % identifier
    return self.query(query)

  def get_endpoint(self, endpoint):
    query = '''select jobid, created, completed from transfers where endpoint like "%%%s%%"''' % endpoint
    return self.query(query)

  def complete_transfers(self, jobid):
    query = '''update transfers set completed = now() where jobid = "%s"''' % jobid
    self.query(query)

  def get_transfer_completed(self, jobid):
    query = '''select completed from transfers where jobid = "%s"''' % jobid
    return self.query(query)

  def get_node_location(self, node):
    query = '''select location from nodes where identifier = "%s"''' % node
    return self.query(query)
