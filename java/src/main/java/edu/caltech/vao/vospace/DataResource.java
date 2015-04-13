
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

@Path("data")
public class DataResource extends VOSpaceResource {

    private final String ROOTNODE = "vos://nvo.caltech!vospace";
    private StorageManager backend;

    public DataResource() throws VOSpaceException {
	super();
	backend = manager.getStorageManager();
    }

    /**
     * This method retrieves the specified data.
     * 
     * @param fileid The identifier for the data to return.
     * @return the specified data
     */
    @Path("{fileid}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getData(@PathParam("fileid") String fileid) throws VOSpaceException {
	try {
	    String location = manager.resolveLocation(fileid);
	    manager.invalidateLocation(fileid);
	    //	    System.err.println(fileid + " " + location);
	    //	    return new File(new URI(location));
	    InputStream in = backend.getBytes(location);
	    ResponseBuilder responseBuilder = Response.ok(in, MediaType.APPLICATION_OCTET_STREAM);
	    Response response = responseBuilder.header("Content-Length", Long.toString(backend.size(location))).build();
//	    String hashText = getMD5(location);
//	    Response response = responseBuilder.header("Content-MD5", hashText).build();
	    return response;
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }


    public String getMD5(String location) throws VOSpaceException {
	try {
   	    InputStream in = new FileInputStream(new File(location));
	    MessageDigest md = MessageDigest.getInstance("MD5");
	    DigestInputStream dis = new DigestInputStream(in, md);
	    byte[] buf = new byte[4096];
	    while (dis.read() != -1);
	    byte[] digest = md.digest();
	    BigInteger bi = new BigInteger(1, digest);
	    String hashText = bi.toString(16);
	    return hashText;
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }


    /**
     * This method deals with the uploaded data.
     * 
     * @param fileid The endpoint for the uploaded data (contents of HTTP PUT).
     */
    @Path("{fileid}")
    @PUT
//    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void putNode(@PathParam("fileid") String fileid, File file) throws VOSpaceException {     
	FileInputStream in = null;
        FileOutputStream out = null;
	try {
	    String location = manager.resolveLocation(fileid);
            in = new FileInputStream(file);
	    backend.putBytes(location, in);
	    manager.updateSize(fileid, Long.toString(backend.size(location)));
	    /*
	    out = new FileOutputStream(new File(new URI(location)));
	    byte[] buffer = new byte[4096]; // To hold file contents
	    int bytes_read; 
	    while ((bytes_read = in.read(buffer)) != -1)
		// Read until EOF
		out.write(buffer, 0, bytes_read); // write
	    */
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	} finally {
	    /*
	    if (in != null)
		try {
		    in.close();
		} catch (IOException e) {
		    ;
		}
	    if (out != null)
		try {
		    out.close();
		} catch (IOException e) {
		    ;
		}
	    */
	    manager.invalidateLocation(fileid);
	}
       
    }
}
