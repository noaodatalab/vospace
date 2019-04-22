#!/bin/bash

if [ $(docker ps | grep vospace | wc -l) -gt 0 ]; then
    if [ $(docker ps | grep vospace_tomcat | wc -l) -gt 0 ]; then
        echo "Stopping" $(docker stop vospace_tomcat)
    fi
    if [ $(docker ps | grep vospace_mysql | wc -l) -gt 0 ]; then
        echo "Stopping" $(docker stop vospace_mysql)
    fi
else
    if [ $(docker network ls | grep vospace-net | wc -l) -lt 1 ]; then
        docker network create --driver bridge vospace-net
    fi
    docker run -d -e MYSQL_ROOT_PASSWORD=Tupperware --rm --name vospace_mysql --network vospace-net vospace_mysql
    docker run -d --rm -p 8080:8080 --name vospace_tomcat --network vospace-net vospace_tomcat
    docker ps
fi
