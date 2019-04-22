#!/bin/bash

# Arguments are <username>, <hostname>
sedf=''
for f in /bin/sed /usr/bin/sed; do if [ -e $f ]; then sedf=$f; break; fi; done
if [ -z $sedf ]; then echo "No sed found."; exit 1; fi

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
rd=$(grep server.http.basedir $conffile | cut -d'=' -f2)

# Make sure the directories actually exist
if [ $USER == 'datalab' -o $USER == 'root' ]; then
    if [ ! -e ${rd}/${u} ]; then
        sudo /bin/mkdir -p ${rd}/${u}
        sudo /bin/chmod 775 ${rd}/${u}
        if [ $USER == 'datalab' ]; then sudo /bin/chown datalab:datalab ${rd}/${u}; fi
    fi
    if [ ! -e ${v}/${u}/public ]; then
        sudo /bin/mkdir -p ${rd}/${u}/public
        sudo /bin/chmod 775 ${rd}/${u}/public
        if [ $USER == 'datalab' ]; then sudo /bin/chown datalab:datalab ${rd}/${u}/public; fi
    fi
    if [ ! -e ${rd}/${u}/tmp ]; then
        sudo /bin/mkdir -p ${rd}/${u}/tmp
        sudo /bin/chmod 770 ${rd}/${u}/tmp
        if [ $USER == 'datalab' ]; then sudo /bin/chown datalab:datalab ${rd}/${u}/tmp; fi
    fi
fi