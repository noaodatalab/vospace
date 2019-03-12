
package edu.caltech.vao.vospace;

import java.net.URISyntaxException;

public class VOSpaceResource {

    protected final VOSpaceManager manager;

    public VOSpaceResource() throws VOSpaceException {
	// Get property file
	try {
	    String propFile = Thread.currentThread().getContextClassLoader().getResource("vospace.properties").toURI().getRawPath();
	    manager = VOSpaceManager.getInstance(propFile);
    } catch (VOSpaceException ve) {
        throw ve;
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
