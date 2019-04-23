#!/bin/bash

# Argument is <hostname>
wd=$(dirname $0)
createsql=''
for f in ./vospace_create.sql ${wd}/vospace_create.sql; do
    if [ -e $f ]; then createsql=$f; break; fi
done
if [ -z $createsql ]; then echo "No vospace_create.sql found." 1>&2; exit 1; fi
proptable=''
for f in ./vospace_proptable.sh ${wd}/vospace_proptable.sh; do
    if [ -e $f ]; then proptable=$f; break; fi
done
if [ -z $proptable ]; then echo "No vospace_proptable.sh found." 1>&2; exit 1; fi
rootnode=''
for f in ./vospace_rootnode.sh ${wd}/vospace_rootnode.sh; do
    if [ -e $f ]; then rootnode=$f; break; fi
done
if [ -z $rootnode ]; then echo "No vospace_rootnode.sh found." 1>&2; exit 1; fi

cat $createsql
echo "# ${proptable}"
$proptable $*
echo "# ${rootnode}"
$rootnode $*
