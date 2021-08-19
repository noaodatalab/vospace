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

/**
 * Message body writer for Nodes returned by the service
 */
@Provider
public class NodeMessageBodyWriter implements MessageBodyWriter<edu.noirlab.datalab.vos.Node> {

    /*
    The getSize method gets called first, so it can add the correct content length
    to the HTTP header. In doing so, we are calling the "toString()" method, which
    triggers the JDOM2 to instantiate and serialize the object to XML. Ideally we want
    that to happen only once.
    The below "output" private attribute is just as holder for the serialized XML.
    Then later, when the "writeTo" method is called we just pass this serialized
    XML string.
     */
    String output = null;
    public long getSize(edu.noirlab.datalab.vos.Node n, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediatype) {

        output = n.toString();
        return output.length();
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return edu.noirlab.datalab.vos.Node.class.isAssignableFrom(type);
    }

    public void writeTo(edu.noirlab.datalab.vos.Node n, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(output.getBytes());
        // Shouldn't make a difference to the GC, but by setting the output to null, the GC
        // should be able to claim that memory even if this class instantiation lingers for longer.
        output=null;
    }
}
