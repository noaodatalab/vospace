#!/bin/bash

if [ $# -lt 1 ]; then db='vospace_new'; else db=$1; fi
if [ $# -lt 2 ]; then host='localhost'; else host=$2; fi

P_MYSQL="/usr/bin/mysql -u dba -pdba -h $host"

exd=`dirname $0`
$P_MYSQL $db < ${exd}/vospace_create.sql

# Fill the new metaproperties table
for c in $(cat ${exd}/src/main/webapp/WEB-INF/classes/ivoa_props.properties | grep -v '^#' | cut -f1 -d'='); do
    # This does not handle the Accepts/Provides/Contains/Read-only settings.
    # The VOSpace must do that job on startup.
    echo "INSERT INTO metaproperties (identifier,type,readonly) VALUES ('ivo://ivoa.net/vospace/core#${c}','4','0');" | $P_MYSQL $db
done

# Create properties table with all the columns
prop_create='CREATE TABLE `properties` (`identifier` varchar(4096) NOT NULL'
for c in $(echo "select identifier from metaproperties" | $P_MYSQL $db -N | cut -f2 -d'#' | grep -v 'identifier'); do
    prop_create=$prop_create', `'${c}'` varchar(256) DEFAULT NULL'
done
prop_create=$prop_create', PRIMARY KEY (`identifier`(767)) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;'
echo $prop_create | $P_MYSQL $db
