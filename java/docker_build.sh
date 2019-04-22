#!/bin/bash

docker build -t vospace_mysql -f Dockerfile.mysql . && \
docker build -t vospace_tomcat -f Dockerfile.tomcat .
