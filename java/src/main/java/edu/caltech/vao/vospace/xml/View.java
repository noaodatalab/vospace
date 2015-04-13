package edu.caltech.vao.vospace.xml;

import java.util.ArrayList;

import edu.caltech.vao.vospace.VOSpaceException;

public class View  {

    private XMLObject view;

    /**
     * Construct a View from the byte array
     * @param bytes The byte array containing the View
     */
    public View(byte[] bytes) throws VOSpaceException {
	view = new XMLObject(bytes);
    }

    /**
     * Construct a View from the string representation
     * @param bytes The string containing the view
     */
    public View(String bytes) throws VOSpaceException {
	view = new XMLObject(bytes.getBytes());
    }

    /**
     * Get the params of the view
     * @return The params of the view
     */
    public Param[] getParam() throws VOSpaceException {
	ArrayList<Param> params = new ArrayList<Param>();
	for (String param : view.xpath("/vos:view/vos:param")) {
	    params.add(new Param(param));
	} 
	return params.toArray(new Param[0]);
    }

    /**
     * Get the URI of the view
     * @return The URI of the view
     */
    public String getURI() throws VOSpaceException {
	return view.xpath("/vos:view/@uri")[0];
    }

    /**
     * Does the view provide access to the original data content?
     * @return whether the view provides access to the original data content
     */
    public boolean isOriginal() throws VOSpaceException {
	 String isOriginal = view.xpath("/vos:view/@original")[0];
	 return Boolean.valueOf(isOriginal).booleanValue();
    }

}