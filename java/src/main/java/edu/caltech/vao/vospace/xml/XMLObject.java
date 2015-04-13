
package edu.caltech.vao.vospace.xml;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import com.ximpleware.*;
import com.ximpleware.xpath.*;

import edu.caltech.vao.vospace.VOSpaceException;

public class XMLObject {

    private VTDNav vn;
    private AutoPilot ap;
    private XMLModifier xm;
    protected String PREFIX;

    /**
     * Construct a XMLObject from the byte array
     * @param req The byte array containing the Node
     */
    public XMLObject(byte[] bytes) throws VOSpaceException {
	try {
	    VTDGen vg = new VTDGen();
	    vg.setDoc(bytes);
	    vg.parse(true);
	    vn = vg.getNav();
	    ap = new AutoPilot();
	    xm = new XMLModifier();
	    ap.declareXPathNameSpace("vos", "http://www.ivoa.net/xml/VOSpace/v2.0");
	    ap.declareXPathNameSpace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	    PREFIX = getNamespacePrefix();
	    if (!validStructure())
		throw new VOSpaceException(VOSpaceException.BAD_REQUEST, "Invalid node representation");
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Return the values of the items identified by the specified XPath expression
     * @param expression The XPath expression identifying the items to retrieve
     * @return the values of the items identified by the XPath expression
     */
    public String[] xpath(String expression) throws VOSpaceException {
	try {
	    ap.bind(vn);
	    ArrayList<String> elements = new ArrayList<String>();
	    ap.selectXPath(expression);
	    int result = -1;
	    while ((result = ap.evalXPath()) != -1) {
		if (vn.getTokenType(result) == VTDNav.TOKEN_ATTR_NAME) {
		    elements.add(vn.toNormalizedString(result + 1));
		} else {
		    int t = vn.getText();
		    if (t > 0) 
			elements.add(vn.toNormalizedString(t));
		}
	    }
	    ap.resetXPath();
	    return elements.toArray(new String[0]);
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Update the value of the text identified by the XPath expression with the specified string
     * @param expression The XPath expression identifying the text to be replaced
     * @param value The new text value 
     */
    public void replace(String expression, String value) throws VOSpaceException {
	try {
	    ap.bind(vn);
	    xm.bind(vn);
	    ap.selectXPath(expression);
	    int result = -1;
	    while ((result = ap.evalXPath()) != -1) {
		if (vn.getTokenType(result) == VTDNav.TOKEN_ATTR_NAME) {
		    xm.updateToken(result + 1, value);
		} else {
		    int t = vn.getText();
		    if (t > 0)
			xm.updateToken(t, value);
		}
	    }
	    vn = xm.outputAndReparse();
	    ap.resetXPath();
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Remove the items identified by the specified XPath expression
     * @param expression The XPath expression identifying the items to remove
     */
    public void remove(String expression) throws VOSpaceException {
	try {
	    ap.bind(vn);
	    xm.bind(vn);
	    ap.selectXPath(expression);
	    int result = -1;
	    while ((result = ap.evalXPath()) != -1) {
		xm.remove();
	    }
	    vn = xm.outputAndReparse();
	    ap.resetXPath();
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Add the item identified by the specified XPath expression
     * @param expression The XPath expression identifying where to add the item
     * @param item The item to add
     */
    public void add(String expression, String item) throws VOSpaceException {
	try {
	    ap.bind(vn);
	    xm.bind(vn);
	    ap.selectXPath(expression);
	    int result = -1;
	    while ((result = ap.evalXPath()) != -1) {
		xm.insertAfterElement(item);
	    }
	    vn = xm.outputAndReparse();
	    ap.resetXPath();
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
	try {
	    ap.bind(vn);
	    xm.bind(vn);
	    ap.selectXPath(expression);
	    int result = -1;
	    while ((result = ap.evalXPath()) != -1) {
		xm.insertAfterHead(item);
	    }
	    vn = xm.outputAndReparse();
	    ap.resetXPath();
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Check whether the specified item exists
     * @param expression The XPath expression identifying the item to check
     * @return whether the specified item exists or not
     */
    public boolean has(String expression) throws VOSpaceException {
	try {
	    boolean has = false;
	    ap.bind(vn);
	    ap.selectXPath(expression);
	    if (ap.evalXPath() != -1)
		has = true;
	    ap.resetXPath();
	    return has;
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Validate the structure of the document
     */
    public boolean validStructure() {
	return true;
    }

    /**
     * Get a byte array corresponding to the object
     * @return a byte array corresponding to the object
     */
    public byte[] getBytes() {
	return vn.getXML().getBytes();
    }

    /**
     * Get the namespace prefix used for the object
     * @return the namespace prefix used for the object
     */
    public String getNamespacePrefix() throws VOSpaceException {
	try {
	    return vn.getPrefixString(1);
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }
    
    /**
     * Get a string representation of the object
     * @return a string representation of the object
     */
    public String toString() {
	return new String(getBytes());
    }
}