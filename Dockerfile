FROM tomcat:9.0.70-jre8

# Create a tomcat user with appropriate UID and GID. This needs to match
# the host files if mounting a VOSpace directory. We name it tomcat primarily
# for legacy reasons
# TODO: make the UID and GID and label build arg?
RUN groupadd -g 81 tomcat && \
    useradd -m -u 81 -g tomcat tomcat
 
# setup main VOS store directory
# TODO: should this also be a build arg?
RUN mkdir -p /net/dl2/vospace/

# set directory permissions for tomcat user
RUN chown -R tomcat:tomcat /usr/local/tomcat/* /net/dl2/vospace/

# storage location
VOLUME /net/dl2/vospace/users

# staging data location
VOLUME /net/dl2/vospace/tmp

# Switch to the custom user
USER tomcat

# set working directory to tomcat location
WORKDIR /usr/local/tomcat/

# copy the distributable into the container
COPY ./java/vospace-2.0.war ./webapps/

# start the tomcat server
CMD ["catalina.sh", "run"]
