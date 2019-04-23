#!/bin/bash

# Outputs the SQL to create a VOSpace user in a VOSpace database.
# Arguments are <username>, <hostname>
sedf=''
for f in /bin/sed /usr/bin/sed; do if [ -e $f ]; then sedf=$f; break; fi; done
if [ -z $sedf ]; then echo "No sed found." 1>&2; exit 1; fi

d=$(date +%Y-%m-%dT%H:%M:%S%z)
if [ $# -lt 1 ]; then u=$USER; else u=$1; fi
if [ $# -lt 2 ]; then h=$(hostname -s); else h=$2; fi
conffile=''
wd=$(dirname $0)
for f in ./vospace.properties.${h} ${wd}/vospace.properties.${h} \
        ./vospace.properties.default ${wd}/vospace.properties.default; do
    if [ -e $f ]; then conffile=$f; break; fi
done
if [ -z $conffile ]; then echo "No vospace configuration found." 1>&2; exit 1; fi
echo "# $h $conffile"
rn=$(grep space.rootnode $conffile | cut -d'=' -f2)
rd=$(grep server.http.basedir $conffile | cut -d'=' -f2)

$sedf -e "s/USER/${u}/g" -e "s/DATE/${d}/g" -e "s|RNODE|${rn}|g" -e "s|RDIR|${rd}|g" <<VOBASE
# users/<USER>
insert ignore into nodes(identifier, depth, type, owner, view, location, creationDate)
    values('RNODE/USER', 0, 3, 'USER',
    'ivo://ivoa.net/vospace/views/blob', 'file://RDIR/USER', now());
insert ignore into properties(identifier, date, ctime, btime, mtime, groupread, groupwrite, ispublic, publicread)
    values('RNODE/USER', 'DATE', 'DATE', 'DATE', 'DATE', 'USER', 'USER', 'False', 'False');

# users/<USER>/public
insert ignore into nodes(identifier, depth, type, owner, view, location, creationDate)
    values('RNODE/USER/public', 1, 3, 'USER',
    'ivo://ivoa.net/vospace/views/blob', 'file://RDIR/USER/public', now());
insert ignore into properties(identifier, date, ctime, btime, mtime, groupread, groupwrite, ispublic, publicread)
    values('RNODE/USER/public', 'DATE', 'DATE', 'DATE', 'DATE', 'USER', 'USER', 'True', 'True');

# users/<USER>/tmp
insert ignore into nodes(identifier, depth, type, owner, view, location, creationDate)
    values('RNODE/USER/tmp', 1, 3, 'USER',
    'ivo://ivoa.net/vospace/views/blob', 'file://RDIR/USER/tmp', now());
insert ignore into properties(identifier, date, ctime, btime, mtime, groupread, groupwrite, ispublic, publicread)
    values('RNODE/USER/tmp', 'DATE', 'DATE', 'DATE', 'DATE', 'USER', 'USER', 'False', 'False');
VOBASE
