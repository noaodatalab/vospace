
package edu.caltech.vao.vospace;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader; 
import javax.ws.rs.ext.Provider;

import edu.caltech.vao.vospace.xml.Node;
import edu.caltech.vao.vospace.xml.NodeFactory;

/**
  * Message body reader for Nodes supplied to the service
  */
@Provider
public class NodeMessageBodyReader implements MessageBodyReader<Node> {

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
	boolean readable =  mediaType.toString().equals(MediaType.APPLICATION_XML) || mediaType.toString().equals(MediaType.TEXT_XML);
	return readable;   
    }

    public Node readFrom(Class<Node> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
	Node node = null;
	try {
	    NodeFactory factory = NodeFactory.getInstance();
	    if (httpHeaders.containsKey("Transfer-Encoding")) {
		node = factory.getNode(entityStream);
	    } else {
		int len = Integer.parseInt(httpHeaders.getFirst("content-length"));
		node = factory.getNode(entityStream, len);
	    }
	} catch (VOSpaceException e) {
	    // e.printStackTrace(System.err);
	    throw new IOException(e.getMessage());
	}
	return node;
    }
}
