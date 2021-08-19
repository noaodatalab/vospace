package edu.noirlab.datalab.vos;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import ca.nrc.cadc.vos.VOSException;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeReader;

/**
 * Message body reader for Nodes supplied to the service
 * This code basically does the same as the
 * "edu.caltech.vao.vospace.NodeMessageBodyReader"
 * but using the ca.nrc.cadc.vos code.
 * We have no usage at this point for this,since parts of
 * the code are still reliant on the VTD XML.
 * For instance the PUT API (putNode method) in NodeResource,
 * gets a Node object in its signature. That object is created
 * by the NodeMessageBodyReader. In this case we are leaving the
 * original VTD code to deal with that.
 * But if at some point we want to switch this could be used.
 * For that we would have to replace in the "application"
 * file in WEB-INF the caltech NodeMessageBodyReader
 * edu.caltech.vao.vospace.NodeMessageBodyReader
 * for our version
 * edu.noirlab.datalab.vos.NodeMessageBodyReader
 *
 */
@Provider
public class NodeMessageBodyReader implements MessageBodyReader<Node> {

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                              MediaType mediaType) {
        boolean readable =  mediaType.toString().equals(MediaType.APPLICATION_XML) ||
                mediaType.toString().equals(MediaType.TEXT_XML);
        return readable;
    }

    public Node readFrom(Class<Node> type, Type genericType, Annotation[] annotations,
                         MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                         InputStream entityStream) throws IOException {
        Node node = null;
        try {
            NodeReader nodeReader = new NodeReader();
            node = nodeReader.read(entityStream);
        } catch (VOSException e) {
            // e.printStackTrace(System.err);
            throw new IOException(e.getMessage());
        }
        return node;
    }
}
