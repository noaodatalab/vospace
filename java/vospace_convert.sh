#!/bin/bash

if [ $# -lt 1 ]; then db='vospace_test'; else db=$1; fi
if [ $# -lt 2 ]; then host='localhost'; else host=$2; fi

P_MYSQL="/usr/bin/mysql -u dba -pdba -h $host"

exd=`dirname $0`
$P_MYSQL $db < ${exd}/vospace_convert.sql

# Fill the new metaproperties table
for c in $(echo "select identifier from metaproperties" | $P_MYSQL vospace -N | cat - ${exd}/vos_properties.txt | sort -u); do
    echo "INSERT INTO metaproperties (identifier,type,readonly) VALUES ('${c}','4','0');" | $P_MYSQL $db
done

# Create properties table with all the columns
prop_create='CREATE TABLE `properties` (`identifier` varchar(512) NOT NULL'
for c in $(echo "select identifier from metaproperties" | $P_MYSQL $db -N | cut -f2 -d'#' | grep -v 'identifier'); do
    prop_create=$prop_create', `'${c}'` varchar(256) DEFAULT NULL'
done
prop_create=$prop_create', PRIMARY KEY (`identifier`) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;'
echo $prop_create | $P_MYSQL $db

count=$(echo "select count(identifier) from properties" | $P_MYSQL vospace -N)
echo -ne "${count}\r"
echo "select * from properties" | $P_MYSQL vospace -N | while read -r i; do
    id=$(echo $i | cut -d' ' -f1)
    col=$(echo $i | cut -d' ' -f2 | cut -f2 -d'#')
    val=$(echo $i | cut -d' ' -f3)
    echo "INSERT INTO properties (identifier,${col}) VALUES ('${id}','${val}') ON DUPLICATE KEY UPDATE ${col}='${val}';" \
            | $P_MYSQL $db
    count=$((count-1))
    echo -ne "${count} \r"
done
