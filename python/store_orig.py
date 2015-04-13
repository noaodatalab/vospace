#!/usr/bin/python -u
# 2010/03/30
# v0.1
#
# store.py
# Python code to handle persistant store transactions

from config import CONFIG
#import mysql.connector
import PySQLPool
from uws import *

class LocalStoreManager():
  """
  Class to handle interactions with a local database
  """

  def __init__(self):
    config = CONFIG.dbinfo().copy()
#    self.db = mysql.connector.Connect(**config)
    self.db = PySQLPool.getNewConnection(host = config['host'], user = config['user'], password = config['password'], schema = 'vospace')

  def query(self, sqlQuery):
#    cursor = self.db.cursor()
#    cursor.execute(sqlQuery)
#    rows =  cursor.fetchall()
#    return rows
    query = PySQLPool.getNewQuery(connection = self.db)
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
    cursor = self.db.cursor()
    cursor.execute(query)

  def get_node(self, uri):
    query = '''select node from nodes where identifier = "%s"''' % uri
    return self.query(query)

  def delete_node(self, uri):
    cursor = self.db.cursor()
    query = 'delete from nodes where identifier = "%s"' % uri
    cursor.execute(query)
    query = 'delete from properties where identifier = "%s"' % uri
    cursor.execute(query)

  def update_node(self, target, destination, node):
    query = '''update nodes set identifier = "%s", node = "%s" where identifier = "node"'''
    cursor = self.db.cursor()
    cursor.execute(query)

  def get_job(self, id, type = 'transfers', phase = None):
    query = '''select job from jobs where identifier like "%s" and type = "%s"''' % (id, type)
    if phase is not None:
      query += ''' and phase = "%s"''' % phase
    return self.query(query)

  def get_aborted_jobs(self, type = 'transfers'):
    query = '''select identifier from jobs where phase = "ABORTED" and type = "%s"''' % type
    return self.query(query)

  def already_exists(self, type, uri):
    query = '''select count(*) from %s where identifier = "%s"''' % (type, uri)
    rows = self.query(query)
    if rows[0][0] > 0:
      return True
    else:
      return False

  def get_node_type(self, uri):
    query = '''select type from nodes where identifier = "%s"''' % (uri)
    return self.query(query)

  def get_children(self, uri):
    query = '''select identifier from nodes where identifier like "%s%%"''' % (uri)
    rows = self.query(query)
    children = [row[0] for row in rows if row[0] != uri]
    return children

  def register_properties(self, identifier, properties):
    props = [(identifier, p, properties[p]) for p in properties]
    query = '''insert into properties values (%s, %s, %s)'''
    cursor = self.db.cursor()
    cursor.executemany(query, tuple(props))

  def register_job(self, job, identifier, phase = 'QUEUED', userid = None, completed = None):
    userid = (userid == None) and '' or userid
    completed = (completed == None) and '' or completed
    query = '''insert into jobs(identifier, type, phase, userid, completed, job) values('%s', 'transfers', '%s', '%s', '%s', '%s')''' % (identifier, phase, userid, completed, job)
    cursor = self.db.cursor()
    cursor.execute(query)

  def register_transfer(self, identifier, target, endpoint, method):
    query = '''insert into transfers(identifier, location, endpoint, method) values('%s', '%s', '%s', '%s')''' % (identifier, target, endpoint, method)
    cursor = self.db.cursor()
    status = cursor.execute(query)

  def get_transfer(self, jobid):
    query = '''select job from jobs where identifier = "%s"''' % jobid
    rows = self.query(query)
    xmljob = etree.fromstring(rows[0][0])
    return Job(job = xmljob)

  def update_job(self, job = None, completed = False):
    if completed:
      query = '''update jobs set job = '%s', completed = now() where identifier = "%s"''' % (job.tostring(), job.jobId)
    else: 
      query = '''update jobs set job = '%s', phase = '%s' where identifier = "%s"''' % (job.tostring(), job.phase, job.jobId)
    cursor = self.db.cursor()
    cursor.execute(query)

  def get_job_method(self, jobid):
    query = '''select type from jobs where identifier = "%s"''' % jobid
    rows = self.query(query)
    query = '''select method from %s where identifier = "%s"''' % (rows[0][0], jobid)
    return self.query(query)
