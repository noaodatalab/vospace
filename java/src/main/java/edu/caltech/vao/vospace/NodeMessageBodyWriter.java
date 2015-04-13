
package edu.caltech.vao.vospace;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter; 
import javax.ws.rs.ext.Provider;

import edu.caltech.vao.vospace.xml.Node;

/**
  * Message body writer for Nodes returned by the service
  */
@Provider
public class NodeMessageBodyWriter implements MessageBodyWriter<Node> {

    public long getSize(Node n, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediatype) {
	return n.toString().length();
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
	return Node.class.isAssignableFrom(type);
    }

    public void writeTo(Node n, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
	entityStream.write(n.toString().getBytes());
    }
}