
package edu.caltech.vao.vospace.xml;

import java.util.ArrayList;

import edu.caltech.vao.vospace.VOSpaceException;

public class Transfer {

    private XMLObject transfer;

    /**
     * Construct a Transfer from the byte array
     * @param bytes The byte array containing the Transfer
     */
    public Transfer(byte[] bytes) throws VOSpaceException {
	transfer = new XMLObject(bytes);
    }

    public Transfer(String bytes) throws VOSpaceException {
	this(bytes.getBytes());
    }
    

    /**
     * Get the target of the transfer
     * @return The target of the transfer
     */
    public String getTarget() throws VOSpaceException {
	return transfer.xpath("/vos:transfer/vos:target")[0];
    }

    /** 
     * Set the target of the transfer
     * @param uri The new target of the transfer
     */
    public void setTarget(String target) throws VOSpaceException {
	transfer.replace("/vos:transfer/vos:target", target);
    }

    /**
     * Get the direction of the transfer
     * @return The direction of the transfer
     */
    public String getDirection() throws VOSpaceException {
	return transfer.xpath("/vos:transfer/vos:direction")[0];
    }

    /**
     * Get the view of the transfer
     * @return The view of the transfer
     */
    public View getView() throws VOSpaceException {
	String uri = transfer.xpath("/vos:transfer/vos:view/@uri")[0];
	String blank = "<vos:view uri=\"" + uri +"\" xmlns:vos=\"http://www.ivoa.net/xml/VOSpace/v2.0\"/>";
	return new View(blank);
    }

    /**
     * Get the protocols of the transfer
     * @return The protocols of the transfer
     */
    public Protocol[] getProtocol() throws VOSpaceException {
	ArrayList<Protocol> protocols = new ArrayList<Protocol>();
	for (String uri : transfer.xpath("/vos:transfer/vos:protocol/@uri")) {
	    Protocol protocol = new Protocol();
	    protocol.setURI(uri);
	    if (transfer.has("/vos:transfer/vos:protocol[@uri = '" + uri + "']/vos:endpoint")) {
		String endpoint = transfer.xpath("/vos:transfer/vos:protocol[@uri = '" + uri + "']/vos:endpoint")[0];
		protocol.setEndpoint(endpoint);
	    }
	    protocols.add(protocol);
	}
	return protocols.toArray(new Protocol[0]);
    }

    /**
     * Is keepBytes set in the transfer?
     * @return The value of the keepBytes attribute
     */
    public boolean isKeepBytes() throws VOSpaceException {
	 String isKeepBytes = transfer.xpath("/vos:transfer/vos:keepBytes")[0];
	 return Boolean.valueOf(isKeepBytes).booleanValue();
    }

    /**
     * Add the protocols of the transfer
     */
    public void addProtocol(Protocol protocol) throws VOSpaceException {
	boolean hasView = transfer.has("/vos:transfer/vos:view");
	if (!hasView) {
	    transfer.add("/vos:transfer", protocol.toString());
	} else {
	    transfer.add("/vos:transfer/vos:view", protocol.toString());
	}
    }

    /**
     * Clear the protcols of the transfer
     */
    public void clearProtocols() throws VOSpaceException {
	transfer.remove("/vos:transfer/vos:protocol");
    }


    /**
     * Get a string representation of the transfer
     * @return a string representation of the transfer
     */
    public String toString() {
	return transfer.toString();
    }
}