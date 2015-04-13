/**
 * HttpPutProtocolHandler.java
 * Author: Matthew Graham (Caltech)
 * Version: Original (0.1) - 31 July 2006
 */

package edu.caltech.vao.vospace.protocol;

import edu.caltech.vao.vospace.VOSpaceException;
import edu.caltech.vao.vospace.storage.StorageManager;
import edu.caltech.vao.vospace.xml.Protocol;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PutMethod;

/**
 * This class handles the implementation details for the HTTP 1.1 PUT protocol
 */
public class HttpPutProtocolHandler implements ProtocolHandler {

    private static String BASE_URL = "http://localhost:7007";

    /*
     * Return the registered identifier for this protocol 
     */
    public String getUri() {
	return "ivo://ivoa.net/vospace/core#httpput";
    }

    /*
     * Set the base url for the protocol
     */
    public void setBaseUrl(String baseurl) {
        BASE_URL = baseurl;
    }

    /*
     * Fill in the details for a ProtocolType
     */
    public Protocol admin(String nodeUri, Protocol protocol, int mode) throws VOSpaceException { 
	try {
	    if (mode == SERVER) protocol.setEndpoint(BASE_URL + "/" + UUID.randomUUID());	
	    return protocol;
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);	
	}
    } 

    /*
     * Invoke the protocol handler and transfer data
     */
    public boolean invoke(Protocol protocol, String location, StorageManager backend) throws IOException {
	boolean success = false;
	try {
	    HttpClient httpClient = new HttpClient();
	    PutMethod put = new PutMethod(protocol.getEndpoint());
	    //	    URI uri = new URI(location);
	    //      put.setRequestBody(new FileInputStream(uri.getPath()));
	    put.setRequestBody(backend.getBytes(location));
	    int responseCode = httpClient.executeMethod(put);
	    if (responseCode >= 400) {
		throw new IOException(put.getResponseBodyAsString());
	    } else {
		success = true;
	    }
	} catch (VOSpaceException e) {
	    throw new IOException(e.getMessage());
	}

	return success;
    }

}
