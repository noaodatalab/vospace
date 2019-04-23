#!/bin/bash

wd=$(dirname $0)
propfile=''
for pf in ./src/main/webapp/WEB-INF/classes/ivoa_props.properties ./ivoa_props.properties \
        ${wd}/src/main/webapp/WEB-INF/classes/ivoa_props.properties ${wd}/ivoa_props.properties; do
    if [ -e $pf ]; then propfile=$pf; break; fi
done
if [ -z $propfile ]; then echo "No ivoa_props.properties found." 1>&2; exit 1; fi

# Outputs SQL to fill the metaproperties table and create the properties table.
# Requires the file ivoa_props.properties
prop_create='CREATE TABLE `properties` (\n `identifier` varchar(4096) NOT NULL,'
for c in $(cat $propfile | grep -v '^#' | cut -f1 -d'='); do
    # This does not handle the Accepts/Provides/Contains/Read-only settings.
    # The VOSpace must do that job on startup.
    echo "INSERT INTO metaproperties (identifier,type,readonly) VALUES ('ivo://ivoa.net/vospace/core#${c}','4','0');"
    prop_create=${prop_create}'\n `'${c}'` varchar(256) DEFAULT NULL,'
done
echo -e ${prop_create}'\nINDEX prop_id_idx (`identifier`(767)) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;'
