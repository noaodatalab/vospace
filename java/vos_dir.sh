#!/bin/bash

# Arguments are <username>, <hostname>, <vospace_root>
d=`date +%Y-%m-%dT%H:%M:%S%z`
hn=`hostname -s`
if [ $# -lt 1 ]; then u=$USER; else u=$1; fi
if [ $# -lt 2 ]; then h=$hn; else h=$2; fi
if [ $# -lt 3 ]; then
    if [ $hn == "dldev" ]; then v="/data/vospace/users/"; else v="/dl2/vospace/users/"; fi
else v=$3; fi

if [ ! -e ${v}/${u}/public ]; then sudo /bin/mkdir -p ${v}/${u}/public; fi
if [ ! -e ${v}/${u}/tmp ]; then sudo /bin/mkdir -p ${v}/${u}/tmp; fi
sudo /bin/chmod 775 ${v}/${u}
sudo /bin/chmod -R 775 ${v}/${u}/public
sudo /bin/chmod -R 770 ${v}/${u}/tmp
sudo /bin/chown -R datalab:datalab ${v}/${u}

/bin/sed -e "s/USER/${u}/g" -e "s/DATE/${d}/g" <<VOBASE | /usr/bin/mysql -u dba -pdba -h ${h} vospace_test
# users/<USER>
insert into nodes(identifier, type, owner, view, location, creationDate, node)
  values('vos://datalab.noao!vospace/USER', 3, 'USER',
     'ivo://ivoa.net/vospace/views/blob',
     'file:///data/vospace/users/USER',
     now(),
     '<node xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
      xsi:type="vos:ContainerNode" \
      uri="vos://datalab.noao!vospace/USER" \
      xmlns="http://www.ivoa.net/xml/VOSpace/v2.0"> \
      <properties> \
	<property uri="ivo://ivoa.net/vospace/core#date">DATE</property> \
	<property readOnly="false" \
	    uri="ivo://ivoa.net/vospace/core#groupread">USER</property> \
	<property readOnly="false" \
	    uri="ivo://ivoa.net/vospace/core#groupwrite">USER</property> \
      </properties> \
      <accepts/> <provides/> <capabilities/> <nodes/> </node>');
insert into properties values('vos://datalab.noao!vospace/USER',
	'ivo://ivoa.net/vospace/core#date', 'DATE');
insert into properties values('vos://datalab.noao!vospace/USER',
	'ivo://ivoa.net/vospace/core#groupread', 'USER');
insert into properties values('vos://datalab.noao!vospace/USER',
	'ivo://ivoa.net/vospace/core#groupwrite', 'USER');

# users/<USER>/public
insert into nodes(identifier, type, owner, view, location, creationDate, node)
  values('vos://datalab.noao!vospace/USER/public', 3, 'USER',
     'ivo://ivoa.net/vospace/views/blob',
     'file:///data/vospace/users/USER/public',
     now(),
     '<node xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
      xsi:type="vos:ContainerNode" \
      uri="vos://datalab.noao!vospace/USER/public" \
      xmlns="http://www.ivoa.net/xml/VOSpace/v2.0"> \
      <properties> \
	<property uri="ivo://ivoa.net/vospace/core#date">DATE</property> \
	<property readOnly="false" \
	    uri="ivo://ivoa.net/vospace/core#groupread">USER</property> \
	<property readOnly="false" \
	    uri="ivo://ivoa.net/vospace/core#groupwrite">USER</property> \
      </properties> \
      <accepts/> <provides/> <capabilities/> <nodes/> </node>');
insert into properties values('vos://datalab.noao!vospace/USER/public',
	'ivo://ivoa.net/vospace/core#date', 'DATE');
insert into properties values('vos://datalab.noao!vospace/USER/public',
	'ivo://ivoa.net/vospace/core#groupread', 'USER');
insert into properties values('vos://datalab.noao!vospace/USER/public',
	'ivo://ivoa.net/vospace/core#groupwrite', 'USER');

# users/<USER>/tmp
insert into nodes(identifier, type, owner, view, location, creationDate, node)
  values('vos://datalab.noao!vospace/USER/tmp', 3, 'USER',
     'ivo://ivoa.net/vospace/views/blob',
     'file:///data/vospace/users/USER/tmp',
     now(),
     '<node xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
      xsi:type="vos:ContainerNode" \
      uri="vos://datalab.noao!vospace/USER/tmp" \
      xmlns="http://www.ivoa.net/xml/VOSpace/v2.0"> \
      <properties> \
	<property uri="ivo://ivoa.net/vospace/core#date">DATE</property> \
	<property readOnly="false" \
	    uri="ivo://ivoa.net/vospace/core#groupread">USER</property> \
	<property readOnly="false" \
	    uri="ivo://ivoa.net/vospace/core#groupwrite">USER</property> \
      </properties> \
      <accepts/> <provides/> <capabilities/> <nodes/> </node>');
insert into properties values('vos://datalab.noao!vospace/USER/tmp',
	'ivo://ivoa.net/vospace/core#date', 'DATE');
insert into properties values('vos://datalab.noao!vospace/USER/tmp',
	'ivo://ivoa.net/vospace/core#groupread', 'USER');
insert into properties values('vos://datalab.noao!vospace/USER/tmp',
	'ivo://ivoa.net/vospace/core#groupwrite', 'USER');
VOBASE
echo "$u VOSpace created on $h"