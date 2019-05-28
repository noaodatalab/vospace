# MariaDB (MySQL); initializes the database for a single "vosuser" user.
FROM mariadb:5.5 as mysql

COPY dbcreate/account.sql /docker-entrypoint-initdb.d/
COPY src/main/webapp/WEB-INF/classes/ivoa_props.properties config/properties.docker dbcreate/* /vobuild/
ARG USER
RUN /vobuild/create.sh docker > /docker-entrypoint-initdb.d/create.sql \
    && /vobuild/user_sql.sh $USER docker > /docker-entrypoint-initdb.d/user_sql.sql \
    && sed -i '1iUSE vospace;' /docker-entrypoint-initdb.d/create.sql \
    && sed -i '1iUSE vospace;' /docker-entrypoint-initdb.d/user_sql.sql \
    && rm -rf /vobuild

# OpenJDK: Build the VOSpace .war file
FROM openjdk:8-stretch as openjdk

RUN apt-get update \
    && apt-get install -y --no-install-recommends ant openjdk-8-jdk="$JAVA_DEBIAN_VERSION" \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

COPY . /opt/src/vospace
COPY config/properties.docker /opt/src/vospace/config/properties.default

RUN cd /opt/src/vospace && ant clean dist

# Tomcat: Install the VOSpace .war file and create a user directory for "vosuser".
FROM tomcat:8 as tomcat

COPY --from=openjdk /opt/src/vospace/vospace-2.0.war /usr/local/tomcat/webapps/
COPY dbcreate/user_dir.sh config/properties.docker /opt/bin/

ARG USER
RUN /opt/bin/user_dir.sh $USER docker
