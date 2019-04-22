#!/bin/bash

# Arguments are <username>, <hostname>
d=$(date +%Y-%m-%dT%H:%M:%S%z)
if [ $# -lt 1 ]; then u=$USER; else u=$1; fi
if [ $# -lt 2 ]; then h=$(hostname -s); else h=$2; fi
conffile=''
for f in ./vospace.properties.${h} ${wd}/vospace.properties.${h} \
        ./vospace.properties.default ${wd}/vospace.properties.default; do
    if [ -e $f ]; then conffile=$f; break; fi
done
if [ -z $conffile ]; then echo "No vospace configuration found."; exit 1; fi
echo "# $h $conffile"
rn=$(grep space.rootnode $conffile | cut -d'=' -f2)
rd=$(grep server.http.basedir $conffile | cut -d'=' -f2)

if [ $USER == 'datalab' -o $USER == 'root' ]; then
    # Make sure the directories actually exist
    if [ ! -e ${rd}/${u} ]; then
        sudo /bin/mkdir -p ${rd}/${u}
        sudo /bin/chmod 775 ${rd}/${u}
        sudo /bin/chown datalab:datalab ${rd}/${u}
    fi
    if [ ! -e ${v}/${u}/public ]; then
        sudo /bin/mkdir -p ${rd}/${u}/public
        sudo /bin/chmod 775 ${rd}/${u}/public
        sudo /bin/chown datalab:datalab ${rd}/${u}/public
    fi
    if [ ! -e ${rd}/${u}/tmp ]; then
        sudo /bin/mkdir -p ${rd}/${u}/tmp
        sudo /bin/chmod 770 ${rd}/${u}/tmp
        sudo /bin/chown datalab:datalab ${rd}/${u}/tmp
    fi
fi
sedf=''
for f in /bin/sed /usr/bin/sed; do if [ -e $f ]; then sedf=$f; break; fi; done
if [ -z $sedf ]; then echo "No sed found."; exit 1; fi
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
