#!/usr/bin/env python
#
#  CAPRUNNER -- Simple service endpoint for running VOSpace capabilities.
#
#  Usage:
#	capRunner [-p <port> | --port=<port>]
#
#  Endpoints
#
#	/init	    POST	Initialize the task by uploading and processing
#				    the capability configuration file
#	/shutdown   GET		Shutdown the task and exit
#	/pause      GET		Pause (i.e. ignore) new notifications
#	/resume     GET		Resume processing of new notfications
#	/notify     GET		Notify handler for new file, GET method
#					/notify?name=<filename>
#	/notify     POST	Notify handler for new file, POST method
#					POST data: "name=<filename>"
#

import optparse
from flask import Flask
from flask import request
from string import Template
from subprocess import PIPE, Popen, STDOUT 

app = Flask(__name__)

doProcessing = 1			# global processing flag
localproc = 1               # global flag to do local computation 

conf = {}    # global configuration

# Test toplevel endpoint
@app.route('/')
def hello():
    return "Hello World from capRunner!\n"


# INIT -- Initialize the capability by processing the configuration file.
#
@app.route('/init', methods=['POST', ])
def capInit():
    if request.method == 'POST':
        f = request.files['config']
	"""
	    Process the config file to setup vars needed to submit to
		the Job Manager, logging information, etc.
	"""
    parseConfig(f)
    return "Init Runner\n"


def parseConfig(file):
    rawconf = open(file[7:]).read() # Remove file://
    lines = rawconf.strip().split("\n")
    for line in lines:
        parts = line.strip().split("=")
        if '{' in parts[1]:
          conf[parts[0].strip()] = '='.join(parts[1:])
        else:
          conf[parts[0].strip()] = parts[1].strip()    

          
# SHUTDOWN -- Exit the task and clean up.
#
#  Utility shutdown procedure
def shutdown_server():
    func = request.environ.get('werkzeug.server.shutdown')
    if func is None:
        raise RuntimeError('Not running with the Werkzeug Server')
    func()

@app.route('/shutdown')
def shutdown():
    shutdown_server()
    return "Runner shutting down ....\n"


# PAUSE -- Pause processing of new notifications until further notice.
#
@app.route('/pause')
def pause():
    global doProcessing
    doProcessing = 0
    return "Paused processing in application!\n"


# RESUME -- RESUME processing of new notifications.
#
@app.route('/resume')
def resume():
    global doProcessing
    doProcessing = 1
    return "Resumed processing in application!\n"


# NOTIFY -- Notification handler for new files.
#
@app.route('/notify', methods=['GET', 'POST'])
def notify():
    if doProcessing == 0:
	return "Processing is paused ....\n"

    #  Do any needed processing of the arguments and submit to Job Manager.
    #  In dev application, simply log the call ...

    if request.method == 'POST':
        fname = request.form['name']
        if 'name' in request.form:
            conf['file'] = request.form['name'][7:] # Remove file://
            conf['table'] = conf['file'][conf['file'].rindex("/") + 1:].replace(".", "_")
        # Run locally
        if localproc:
            temp = Template(conf['cmd'][1:-1])  # Remove { } around cmd
            cmd = temp.safe_substitute(conf)
            pipe = Popen(cmd, shell = True, stdout = PIPE, stderr = STDOUT)
            output = pipe.stdout.read()
        else:
            pass
        
        return "Hello POST file: " + fname + " " + output + " " + cmd + "\n"
    
    else:
        fname = request.args.get('name', '')
        return "GET call with arg '" + fname + "' " + str(doProcessing) + "\n"

    
# DEBUG -- Return debugging information
#
@app.route('/debug')    
def debug():
  return str(conf)


#  Application MAIN
#
if __name__ == '__main__':
    #   Parse the arguments
    parser = optparse.OptionParser()
    parser.add_option ('--port', '-p', action="store", dest="port",
                        help="Listener port number", default=5000)
    parser.add_option ('--config', '-c', dest="config",
                        help="Configuration file", default=None) 
    options, args = parser.parse_args()

    parseConfig(options.config)
    
    #   Start the application running on the requested port
    app.debug = True
    app.run ('0.0.0.0', int(options.port))
