#!/bin/bash

# Argument is <hostname>
sedf=''
for f in /bin/sed /usr/bin/sed; do if [ -e $f ]; then sedf=$f; break; fi; done
if [ -z $sedf ]; then echo "No sed found." 1>&2; exit 1; fi

d=$(date +%Y-%m-%dT%H:%M:%S%z)
if [ $# -lt 1 ]; then h=$(hostname -s); else h=$1; fi
wd=$(dirname $0)
conffile=''
for f in ./vospace.properties.${h} ${wd}/vospace.properties.${h} \
        ./vospace.properties.default ${wd}/vospace.properties.default; do
    if [ -e $f ]; then conffile=$f; break; fi
done
if [ -z $conffile ]; then echo "No vospace configuration found." 1>&2; exit 1; fi
echo "# $h $conffile"
rn=$(grep space.rootnode $conffile | cut -d'=' -f2)
rd=$(grep server.http.basedir $conffile | cut -d'=' -f2)
$sedf -e "s/DATE/${d}/g" -e "s|RNODE|${rn}|g" -e "s|RDIR|${rd}|g" <<VOBASE
# ROOT NODE
insert ignore into nodes(identifier, depth, type, owner, view, location, creationDate)
    values('RNODE', -1, 3, 'root', 'ivo://ivoa.net/vospace/views/blob', 'file://RDIR', now());
insert ignore into properties(identifier, date, ctime, btime, mtime, groupread, groupwrite, ispublic, publicread)
    values('RNODE', 'DATE', 'DATE', 'DATE', 'DATE', 'root', 'root', 'False', 'False');
VOBASE
