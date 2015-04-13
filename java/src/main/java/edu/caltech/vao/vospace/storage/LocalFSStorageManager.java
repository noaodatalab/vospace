
package edu.caltech.vao.vospace.storage;

import edu.caltech.vao.vospace.VOSpaceException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

/**
 * Backend storage manager for local filesystem
 */
public class LocalFSStorageManager implements StorageManager {

    /**
     * Construct a basic LocalFSStorageManager
     */
    public LocalFSStorageManager(Properties props) {}


    /**
     * Authenticate the client to the current backend storage
     * @param endpoint The storage URL
     * @param credentials The client's security credentials
     */
    public void authenticate(String endpoint, HashMap<String, String> credentials) throws VOSpaceException {
	// Nothing needed here
    }

    /**
     * Create a container at the specified location in the current backend storage
     * @param location The location of the container
     */
    public void createContainer(String location) throws VOSpaceException {
	try {
	    System.err.println(location);
	    boolean success = (new File(new URI(location))).mkdir();
	    if (!success) throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, "Container cannot be created");
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Move the bytes from the specified old location to the specified new location 
     * in the current backend storage
     * @param oldLocation The old location of the bytes
     * @param newLocation The new location of the bytes
     */
    public void moveBytes(String oldLocation, String newLocation) throws VOSpaceException {
	try {
	    File oldFile = new File(new URI(oldLocation));
	    if (oldFile.isFile()) {
		FileUtils.moveFile(oldFile, new File(new URI(newLocation)));
	    } else {
		FileUtils.moveDirectory(oldFile, new File(new URI(newLocation)));
	    }
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Copy the bytes from the specified old location to the specified new location
     * in the current backend storage
     * @param oldLocation The old location of the bytes
     * @param newLocation The new location of the bytes
     */
    public void copyBytes(String oldLocation, String newLocation) throws VOSpaceException {
	try {
	    System.err.println(oldLocation + " " + newLocation);
	    File oldFile = new File(new URI(oldLocation));
	    if (oldFile.isFile()) {
		FileUtils.copyFile(oldFile, new File(new URI(newLocation)));
	    } else {
		FileUtils.copyDirectory(oldFile, new File(new URI(newLocation)));
	    }
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Put the bytes from the specified input stream at the specified location in 
     * the current backend storage
     * @param location The location for the bytes
     * @param stream The stream containing the bytes
     */
    public void putBytes(String location, InputStream stream) throws VOSpaceException {
	try {
	    URI uri = new URI(location);
	    BufferedInputStream bis = new BufferedInputStream(stream);
	    FileOutputStream fos = new FileOutputStream(uri.getPath()); 
            byte[] buffer = new byte[8192];
            int count = bis.read(buffer);
            while (count != -1 && count <= 8192) {
                fos.write(buffer, 0, count);
                count = bis.read(buffer);
            }
            if (count != -1) {
                fos.write(buffer, 0, count);
            }
	    fos.close();
	    bis.close();
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Get the bytes from the specified location in the current backend storage
     * @param location The location of the bytes
     * @return a stream containing the requested bytes
     */
    public InputStream getBytes(String location) throws VOSpaceException {
	try {
	    URI uri = new URI(location);
	    InputStream in = new FileInputStream(uri.getPath());
	    return in;
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Remove the bytes at the specified location in the current backend storage
     * @param location The location of the bytes
     */
    public void removeBytes(String location) throws VOSpaceException {
	try {
	    boolean success = new File(new URI(location)).delete();
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Retrieve when the bytes at the specified location in the current backend storage
     * were last modified. A response of -1 indicates that the information is not
     * available.
     * @param location The location to check
     * @return when the location was last modified
     */
    public long lastModified(String location) throws VOSpaceException {
	try {
	    long lastModified = new File(new URI(location)).lastModified();
	    return lastModified;
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Retrieve the size of the data object at the specified location.
     * @param location The location to check
     * @return how many bytes the location occupies
     */
    public long size(String location) throws VOSpaceException {
	try {
	    long size = new File(new URI(location)).length();
	    return size;
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }
}