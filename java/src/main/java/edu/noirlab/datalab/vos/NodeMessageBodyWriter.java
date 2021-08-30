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

    /*
    The getSize method gets called first, in other to put the correct content length
    in the HTTP header. In doing so, we call the "toString()" method, which will trigger
    JDOM2 to instantiate and create an expensive XML DOM.
    So here, the below "output" private attribute is just as holder for the XML built
    so the length is known. Then later, when the "writeTo" method is called we just pass the
    XML string stored in the output content.
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
