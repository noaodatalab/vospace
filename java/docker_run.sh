#!/bin/bash

if [ $(docker ps | grep vospace | wc -l) -gt 0 ]; then
    docker stop vospace_tomcat
    docker stop vospace_mysql
else
    if [ $(docker network ls | grep vospace-net | wc -l) -lt 1 ]; then
        docker network create --driver bridge vospace-net
    fi
    docker run -d -e MYSQL_ROOT_PASSWORD=Tupperware --rm --name vospace_mysql --network vospace-net vospace_mysql
    docker run -d --rm -p 8080:8080 --name vospace_tomcat --network vospace-net vospace_tomcat
fi
