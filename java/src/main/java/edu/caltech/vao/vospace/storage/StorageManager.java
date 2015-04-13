
package edu.caltech.vao.vospace.storage;

import edu.caltech.vao.vospace.VOSpaceException;

import java.io.InputStream;
import java.util.HashMap;

/**
 * Interface for communicating with backend storage
 */
public interface StorageManager {

    /**
     * Authenticate the client to the current backend storage
     * @param endpoint The storage URL
     * @param credentials The client's security credentials
     */
    public void authenticate(String endpoint, HashMap<String, String> credentials) throws VOSpaceException;

    /**
     * Create a container at the specified location in the current backend storage
     * @param location The location of the container
     */
    public void createContainer(String location) throws VOSpaceException;

    /**
     * Move the bytes from the specified old location to the specified new location 
     * in the current backend storage
     * @param oldLocation The old location of the bytes
     * @param newLocation The new location of the bytes
     */
    public void moveBytes(String oldLocation, String newLocation) throws VOSpaceException;

    /**
     * Copy the bytes from the specified old location to the specified new location
     * in the current backend storage
     * @param oldLocation The old location of the bytes
     * @param newLocation The new location of the bytes
     */
    public void copyBytes(String oldLocation, String newLocation) throws VOSpaceException;

    /**
     * Put the bytes from the specified input stream at the specified location in 
     * the current backend storage
     * @param location The location for the bytes
     * @param stream The stream containing the bytes
     */
    public void putBytes(String location, InputStream stream) throws VOSpaceException;

    /**
     * Get the bytes from the specified location in the current backend storage
     * @param location The location of the bytes
     * @return a stream containing the requested bytes
     */
    public InputStream getBytes(String location) throws VOSpaceException;

    /**
     * Remove the bytes at the specified location in the current backend storage
     * @param location The location of the bytes
     */
    public void removeBytes(String location) throws VOSpaceException;

    /**
     * Retrieve when the bytes at the specified location in the current backend storage
     * were last modified. A response of -1 indicates that the information is not
     * available.
     * @param location The location to check
     * @return when the location was last modified
     */
    public long lastModified(String location) throws VOSpaceException;

    /**
     * Retrieve the size of the data object at the specified location.
     * @param location The location to check
     * @return how many bytes the location occupies
     */
    public long size(String location) throws VOSpaceException;
}