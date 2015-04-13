
package edu.caltech.vao.vospace.xml;

import java.io.InputStream;
import java.io.IOException;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;

import edu.caltech.vao.vospace.VOSpaceException;

/** 
 * A factory for creating nodes
 */
public class NodeFactory {

    private static NodeFactory ref;
    private static final Set<String> SUPPORTED_TYPES = new HashSet<String>(Arrays.asList(new String[] {"Node","DataNode","LinkNode","ContainerNode", "UnstructuredDataNode", "StructuredDataNode"}));

    private NodeFactory() {}

    /*
     * Get a NodeFactory
     */
    public static NodeFactory getInstance() {
	if (ref == null) ref = new NodeFactory();
	return ref;
    }

    /*
     * Get a node
     */
    public Node getNode(InputStream in, int len) throws VOSpaceException{
	byte[] bytes = new byte[len];
	try {
	    in.read(bytes, 0, len);
	} catch (IOException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
	return getNode(bytes);
    }


    /*
     * Get a node - chunked data
     */
    public Node getNode(InputStream in) throws VOSpaceException{
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count;
        try {
            while ((count = in.read(buffer)) > 0) {
                baos.write(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return getNode(baos.toByteArray());
    }


    /*
     * Get a node
     */
    public Node getNode(HttpServletRequest req) throws VOSpaceException{
	byte[] bytes = null;
	try {
	    InputStream in = req.getInputStream();
	    int len = req.getContentLength();
	    bytes = new byte[len];
	    in.read(bytes, 0, len);
	} catch (IOException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
	return getNode(bytes);
    }

    /*
     * Get a node
     */
    public Node getNode(String req) throws VOSpaceException {
	byte[] bytes = req.getBytes();
	return getNode(bytes);
    }

    private Node getNode(byte[] bytes) throws VOSpaceException {
	String type = getType(bytes);
	if (!SUPPORTED_TYPES.contains(type)) throw new VOSpaceException(VOSpaceException.BAD_REQUEST, "Node type not supported");
	Node node = null;
	try {
	    node = (Node) Class.forName("edu.caltech.vao.vospace.xml." + type).getConstructor(byte[].class).newInstance(bytes);
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
      	}
	return node;
    }

    private String getType(byte[] bytes) {
	String doc = new String(bytes).replace("'", "\"");
	int start = doc.indexOf("\"", doc.indexOf("xsi:type"));
	int end = doc.indexOf("\"", start + 1);
	String type = doc.substring(start + 1, end);
	return type.substring(type.indexOf(":") + 1);
    }

    /**
     * Get a node of the default type for the service
     * @return a Node of the default type
     */
    public Node getDefaultNode() throws VOSpaceException {
	String datanode = "<node xmlns=\"http://www.ivoa.net/xml/VOSpace/v2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"vos:DataNode\" uri=\"\" busy=\"false\"><properties></properties><accepts></accepts><provides></provides><capabilities></capabilities></node>";
	return getNode(datanode);
    }

}
