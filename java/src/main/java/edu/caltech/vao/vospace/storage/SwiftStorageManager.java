
package edu.caltech.vao.vospace.storage;


import com.rackspacecloud.client.cloudfiles.FilesAuthorizationException;
import com.rackspacecloud.client.cloudfiles.FilesClient;
import com.rackspacecloud.client.cloudfiles.FilesException;
import com.rackspacecloud.client.cloudfiles.FilesInvalidNameException;
import com.rackspacecloud.client.cloudfiles.FilesNotFoundException;

import edu.caltech.vao.vospace.VOSpaceException;

import edu.jhu.pha.vospace.api.PathSeparator;
import edu.jhu.pha.vospace.api.PathSeparator.NodePath;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.http.HttpException;

/**
 * Backend storage manager for OpenStack Swift system
 * @author Matthew Graham
 * @author Dmitry Mishin 
 * (Uses code from edu.jhu.pha.vospace.swiftapi.SwiftClient)
 */
public class SwiftStorageManager {

    private FilesClient cli = null;

    /**
     * Authenticate the client to the current backend storage
     * @param endpoint The storage URL
     * @param credentials The client's security credentials
     */
    public void authenticate(String endpoint, HashMap<String, String> credentials) throws VOSpaceException {
	try {
	    if (cli == null) {
		cli = new FilesClient(credentials.get("user"), credentials.get("password"), endpoint);
	    }
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Create a container at the specified location in the current backend storage
     * @param location The location of the container
     */
    public void createContainer(String location) throws VOSpaceException {
	NodePath npath = PathSeparator.splitPath(location, false);
	try {
	    getClient().createContainer(npath.getContainerName());
	} catch (Exception ex) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, ex.getMessage());
	}
    }

    /**
     * Move the bytes from the specified old location to the specified new location 
     * in the current backend storage
     * @param oldLocation The old location of the bytes
     * @param newLocation The new location of the bytes
     */
    public void moveBytes(String oldLocation, String newLocation) throws VOSpaceException {
	// TODO
    }

    /**
     * Copy the bytes from the specified old location to the specified new location
     * in the current backend storage
     * @param oldLocation The old location of the bytes
     * @param newLocation The new location of the bytes
     */
    public void copyBytes(String oldLocation, String newLocation) throws VOSpaceException {
	// TODO
    }

    /**
     * Put the bytes from the specified input stream at the specified location in 
     * the current backend storage
     * @param location The location for the bytes
     * @param stream The stream containing the bytes
     */
    public void putBytes(String location, InputStream stream) throws VOSpaceException {
	NodePath npath = PathSeparator.splitPath(location, false);

	try {
	    getClient().storeStreamedObject(npath.getContainerName(), stream, "application/file", npath.getNodePath(), null);
	} catch (FilesException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	} catch (HttpException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	} catch (IOException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Get the bytes from the specified location in the current backend storage
     * @param location The location of the bytes
     * @return a stream containing the requested bytes
     */
    public InputStream getBytes(String location) throws VOSpaceException {
	NodePath npath = PathSeparator.splitPath(location, false);
	try {
	    return getClient().getObjectAsStream(npath.getContainerName(), npath.getNodePath());
	} catch (FilesAuthorizationException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	} catch (FilesInvalidNameException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	} catch (FilesNotFoundException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	} catch (HttpException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	} catch (IOException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Remove the bytes at the specified location in the current backend storage
     * @param location The location of the bytes
     */
    public void removeBytes(String location) throws VOSpaceException {
	NodePath npath = PathSeparator.splitPath(location, false);
	try {
	    if (null == npath.getNodePath()) {
		getClient().deleteContainer(npath.getContainerName());
	    } else {
		getClient().deleteObject(npath.getContainerName(), npath.getNodePath());
	    }
	} catch (FilesNotFoundException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	} catch (FilesException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	} catch (HttpException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	} catch (IOException e) {
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
	return -1;
    }
	
    /**
     * @return OpenStack connector
     */
    private FilesClient getClient() throws VOSpaceException{
	if (cli.isLoggedin()) {
	    return cli;
	}
	if (null != cli.getUserName() && null != cli.getPassword()) {
	    try {
		cli.login();
		return cli;
	    } catch (HttpException e) {
		throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	    } catch (IOException e) {
		throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	    }
	} else {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, "You should be logged in. Please initialise with login and password first."+cli.getUserName() +" "+ cli.getPassword());
	}
    }
}