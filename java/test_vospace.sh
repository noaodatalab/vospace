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

function vo_list {
    local vout=$(vo_curl "nodes/$1")
    local vstat="${vout%%[[:space:]]*}"
    if [ $vstat -eq 200 ]; then
        echo "$vout" | tr ' ' '\n' | grep "\"${ROOT}/${1}/" | cut -d'"' -f2 | sed -e "s|${ROOT}/||g"
    else
        echo $vout
    fi
}

function vo_delete {
    local vout=$(vo_curl "nodes/$1" "DELETE")
    echo $vout
}

function vo_container {
    local vout=$(echo "$CONTAINER" | sed -e "s|URI|${ROOT}/${1}|g" | vo_curl "nodes/$1" "PUT")
    local vstat=${vout%%[[:space:]]*}
    if [ $vstat -eq 201 ]; then
        echo "$vout" | tr ' ' '\n' | grep "${ROOT}/${1}" | cut -d'"' -f2 | sed -e "s|${ROOT}/||g"
    else
        echo $vout
    fi
}

function vo_data {
    local vout=$(echo "$DATANODE" | sed -e "s|URI|${ROOT}/${1}|g" | vo_curl "nodes/$1" "PUT")
    local vstat=${vout%%[[:space:]]*}
    if [ $vstat -eq 201 ]; then
        echo "$vout" | tr ' ' '\n' | grep "${ROOT}/${1}" | cut -d'"' -f2 | sed -e "s|${ROOT}/||g"
    else
        echo $vout
    fi
}

function vo_link {
    local vout=$(echo "$LINKNODE" | sed -e "s|URI|${ROOT}/${1}|g" -e "s|TARGET|${ROOT}/${2}|g" | vo_curl "nodes/$1" "PUT")
    local vstat=${vout%%[[:space:]]*}
    if [ $vstat -eq 201 ]; then
        echo "$vout" | tr ' ' '\n' | grep "${ROOT}/${1}" | cut -d'"' -f2 | sed -e "s|${ROOT}/||g"
    else
        echo $vout
    fi
}

datenode=$(date +"Z%Y%m%d%H%M")

echo "--- CREATE CONTAINER ---"
vo_container "${USER}/${datenode}"
vo_container "${USER}/${datenode}/Z"
vo_container "${USER}/${datenode}/Z/Y"
echo "--- CREATE DATA ---"
vo_data "${USER}/${datenode}/DATAX"
vo_data "${USER}/${datenode}/Z/DATAZ"
vo_data "${USER}/${datenode}/Z/Y/DATAY"
echo "--- CREATE CONTAINER LINK ---"
vo_link "${USER}/${datenode}/ZLINK" "${USER}/${datenode}/Z"
echo "--- CREATE DATA LINK ---"
vo_link "${USER}/${datenode}/Z/DATAZLINK" "${USER}/${datenode}/Z/DATAZ"
vo_link "${USER}/${datenode}/Z/DATAYLINK" "${USER}/${datenode}/Z/Y/DATAY"
echo "--- CREATE ALREADY EXIST (409) ---"
vo_container "${USER}/${datenode}"
vo_container "${USER}/${datenode}/DATAX"
vo_data "${USER}/${datenode}"
vo_data "${USER}/${datenode}/DATAX"
vo_link "${USER}/${datenode}/ZLINK" "${USER}/${datenode}/Z/DATAZ"
vo_link "${USER}/${datenode}/Z" "${USER}/${datenode}/DATAX"
echo "--- CREATE NO PATH (404) ---"
vo_data "${USER}/${datenode}/Z/Y/X/DATAX"
vo_link "${USER}/${datenode}/Z/Y/DATAYLINK" "${USER}/${datenode}/Z/Y/NOEXIST"
echo "--- CREATE LINK FOUND (400) ---"
vo_data "${USER}/${datenode}/ZLINK/Y/DATAW"
echo "--- CREATE NOPERMISSION (403) ---"
vo_data "demo00/NOEXIST"
echo "--- LIST ---"
vo_list "${USER}"
echo "--- LIST ${datenode} ---"
vo_list "${USER}/${datenode}"
echo "--- LIST ${datenode}/Z ---"
vo_list "${USER}/${datenode}/Z"
echo "--- LIST ${datenode}/Z/Y ---"
vo_list "${USER}/${datenode}/Z/Y"
echo "--- LIST NOEXIST (404) ---"
vo_list "${USER}/${datenode}/NOEXIST"
echo "--- LIST NOPERMISSION (403) ---"
vo_list "demo00/${datenode}"
read -rsp $'Press any key to continue...\n' -n1 key
echo "--- DELETE NOEXIST ---"
vo_delete "${USER}/${datenode}/NOEXIST"
echo "--- DELETE ---"
vo_delete "${USER}/${datenode}/Z/DATAZ"
vo_delete "${USER}/${datenode}/Z/Y/DATAY"
vo_delete "${USER}/${datenode}/Z"
vo_delete "${USER}/${datenode}/DATAX"
vo_delete "${USER}/${datenode}"
echo "--- DELETE CONTAINER NOEXIST (404) ---"
vo_delete "${USER}/${datenode}/NOEXIST"
echo "--- DELETE NOPERMISSION (403) ---"
vo_delete "demo00/NOEXIST"
