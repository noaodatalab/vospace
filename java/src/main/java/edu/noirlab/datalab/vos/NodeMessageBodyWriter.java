package edu.noirlab.datalab.vos;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

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
public class NodeMessageBodyWriter implements MessageBodyWriter<edu.noirlab.datalab.vos.Node> {

    String output = null;
    public long getSize(edu.noirlab.datalab.vos.Node n, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediatype) {
        System.out.println("GetSize ...");
        output = n.toString();
        return output.length();
        //return n.toString().length();
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return edu.noirlab.datalab.vos.Node.class.isAssignableFrom(type);
    }

    public void writeTo(edu.noirlab.datalab.vos.Node n, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        System.out.println("write to...");
        //entityStream.write(n.toString().getBytes());
        entityStream.write(output.getBytes());
        System.out.println("freeing the output reference ...");
        output=null;
    }
}
