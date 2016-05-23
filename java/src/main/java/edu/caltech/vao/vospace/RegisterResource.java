
package edu.caltech.vao.vospace;

import edu.caltech.vao.vospace.storage.StorageManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.math.BigInteger;

import java.net.URI;

import java.security.MessageDigest;
import java.security.DigestInputStream;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import edu.caltech.vao.vospace.xml.*;

@Path("register")
public class RegisterResource extends VOSpaceResource {

    private StorageManager backend;

    public RegisterResource() throws VOSpaceException {
	super();
	backend = manager.getStorageManager();
    }

    
    /**
     * This method registers the specified node.
     * 
     * @param node The node to create (contents of HTTP PUT).
     */
    @Path("{nodeid: .*}")
    @PUT
    public Response putNode(@PathParam("nodeid") String nodeid, Node node, @QueryParam("location") String location) throws VOSpaceException {
	try {
            manager.registerNode(node, "file://" + location);
	    URI nodeUri = new URI(node.getUri());
	    return Response.created(nodeUri).build();
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }
}
