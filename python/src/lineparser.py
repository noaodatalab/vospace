# Code for parsing command line arguments with subtasks

import os
import sys
import logging
from optparse import Values
from __version__ import version

def parseSelf(obj):
  opt = Values()
  for attr in dir(obj):
    if isinstance(getattr(obj, attr), Option):
      opt.ensure_value(attr, getattr(obj, attr).value)
  return opt 


def parseArgs(task, args):
  ''' Parse task parameters '''  
  params = []
  if any(x in args for x in ['--help', '--h', '-h', 'help', '-help']):
    print "The '%s' task takes the following parameters:" % task.name
    for par in task.params:
      opt = getattr(task, par)
      name = opt.display and opt.display or par
      if opt.required:
        print "  %s - %s [required]" % (name, opt.description)
      else:
        print "  %s - %s [optional]" % (name, opt.description)
    sys.exit()
  else:
    # Set task parameters
    for i in range(2, len(args)):
      parts = args[i].strip().split("=") 
      name = parts[0][0] == "-" and parts[0][2:] or parts[0]
      params.append(name)
      if hasattr(task, name):
        opt = getattr(task, name)
        opt.value = '='.join(parts[1:])
      else:
        print "The parameter '%s' is not supported by this task" % name
        sys.exit(-1)
    # Set logging
    task.setLogger()
    # Check that required parameters are not missing
    for par in task.params:
      if getattr(task, par).required and par not in params:
        opt = getattr(task, par)
        disp = opt.display and opt.display or par
        var = raw_input("%s (default: %s): " % (disp, opt.default))
        if var == "":
          opt.value = opt.default
        else:
          opt.value = var
#        print "The parameter '%s' is not set but is required by this task" % par
#        sys.exit(-1)


class Option:
  ''' Represents an option '''
  def __init__(self, name, value, description, display = None, default = None, required = False):
    self.name = name
    self.value = value
    self.display = display
    self.description = description
    self.default = default
    self.required = required


  def __str__(self):
    return self.value


class Task:
  ''' Superclass to represent a task '''
  def __init__(self, parent, name, description):
    self.dl = parent
    self.name = name
    self.description = description
    self.logger = None
    self.params = []
    self.addOption("debug", Option("debug", "", "print debug log level messages", default = False))
    self.addOption("verbose", Option("verbose", "", "print verbose level log messages", default = False))
    self.addOption("warning", Option("warning", "", "print warning level log messages", default = False))

  def run(self):
    pass

  def addOption(self, name, option):
    self.params.append(name)
    setattr(self, name, option)

  def addLogger(self, logLevel, logFile):
    logFormat = ("%(asctime)s %(thread)d vos-"+str(version)+"%(module)s.%(funcName)s.%(lineno)d %(message)s")
    logging.basicConfig(level = logLevel, format = logFormat,
                        filename = os.path.abspath(logFile))
    self.logger = logging.getLogger()
    self.logger.setLevel(logLevel)
    self.logger.addHandler(logging.StreamHandler())

  def setLogger(self, logLevel = None):
    if logLevel is None:
      logLevel = logging.ERROR
      if self.verbose: logLevel = logging.INFO
      if self.debug: logLevel = logging.DEBUG
      if self.warning: logLevel = logging.WARNING
    else:
      logLevel = logLevel
    self.addLogger(logLevel, "/tmp/admintool.err")

  def setOption(self, name, value):
    if hasattr(self, name):
      opt = getattr(self, name)
      opt.value = value
    else:
      print "Task '%s' has no option '%s'" % (self.name, name)
