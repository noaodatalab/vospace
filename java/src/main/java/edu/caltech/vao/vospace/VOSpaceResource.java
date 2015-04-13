 
package edu.caltech.vao.vospace;

import java.net.URISyntaxException;

public class VOSpaceResource {

    protected final VOSpaceManager manager;
    private final String PROPFILE = "/Users/mjg/Projects/repos/vospace/java/java/vospace.properties";

    public VOSpaceResource() throws VOSpaceException {
	// Get property file
	try {
	    //	    String propFile = this.getClass().getClassLoader().getResource("vospace.properties").toURI().getRawPath();
	    manager = VOSpaceManager.getInstance(PROPFILE);
	    //	} catch (URISyntaxException e) {
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }


    public static void main(String args[]) {
	try {
	    VOSpaceResource res = new VOSpaceResource();
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
    }
}
