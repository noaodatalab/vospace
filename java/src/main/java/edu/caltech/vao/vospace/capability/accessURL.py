#!/usr/bin/env python
#
# @file    accessURL.py
# @brief   Download all files referenced by an accessURL tag
# @author  Matthew J. Graham
#
# <!----------------------------------------------------------------------------
# Copyright (c) 2016 by the National Optical Astronomy Observatory
# This software is part of the NOAO DataLab. For more information, visit
# http://datalab.noao.edu
# -------------------------------------------------------------------------- -->

import sys
from httplib2 import Http
from storage import getNode
import grequests
from lxml import etree
import plac

# Main body
# ------------------------------------------------------------------------------

def main(file = None, node = None, endpoint = None):
    '''Parse the specified file'''
    dir = file[: file.rindex("/") + 1]
    nodedir = node[: node.rindex("/") + 1]
    xml = etree.parse(file)
    refcol = findColumn(xml, ucd = "VOX:Image_AccessReference", name = "access_url")
    # Assume no namespaces
    rows = xml.xpath("//DATA/TABLEDATA/TR")
    cols = [row.xpath("TD") for row in rows]
    refs = [col[refcol].text for col in cols]
    # Parallel downloads
    rs = [grequests.get(ref) for ref in refs]
    resps = grequests.map(rs)
    for ref, resp in zip(refs, resps):
        # Write file to disk
        filename = getFilename(ref)
        with open(dir + filename, 'wb') as fd:
            fd.write(resp.content)
        # Register with VOSpace
        registerLocation(nodedir + filename, dir + filename, endpoint)


def exception_handler(request, exception):
    print "Request failed"

        
def findColumn(table, ucd, name = None):
    ''' Find the column in the VOTable corresponding to the given UCD and column name '''
    # Assume no namespaces
    fields = table.xpath('//FIELD')
    col = 0
    for field in fields:
        if field.get("ucd") == ucd:
            if name is None:
                return col
            elif field.get("name") == name:
                return col
            col += 1
        else:
            col += 1
    return -1


def getFilename(ref):
    ''' Get local filename from access URL '''
    start = ref.index('fileRef') + 8
    end = ref.find('&', start)
    filename = ref[start: end] 
    return filename

        
def lcs(S,T):
    m = len(S)
    n = len(T)
    counter = [[0]*(n+1) for x in range(m+1)]
    longest = 0
    lcs_set = set()
    for i in range(m):
        for j in range(n):
            if S[i] == T[j]:
                c = counter[i][j] + 1
                counter[i+1][j+1] = c
                if c > longest:
                    lcs_set = set()
                    longest = c
                    lcs_set.add(S[i-c+1:i+1])
                elif c == longest:
                    lcs_set.add(S[i-c+1:i+1])
    return lcs_set.pop()


def registerLocation(node, location, endpoint):
    ''' Register the location with VOSpace '''
    # uri = vos://xxx.xxx!vospace/<user>/<path>
    # location = file:///<absolute path>
    # Registration endpoint
    part = lcs(node, location)
    path = location[location.index(part):]
    accessURL = "%s%s?location=%s" % (endpoint, path, location)
    # Get node description
    node = getNode(node, location)
    # HTTP PUT to VOSpace /register
    h = Http()
    resp, content = h.request(accessURL, 'PUT', body = node, headers = {'Content-type': 'application/xml'})
    if int(resp['status']) != 201:
        print 'Error with node %s' % node
        sys.exit(-1)
        

# Plac annotations for main function arguments
# ------------------------------------------------------------------------------
# Argument annotations are: (help, kind, abbrev, type, choices, metavar)
# Plac automatically adds a -h argument for help, so no need to do it here.

main._annotations_ = dict(
    file          = ('file containing SIA v1 response', 'option', 'f'),
    node          = ('VOSpace identifier of the file', 'option', 'n'),
    endpoint      = ('registration endpoint for the service', 'option', 'e')
)


# Entry point
# ------------------------------------------------------------------------------

def cli_main():
    plac.call(main)

cli_main()
