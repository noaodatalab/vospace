#!/bin/bash

# Outputs the SQL to create a VOSpace database in a MySQL/MariaDB server.
# Argument is <hostname>; the hostname of the VOSpace server instance.
wd=$(dirname $0)
createsql=''
for f in ./create.sql ${wd}/create.sql; do
    if [ -e $f ]; then createsql=$f; break; fi
done
if [ -z $createsql ]; then echo "No create.sql found." 1>&2; exit 1; fi
proptable=''
for f in ./proptable.sh ${wd}/proptable.sh; do
    if [ -e $f ]; then proptable=$f; break; fi
done
if [ -z $proptable ]; then echo "No proptable.sh found." 1>&2; exit 1; fi
rootnode=''
for f in ./rootnode.sh ${wd}/rootnode.sh; do
    if [ -e $f ]; then rootnode=$f; break; fi
done
if [ -z $rootnode ]; then echo "No rootnode.sh found." 1>&2; exit 1; fi

cat $createsql
echo "# ${proptable}"
$proptable $*
echo "# ${rootnode}"
$rootnode $*
