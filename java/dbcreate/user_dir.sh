#!/bin/bash

# Creates the VOSpace user directories for a VOSpace server instance.
# Arguments are <username>, <hostname>
if [ $# -lt 1 ]; then u=$USER; else u=$1; fi
if [ $# -lt 2 ]; then h=$(hostname -s); else h=$2; fi
conffile=''
wd=$(dirname $0)
for f in ./properties.${h} ../config/properties.${h} ${wd}/properties.${h} ${wd}/../config/properties.${h} \
        ./properties.default ../config/properties.default ${wd}/properties.default ${wd}/../config/properties.default; do
    if [ -e $f ]; then conffile=$f; break; fi
done
if [ -z $conffile ]; then echo "No vospace configuration found." 1>&2; exit 1; fi
echo "# $h $conffile"
rd=$(grep server.http.basedir $conffile | cut -d'=' -f2)

# Create the user directories
if [ "$h" == 'docker' -o "$USER" == 'root' ]; then dosudo=""; else dosudo="sudo"; fi
if [ ! -e ${rd}/${u} ]; then
    $dosudo /bin/mkdir -p ${rd}/${u}
    $dosudo /bin/chmod 775 ${rd}/${u}
    if [ "$USER" == 'datalab' ]; then $dosudo /bin/chown datalab:datalab ${rd}/${u}; fi
fi
if [ ! -e ${v}/${u}/public ]; then
    $dosudo /bin/mkdir -p ${rd}/${u}/public
    $dosudo /bin/chmod 775 ${rd}/${u}/public
    if [ "$USER" == 'datalab' ]; then $dosudo /bin/chown datalab:datalab ${rd}/${u}/public; fi
fi
if [ ! -e ${rd}/${u}/tmp ]; then
    $dosudo /bin/mkdir -p ${rd}/${u}/tmp
    $dosudo /bin/chmod 770 ${rd}/${u}/tmp
    if [ "$USER" == 'datalab' ]; then $dosudo /bin/chown datalab:datalab ${rd}/${u}/tmp; fi
fi
