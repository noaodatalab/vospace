/**
c* ProtocolHandler.java
 * Author: Matthew Graham (Caltech)
 * Version: Original (0.1) - 31 July 2006
 */

package edu.caltech.vao.vospace.protocol;

import edu.caltech.vao.vospace.VOSpaceException;
import edu.caltech.vao.vospace.storage.StorageManager;
import edu.caltech.vao.vospace.xml.Protocol;

import java.io.IOException;

/**
 * This interface represents the implementation details of a protocol
 * involved in a data transfer
 */
public interface ProtocolHandler {

    public static final int CLIENT = 0;
    public static final int SERVER = 1;

    /*
     * Return the registered identifier for this protocol 
     */
    public String getUri();

    /*
     * Set the base url for the protocol
     */
    public void setBaseUrl(String baseurl);

    /*
     * Fill in the details for a Protocol
     */
    public Protocol admin(String nodeUri, Protocol protocol, int mode) throws VOSpaceException;

    /*
     * Invoke the protocol handler and transfer data 
     */ 
    public boolean invoke(Protocol protocol, String location, StorageManager backend) throws IOException; 
}
