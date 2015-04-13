
package edu.caltech.vao.vospace.xml;

import edu.caltech.vao.vospace.VOSpaceException;

import java.util.ArrayList;

public class Protocol  {

    private XMLObject protocol;

    /**
     * Construct an empty Protocol
     */
    public Protocol() throws VOSpaceException{
	String blank = "<vos:protocol uri=\"\" xmlns:vos=\"http://www.ivoa.net/xml/VOSpace/v2.0\"><vos:endpoint></vos:endpoint></vos:protocol>";
	protocol = new XMLObject(blank.getBytes());
    }

    /**
     * Construct a Protocol from the byte array
     * @param bytes The byte array containing the Protocol
     */
    public Protocol(byte[] bytes) throws VOSpaceException {
	protocol = new XMLObject(bytes);
    }

    /**
     * Construct a Protocol from the string representation
     * @param bytes The string containing the Protocol
     */
    public Protocol(String bytes) throws VOSpaceException {
	protocol = new XMLObject(bytes.getBytes());
    }

    /**
     * Get the endpoint of the protocol
     * @return The endpoint of the protocol
     */
    public String getEndpoint() throws VOSpaceException {
	return protocol.xpath("/vos:protocol/vos:endpoint")[0];
    }

    /**
     * Get the URI of the protocol
     * @return The URI of the protocol
     */
    public String getURI() throws VOSpaceException {
	return protocol.xpath("/vos:protocol/@uri")[0];
    }

    /**
     * Get the params of the protocol
     * @return The params of the protocol
     */
    public Param[] getParam() throws VOSpaceException {
	ArrayList<Param> params = new ArrayList<Param>();
	for (String param : protocol.xpath("/vos:protocol/vos:param")) {
	    params.add(new Param(param));
	} 
	return params.toArray(new Param[0]);
    }

    /**
     * Set the endpoint of the protocol
     * @param endpoint The endpoint of the protocol
     */
    public void setEndpoint(String endpoint) throws VOSpaceException {
        boolean hasEndpoint = protocol.has("/vos:protocol/vos:endpoint");
      if (!hasEndpoint)
            protocol.add("/vos:protocol", protocol.PREFIX == null ? "<endpoint></endpoint>" : "<" + protocol.PREFIX + ":endpoint></" + protocol.PREFIX + ":endpoint>");
        if (endpoint != null)
	    protocol.replace("/vos:protocol/vos:endpoint", endpoint);
    }

    /**
     * Set the URI of the protocol
     * @return The URI of the protocol
     */
    public void setURI(String uri) throws VOSpaceException {
	protocol.replace("/vos:protocol/@uri", uri);
    }

    /**
     * Get a string representation of the protocol
     * @return a string representation of the protocol
     */
    public String toString() {
	return protocol.toString();
    }
}