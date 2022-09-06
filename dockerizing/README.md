# Dockerizing VOSpace development

Two docker containers are created, one for the tomcat server and the other for the mysql database.

This directory contains all the needed files, except for the Tomcat webapp and java classes mount points
which are relative to this directory and part of the vospace github project.
 
## How to build and run the containers
### Run containers and build if it is the first time.
`docker-compose --env-file ./docker_vospace.env -f docker-compose.yml up`

### Run containers and force a rebuild
`docker-compose --env-file ./docker_vospace.env -f docker-compose.yml up --build`

### Run only some services
`docker-compose --env-file ./docker_vospace.env -f docker-compose.yml start <service name>`

**E.g.**

To start only tomcat

_docker-compose --env-file ./docker_vospace.env -f docker-compose.yml start tomcat_

To start only mysql

_docker-compose --env-file ./docker_vospace.env -f docker-compose.yml start mysql_

To stop them simply replace start for stop
**E.g.**

_docker-compose --env-file ./docker_vospace.env -f docker-compose.yml stop mysql_


### Verify images and containers
#### Images
Do a:
`docker image ls`
These two images should appear:

```
> docker image ls
REPOSITORY                   TAG          IMAGE ID       CREATED          SIZE
vospace/alpine-mysql         3.7          655217501e83   11 minutes ago   200MB
tomcat/amazoncorretto        8            56af83de463c   11 minutes ago   381MB
```

#### Containers
and a:
`docker container ls -a`

These two containers should appear:

```
>docker container ls -a
CONTAINER ID   IMAGE                      COMMAND                  CREATED          STATUS                        PORTS                NAMES
5bb7525f1756   tomcat/amazoncorretto:8    "catalina.sh jpda run"   About an hour ago   Up 4 seconds               0.0.0.0:8000->8000/tcp, 0.0.0.0:8080->8080/tcp   vospace\_tomcat
5fdd882bfc9a   vospace/alpine-mysql:3.7   "/startup.sh"            About an hour ago   Up 6 seconds               0.0.0.0:3306->3306/tcp                           vospace\_mysql
```


### Build the images but delete/refresh existing cache
Do a 
`docker-compose --env-file ./docker_vospace.env build --no-cache`

## Interacting with the containers
### MYSQL container
#### Access the mysql DB via a mysql client
#### From the host machine
To run an external mysql client from the local machine onto the mysql container simply do

`mysql -h 127.0.0.1 -u <db user> -p  <db name>`

**E.g.**

_mysql -h 127.0.0.1 -u dba -p  vospace\_dev_

**_Note_** the _127.0.0.1_ as opposed to _localhost_. If you use _localhost_ the mysql client will try to 
find and use the local unix socket, which is available within the container but not the host.
So to force a network connection use the loopback ip address _127.0.0.1_

In my MacOS environment I had two options:
#### From a local mysql client

I installed a mysql client in my Mac via brew, then run it as below:

**E.g.**

_/usr/local/opt/mysql-client/bin/mysql -h 127.0.0.1 -u dba -p  vospace\_dev_

#### From the container itself
The other is to access the mysql client from the mysql container itself

`docker exec -it $(docker ps -aqf "name=^${CONTAINER_NAME}$") sh -c "mysql -u <db user> -p <db name>"`

**E.g.**

_docker exec -it $(docker ps -aqf "name=^${CONTAINER_NAME}$") sh -c "mysql -u dba -p vospace\_dev"_


### Tomcat container
#### Development in IntelliJ

A new task, **_tom_**, was added to the ant build.xml file in the java directory. The new task will compile the code and
reload the vospace service running in the tomcat server.

To run this task, you can open a terminal and run:

`ant tom`

or you can also map that that ant target to some key shortcut in IntelliJ

**E.g.**

_ctrl+command+T_

**Note** For more details see ant _tom_ task in build.xml in java directory as it forces a curl command reload of the tomcat service.

##### JPDA debugging
This uses the Java Platform Debugging Architecture (JPDA). Basically, if configured, the JVM opens a port that allows 
debugging.
In IntelliJ Edit Configurations for debugging and choose **_Remote JVM Debug_**, and make sure the port you use is the same you've configured in the container JVM

##### Reload a service
`curl --user admin:admin "http://localhost:8080/manager/text/reload?path=/<service name>"`

**E.g.**

```
>curl --user admin:admin "http://localhost:8080/manager/text/reload?path=/vospace"
OK - Reloaded application at context path [/vospace]
```


#### Testing the Tomcat server via http

`http://localhost:8080/vospace/test/Dummy`

should display:

```
Dummy Test
Dummy Test 2!!
Dummy Test 3!!
Dummy Test 4!!
```


## Useful docker commands
### Access a container via sh
`docker exec -it <container id> sh`

**e.g.**

_docker exec -it 0736d5a22ad7 sh_

### Access a container via its image name
`docker exec -it $(docker ps -a | grep "$IMAGE_NAME" | cut -d " " -f1) sh`

### Delete images.
To delete an image, all containers based on it, need to be stopped and deleted first.

**e.g.**

IMAGE\_NAME="vospace\_dev/alpine-mysql"

#### stop all containers based on a given image

`docker stop $(docker ps -a | grep "$IMAGE_NAME" | cut -d " " -f1)`

#### remove all containers based on a given image

`docker container rm $(docker ps -a | grep "$IMAGE_NAME" | cut -d " " -f1)`

#### delete image based on name

`docker rmi $(docker images "$IMAGE_NAME" -a -q)`


