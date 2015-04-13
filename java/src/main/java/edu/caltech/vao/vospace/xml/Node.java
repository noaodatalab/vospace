
package edu.caltech.vao.vospace.xml;

import java.util.HashMap;

import edu.caltech.vao.vospace.VOSpaceException;

public class Node {

    private XMLObject node;
    protected String PREFIX;

    /**
     * Construct a Node from the byte array
     * @param req The byte array containing the Node
     */
    public Node(byte[] bytes) throws VOSpaceException {
	// Remove any XML processing instructions
	String rep = new String(bytes);
	rep.replace("<?xml version='1.0' encoding='UTF-8'?>", "");
	node = new XMLObject(rep.getBytes());
        PREFIX = node.PREFIX;
    }

    /**
     * Get the uri of the node
     * @return The uri of the node
     */
    public String getUri() throws VOSpaceException {
	return node.xpath("/vos:node/@uri")[0];
    }

    /**
     * Get the type of the node
     * @return The type of the node
     */
    public String getType() throws VOSpaceException {
	return node.xpath("/vos:node/@xsi:type")[0];
    }

    /**
     * Set the uri of the node
     * @param uri The new uri of the node
     */
    public void setUri(String uri) throws VOSpaceException {
	node.replace("/vos:node/@uri", uri);
    }

    
    /**
     * Check whether the node has any properties set
     * @return whether the node has any properties set
     */
    public boolean hasProperties() throws VOSpaceException {
	try {
	    return node.has("/vos:node/vos:properties/vos:property");
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Get the properties set on the node
     * @return any properties the node has set on it
     */
    public HashMap<String, String> getProperties() throws VOSpaceException {
	try {
	    HashMap<String, String> properties = new HashMap<String, String>();
	    String[] propUris = node.xpath("/vos:node/vos:properties/vos:property/@uri");
	    for (String uri: propUris) {
		String[] values = node.xpath("/vos:node/vos:properties/vos:property[@uri = '" + uri + "']");
		String[] nilSet = node.xpath("/vos:node/vos:properties/vos:property[@uri = '" + uri +"']/@xsi:nil");
		// Add property to list if not nilled
		if (nilSet.length == 0) {
		    if (values.length > 0) {
			properties.put(uri, values[0]);
		    } else {
			properties.put(uri, "");
		    }
		}
	    }
	    return properties;
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Add the specified property to the node
     */
    public void setProperty(String property, String value) throws VOSpaceException {
	String xpath = "/vos:node/vos:properties/vos:property[@uri = '" + property + "']";
	String newProp = node.PREFIX == null ? "<property uri=\"" + property + "\">" + value + "</property>" : "<" + node.PREFIX + ":property uri = \"" + property + "\">" + value + "</" + node.PREFIX + ":property>";
        try {
	    if (node.has(xpath)) {
		node.replace(xpath, value);
	    } else {
		if (!node.has("/vos:node/vos:properties/vos:property")) {
		    // Get around the <properties/> child insertion issue
		    node.remove("/vos:node/vos:properties");
		    String props = node.PREFIX == null ? "<properties></properties>" : "<" + node.PREFIX + ":properties></" + node.PREFIX + ":properties>";
		    node.addChild("/vos:node", props);
		}
		node.addChild("/vos:node/vos:properties", newProp);
	    }
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Remove the items identified by the specified XPath expression
     * @param expression The XPath expression identifying the items to remove
     */
    public void remove(String expression) throws VOSpaceException {
	node.remove(expression);
    }

    /**
     * Remove the <properties> element
     */
    public void removeProperties() throws VOSpaceException {
	node.remove("/vos:node/vos:properties");
    }

    /**
     * Remove the <capabilities> element
     */
    public void removeCapabilities() throws VOSpaceException {
	node.remove("/vos:node/vos:capabilities");
    }

    /**
     * Add a <capability> with the specified value to the <capabilities> element creating the latter
     * if it does not exist.
     * @param value The value of the <capability> element
     */
    public void addCapabilities(String value) throws VOSpaceException {
	boolean hasCapabilities = node.has("/vos:node/vos:capabilities");
	if (!hasCapabilities)
	    node.add("/vos:node/vos:provides", node.PREFIX == null ? "<capabilities></capabilities>" : "<" + node.PREFIX + ":capabilities></" + node.PREFIX + ":capabilities>");
	if (value != null)
	    node.addChild("/vos:node/vos:capabilities", node.PREFIX == null ? "<capability uri=\"" + value + "\"/>" : "<" + node.PREFIX + ":capability uri=\"" + value + "\"/>");
    }

    
    /**
     * Get the capabilities set on the node
     * @return any capabilities the node has set on it
     */
    public String[] getCapabilities() throws VOSpaceException {
	try {
	    String[] capUris = node.xpath("/vos:node/vos:capabilities/vos:capability/@uri");
	    return capUris;
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }

    
    /**
     * Add the item identified by the specified XPath expression
     * @param expression The XPath expression identifying where to add the item
     * @param item The item to add
     */
    public void addChild(String expression, String item) throws VOSpaceException {
	node.addChild(expression, item);
    }

    /**
     * Add the item identified by the specified XPath expression
     * @param expression The XPath expression identifying where to add the item
     * @param item The item to add
     */
    public void add(String expression, String item) throws VOSpaceException {
	node.add(expression, item);
    }

    /**
     * Check whether the specified item exists
     * @param expression The XPath expression identifying the item to check
     * @return whether the specified item exists or not
     */
    public boolean has(String expression) throws VOSpaceException {
	boolean has = node.has(expression);
	return has;
    }

    /**
     * Return the value of the specified XPath expression
     * @param expression The XPath expression identifying the item(s) to retrieve
     * @return the specified item(s)
     */
    public String[] get(String expression) throws VOSpaceException {
	String[] items = node.xpath(expression);
	return items;
    }

    /**
     * Update the value of the text identified by the XPath expression with the specified string
     * @param expression The XPath expression identifying the text to be replaced
     * @param value The new text value 
     */
    public void replace(String expression, String value) throws VOSpaceException {
	node.replace(expression, value);
    }

    /**
     * Get a string representation of the node
     * @return a string representation of the node
     */
    public String toString() {
	return node.toString();
    }
}
