FROM mariadb:5.5 as mysql

COPY vospace_account.sql /docker-entrypoint-initdb.d/
COPY src/main/webapp/WEB-INF/classes/ivoa_props.properties vospace_create.sh vospace_create.sql \
    vospace.properties.docker vospace_proptable.sh vospace_rootnode.sh vospace_user_sql.sh /vobuild/

RUN /vobuild/vospace_create.sh docker > /docker-entrypoint-initdb.d/vospace_create.sql \
    && /vobuild/vospace_user_sql.sh vosuser docker > /docker-entrypoint-initdb.d/vospace_user_sql.sql \
    && sed -i '1iUSE vospace;' /docker-entrypoint-initdb.d/vospace_create.sql \
    && sed -i '1iUSE vospace;' /docker-entrypoint-initdb.d/vospace_user_sql.sql \
    && rm -rf /vobuild

FROM tomcat:7 as tomcat

RUN apt-get update \
    && apt-get install -y --no-install-recommends ant openjdk-7-jdk="$JAVA_DEBIAN_VERSION" \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

COPY . /opt/src/vospace
COPY ./vospace.properties.docker /opt/src/vospace/vospace.properties.default

RUN cd /opt/src/vospace && ant clean deploy \
    && /opt/src/vospace/vospace_user_dir.sh vosuser docker && rm -rf /opt/src/vospace
