#!/bin/bash

if [ $# -lt 1 ]; then u=$USER; else u=$1; fi
if [ $# -lt 2 ]; then h=$(hostname -s); else h=$2; fi
d=`date +%Y-%m-%dT%H:%M:%S%z`
/bin/sed -e "s/USER/${u}/g" -e "s/DATE/${d}/g" <<VOBASE | /usr/bin/mysql -u dba -pdba -h ${h} vospace_test
# users/<USER>
insert into nodes(identifier, type, owner, view, location, creationDate)
    values('vos://datalab.noao!vospace/USER', 3, 'USER',
    'ivo://ivoa.net/vospace/views/blob', 'file:///data/vospace/users/USER', now());
insert into properties(identifier, date, groupread, groupwrite)
    values('vos://datalab.noao!vospace/USER', 'DATE', 'USER', 'USER');

# users/<USER>/public
insert into nodes(identifier, type, owner, view, location, creationDate)
    values('vos://datalab.noao!vospace/USER/public', 3, 'USER',
    'ivo://ivoa.net/vospace/views/blob', 'file:///data/vospace/users/USER/public', now());
insert into properties(identifier, date, groupread, groupwrite)
    values('vos://datalab.noao!vospace/USER/public', 'DATE', 'USER', 'USER');

# users/<USER>/tmp
insert into nodes(identifier, type, owner, view, location, creationDate)
    values('vos://datalab.noao!vospace/USER/tmp', 3, 'USER',
    'ivo://ivoa.net/vospace/views/blob', 'file:///data/vospace/users/USER/tmp', now());
insert into properties(identifier, date, groupread, groupwrite)
    values('vos://datalab.noao!vospace/USER/tmp', 'DATE', 'USER', 'USER');
VOBASE
echo "$u VOSpace created on $h"