
package edu.caltech.vao.vospace.xml;

import edu.caltech.vao.vospace.VOSpaceException;

public class DataNode extends Node {

    /**
     * Construct a Node from the byte array
     * @param req The byte array containing the Node
     */
    public DataNode(byte[] bytes) throws VOSpaceException {
        super(bytes);
    }

    /**
     * Validate the structure of the document
     */
    protected boolean validStructure() {
	// Check to see whether accepts, provides and capabilities defined
	return true;
    }

    /**
     * Remove the <accepts> element
     */
    public void removeAccepts() throws VOSpaceException {
	remove("/vos:node/vos:accepts");
    }

    /**
     * Remove the <provides> element
     */
    public void removeProvides() throws VOSpaceException {
	remove("/vos:node/vos:provides");
    }

    /**
     * Remove the busy attribute
     */
    public void removeBusy() throws VOSpaceException {
	remove("/vos:node/@busy");
    }


    /**
     * Add a <view> with the specified value to the <accepts> element creating the latter
     * if it does not exist.
     * @param value The value of the <view> element
     */
    public void addAccepts(String value) throws VOSpaceException {
	boolean hasAccepts = has("/vos:node/vos:accepts");
	if (!hasAccepts)
	    add("/vos:node/vos:properties", PREFIX == null ? "<accepts></accepts>" : "<" + PREFIX + ":accepts></" + PREFIX + ":accepts>");
	if (value != null)
	    addChild("/vos:node/vos:accepts", PREFIX == null ? "<view uri=\"" + value + "\"/>" : "<" + PREFIX + ":view uri=\"" + value + "\"/>");
    }

    /**
     * Add a <view> with the specified value to the <provides> element creating the latter
     * if it does not exist.
     * @param value The value of the <view> element
     */
    public void addProvides(String value) throws VOSpaceException {
	boolean hasProvides = has("/vos:node/vos:provides");
	if (!hasProvides)
	    add("/vos:node/vos:accepts", PREFIX == null ? "<provides></provides>" : "<" + PREFIX + ":provides></" + PREFIX + ":provides>");
	if (value != null)
	    addChild("/vos:node/vos:provides", PREFIX == null ? "<view uri=\"" + value + "\"/>" : "<" + PREFIX + ":view uri=\"" + value + "\"/>");
    }

    /**
     * Set the busy attribute
     * @param value The value of the busy attribute
     */
    public void setBusy(boolean value) throws VOSpaceException {
	replace("/vos:node/@busy", String.valueOf(value));
    }

}
