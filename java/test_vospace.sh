#!/bin/bash

# Arguments are <hostname> <username>
if [ $# -lt 1 ]; then h="dldev.datalab.noao.edu"; else h=$1; fi
if [ $# -lt 2 ]; then u=$USER; else u=$2; fi
if [ $h != "localhost" ]; then
    if [ ! -e $HOME/.datalab/id_token.${u} ]; then
        echo "No token file $HOME/.datalab/id_token.${u}." 1>&2; exit
    fi
    token="X-Dl-Authtoken: $(cat $HOME/.datalab/id_token.${u})"
else
    token="X-Dl-Authtoken: vosuser.1.1.$1$salt$checksum"
fi
wd=$(dirname $0)
if [ $h == "localhost" ]; then pf="docker"; else pf=${h%%.*}; fi
for f in ./vospace.properties.${pf} ${wd}/vospace.properties.${pf} \
        ./vospace.properties.default ${wd}/vospace.properties.default; do
    if [ -e $f ]; then conffile=$f; break; fi
done
if [ -z $conffile ]; then echo "No vospace configuration found." 1>&2; exit 1; fi
echo "Config: $u $h $conffile"

ROOT=$(grep space.rootnode $conffile | cut -d'=' -f2)
BASE="http://${h}:8080/vospace-2.0/vospace"

read -r -d '' LINKNODE <<'EOF'
<ns0:node xmlns:ns0="http://www.ivoa.net/xml/VOSpace/v2.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" uri="URI" xsi:type="vos:LinkNode">
  <ns0:properties />
  <ns0:accepts />
  <ns0:provides />
  <ns0:capabilities />
<ns0:target>TARGET</ns0:target></ns0:node>
EOF

read -r -d '' DATANODE <<'EOF'
<ns0:node xmlns:ns0="http://www.ivoa.net/xml/VOSpace/v2.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" busy="false" uri="URI" xsi:type="vos:DataNode">
  <ns0:properties>
    <ns0:property uri="ivo://datalab.noao.edu/vospace/core#testprop">testval</ns0:property>
  </ns0:properties>
  <ns0:accepts />
  <ns0:provides />
  <ns0:capabilities />
</ns0:node>
EOF

read -r -d '' CONTAINER <<'EOF'
<ns0:node xmlns:ns0="http://www.ivoa.net/xml/VOSpace/v2.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" uri="URI" xsi:type="vos:ContainerNode">
  <ns0:properties>
    <ns0:property uri="ivo://datalab.noao.edu/vospace/core#testprop">testval</ns0:property>
  </ns0:properties>
  <ns0:accepts />
  <ns0:provides />
  <ns0:capabilities />
<ns0:nodes /></ns0:node>
EOF

# URL, protocol
function vo_curl {
    local curl_args=()
    if [ $# -gt 1 ]; then
        curl_args+=("-X" "${2}")
        if [ "${2}" == "PUT" ]; then curl_args+=("-H" "Content-type: text/xml" "-d" "@-"); fi
    fi
    curl -siH "${token}" -w'\n' "${curl_args[@]}" "${BASE}/${1}" \
            | sed -Ee '1s/^.*([[:digit:]]{3}).*$/\1/' -e '2,/^[[:space:]]*$/d'
}

function vo_get {
    while [ $# -ne 0 ]; do
        echo "--- GET ${1} ---"
        local vout=$(vo_curl "nodes/$1")
        local vstat="${vout%%[[:space:]]*}"
        if [ $vstat -eq 200 ]; then
            local vtype=$(echo "$vout" | tr ' ' '\n' | grep ":type" | head -1 | cut -d'"' -f2)
            echo "$vtype"
            if [ "$vtype" == "vos:ContainerNode" ]; then
                echo "$vout" | tr ' ' '\n' | grep "\"${ROOT}/${1}/" | cut -d'"' -f2 | sed -e "s|${ROOT}/||g"
            else
                echo "$vout" | tr ' ' '\n' | grep "\"${ROOT}/${1}" | cut -d'"' -f2 | sed -e "s|${ROOT}/||g"
            fi
            if [ "$vtype" == "vos:LinkNode" ]; then
                echo "$vout" | tr ' ' '\n' | grep "<target>" | cut -d'<' -f2 | sed -e "s|${ROOT}/||g" -e "s|>| |g"
            fi
        else
            echo $vout
        fi
        shift
    done
}

function vo_delete {
    while [ $# -ne 0 ]; do
        echo "--- DELETE $1 ---"
        local vout=$(vo_curl "nodes/$1" "DELETE")
        echo $vout
        shift
    done
}

function vo_container {
    echo "--- CREATE CONTAINER ---"
    while [ $# -ne 0 ]; do
        local vout=$(echo "$CONTAINER" | sed -e "s|URI|${ROOT}/${1}|g" | vo_curl "nodes/$1" "PUT")
        local vstat=${vout%%[[:space:]]*}
        if [ $vstat -eq 201 ]; then
            echo "$vout" | tr ' ' '\n' | grep "${ROOT}/${1}" | cut -d'"' -f2 | sed -e "s|${ROOT}/||g"
        else
            echo $vout
        fi
        shift
    done
}

function vo_data {
    echo "--- CREATE DATA ---"
    while [ $# -ne 0 ]; do
        local vout=$(echo "$DATANODE" | sed -e "s|URI|${ROOT}/${1}|g" | vo_curl "nodes/$1" "PUT")
        local vstat=${vout%%[[:space:]]*}
        if [ $vstat -eq 201 ]; then
            echo "$vout" | tr ' ' '\n' | grep "${ROOT}/${1}" | cut -d'"' -f2 | sed -e "s|${ROOT}/||g"
        else
            echo $vout
        fi
        shift
    done
}

function vo_link {
    echo "--- CREATE LINK ---"
    while [ $# -gt 1 ]; do
        local vout=$(echo "$LINKNODE" | sed -e "s|URI|${ROOT}/${2}|g" -e "s|TARGET|${ROOT}/${1}|g" | vo_curl "nodes/$2" "PUT")
        local vstat=${vout%%[[:space:]]*}
        if [ $vstat -eq 201 ]; then
            echo "$vout" | tr ' ' '\n' | grep "${ROOT}/${2}" | cut -d'"' -f2 | sed -e "s|${ROOT}/||g"
        else
            echo $vout
        fi
        shift
        shift
    done
}

datenode=$(date +"Z%Y%m%d%H%M")

vo_get "${u}"
vo_container "${u}/${datenode}" "${u}/${datenode}/Z" "${u}/${datenode}/Z/Y"
vo_data "${u}/${datenode}/DATAX" "${u}/${datenode}/Z/DATAZ" "${u}/${datenode}/Z/Y/DATAY"
vo_link "${u}/${datenode}/Z" "${u}/${datenode}/ZLINK" \
        "${u}/${datenode}/Z/DATAZ" "${u}/${datenode}/Z/DATAZLINK" \
        "${u}/${datenode}/Z/Y/DATAY" "${u}/${datenode}/Z/DATAYLINK" \
        "${u}/${datenode}/Z/DATAYLINK" "${u}/${datenode}/Z/LINKLINK" \
        "${u}/${datenode}/Z/LINKLINK" "${u}/${datenode}/Z/LINKLINKLINK"
vo_container "${u}/${datenode}" "${u}/${datenode}/DATAX"
vo_data "${u}/${datenode}" "${u}/${datenode}/DATAX"
vo_link "${u}/${datenode}/Z/DATAZ" "${u}/${datenode}/ZLINK" \
        "${u}/${datenode}/DATAX" "${u}/${datenode}/Z" \
        "${u}/${datenode}/Z/Y/NOEXIST" "${u}/${datenode}/Z/Y/DATAYLINK"
vo_data "${u}/${datenode}/Z/Y/X/DATAX" "${u}/${datenode}/ZLINK/Y/DATAW" "demo00/NOEXIST"
read -rsp $'Press any key to continue...\n' -n1 key
vo_get "${u}" "${u}/${datenode}" "${u}/${datenode}/Z" "${u}/${datenode}/Z/Y" \
        "${u}/${datenode}/Z/Y/DATAY" "${u}/${datenode}/Z/LINKLINKLINK" \
        "${u}/${datenode}/NOEXIST" "demo00/${datenode}"
read -rsp $'Press any key to continue...\n' -n1 key
vo_delete "${u}/${datenode}/ZLINK/Y/DATAY" "${u}/${datenode}/Z/DATAZ" \
        "${u}/${datenode}/Z/Y/DATAY" "${u}/${datenode}/NOEXIST"
vo_get "${u}/${datenode}/Z"
vo_delete "${u}/${datenode}"
vo_get "${u}"
vo_delete "${u}/${datenode}/NOEXIST" "demo00/NOEXIST"
