#!/bin/bash

if [ ! -e $HOME/.datalab/id_token.${USER} ]; then
    echo "No token file $HOME/.datalab/id_token.${USER}."
    exit
fi
token="X-Dl-Authtoken: $(cat $HOME/.datalab/id_token.${USER})"
ROOT="vos://datalab.noao!vospace"
BASE="http://dldev:8080/vospace-2.0/vospace"

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
  <ns0:properties />
  <ns0:accepts />
  <ns0:provides />
  <ns0:capabilities />
</ns0:node>
EOF

read -r -d '' CONTAINER <<'EOF'
<ns0:node xmlns:ns0="http://www.ivoa.net/xml/VOSpace/v2.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" uri="URI" xsi:type="vos:ContainerNode">
  <ns0:properties />
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
        local vout=$(echo "$LINKNODE" | sed -e "s|URI|${ROOT}/${1}|g" -e "s|TARGET|${ROOT}/${2}|g" | vo_curl "nodes/$1" "PUT")
        local vstat=${vout%%[[:space:]]*}
        if [ $vstat -eq 201 ]; then
            echo "$vout" | tr ' ' '\n' | grep "${ROOT}/${1}" | cut -d'"' -f2 | sed -e "s|${ROOT}/||g"
        else
            echo $vout
        fi
        shift
        shift
    done
}

datenode=$(date +"Z%Y%m%d%H%M")

vo_get "${USER}"
vo_container "${USER}/${datenode}" "${USER}/${datenode}/Z" "${USER}/${datenode}/Z/Y"
vo_data "${USER}/${datenode}/DATAX" "${USER}/${datenode}/Z/DATAZ" "${USER}/${datenode}/Z/Y/DATAY"
vo_link "${USER}/${datenode}/ZLINK" "${USER}/${datenode}/Z" \
        "${USER}/${datenode}/Z/DATAZLINK" "${USER}/${datenode}/Z/DATAZ" \
        "${USER}/${datenode}/Z/DATAYLINK" "${USER}/${datenode}/Z/Y/DATAY" \
        "${USER}/${datenode}/Z/LINKLINK" "${USER}/${datenode}/Z/DATAYLINK" \
        "${USER}/${datenode}/Z/LINKLINKLINK" "${USER}/${datenode}/Z/LINKLINK"
vo_container "${USER}/${datenode}" "${USER}/${datenode}/DATAX"
vo_data "${USER}/${datenode}" "${USER}/${datenode}/DATAX"
vo_link "${USER}/${datenode}/ZLINK" "${USER}/${datenode}/Z/DATAZ" \
        "${USER}/${datenode}/Z" "${USER}/${datenode}/DATAX" \
        "${USER}/${datenode}/Z/Y/DATAYLINK" "${USER}/${datenode}/Z/Y/NOEXIST"
vo_data "${USER}/${datenode}/Z/Y/X/DATAX" "${USER}/${datenode}/ZLINK/Y/DATAW" "demo00/NOEXIST"
read -rsp $'Press any key to continue...\n' -n1 key
vo_get "${USER}" "${USER}/${datenode}" "${USER}/${datenode}/Z" "${USER}/${datenode}/Z/Y" \
        "${USER}/${datenode}/Z/Y/DATAY" "${USER}/${datenode}/Z/LINKLINKLINK" \
        "${USER}/${datenode}/NOEXIST" "demo00/${datenode}"
read -rsp $'Press any key to continue...\n' -n1 key
vo_delete "${USER}/${datenode}/ZLINK/Y/DATAY" "${USER}/${datenode}/NOEXIST" \
        "${USER}/${datenode}/Z/DATAZ" "${USER}/${datenode}/Z/Y/DATAY"
vo_get "${USER}/${datenode}/Z"
vo_delete "${USER}/${datenode}"
vo_get "${USER}"
vo_delete "${USER}/${datenode}/NOEXIST" "demo00/NOEXIST"
