/**
 * MetaStore.java
 * Author: Matthew Graham (Caltech)
 * Version: Original (0.1) - 27 June 2006
 */

package edu.caltech.vao.vospace.meta;

import edu.caltech.vao.vospace.NodeType;
import org.apache.commons.pool.ObjectPool;

import java.net.URISyntaxException;
import java.sql.SQLException;
import edu.caltech.vao.vospace.VOSpaceException;

/**
 * This interface represents a metadata store for VOSpace
 */
public interface MetaStore {

    /*
     * Get the job with the specified identifier
     * @param jobID The ID of the job to get
     * @return The requested job or <i>null</i> if there is no job with the given ID.
     */
    public String getJob(String jobID) throws SQLException;

    /*
     * Set the id of the store
     * @param id The id of the store
     */
    public void setStoreID(int id);

    /*
     * Add the job with the specified identifier
     * @param jobID The ID of the job to get
     * @param job The XML string representation of the job
     */
    public void addJob(String jobID, String job) throws SQLException;

    /*
     * Check whether the object with the specified identifier is in the store
     */
    public boolean isStored(String identifier) throws SQLException;

    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, Object metadata) throws SQLException, VOSpaceException;

    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, String owner, Object metadata) throws SQLException, VOSpaceException;

    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, String owner, String location, Object metadata) throws SQLException, VOSpaceException;

    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, String view, String owner, String location, Object metadata) throws SQLException, VOSpaceException;

    public String checkData(String[] identifiers, int limit) throws SQLException;

    public boolean getAllData(String token, int limit) throws SQLException ;

    /*
     * Retrieve the metadata for the specified identifier at the specified level
     * of detail
     */
    public String[] getData(String[] identifiers, String token, int limit) throws SQLException, VOSpaceException;

    /*
     * Retrieves the metadata for the specified identifier as a Node Object to be
     * later serialized to XML via JDOM2
     */
    public ca.nrc.cadc.vos.Node[] getDataJDOM2(String[] identifiers, String token, int limit) throws SQLException, VOSpaceException;

    /*
     * Get the target of a link node
     */
    public String getTarget(String linkId) throws SQLException;

    /*
     * Remove the metadata for the specified identifier
     */
    public String[] removeData(String identifier, boolean container) throws SQLException, VOSpaceException;

    /*
     * Update the metadata for the specified identifier
     */
    public void updateData(String identifier, Object metadata) throws SQLException, VOSpaceException;

    /*
     * Update the metadata for the specified identifier including updating the
     * identifier
     */
    public void updateData(String identifier, String newIdentifier, Object metadata) throws SQLException, VOSpaceException;

    /*
     * Update the metadata for the specified identifier including updating the
     * identifier and the location
     */
    public void updateData(String identifier, String newIdentifier, String newLocation, Object metadata) throws SQLException, VOSpaceException;

    /*
     * Get a token
     */
    public String getToken(String[] identifiers) throws SQLException;

    /*
     * Get the physical location of the specified identifier
     */
    public String getLocation(String identifier) throws SQLException;

    /*
     * Set the physical location of the specified identifier
     */
    public void setLocation(String identifier, String location);

    /*
     * Get the status of the object with the specified identifier
     */
    public boolean getStatus(String identifier) throws SQLException;

    /*
     * Set the status of the object with the specified identifier
     */
    public void setStatus(String identifier, boolean status) throws SQLException;

    /*
     * Get the type of the object with the specified identifier
     */
    public int getType(String identifier) throws SQLException;

    /*
     * Get the owner of the object with the specified identifier
     */
    public String getOwner(String identifier) throws SQLException;

    /*
     * Check whether the specified property is known to the service
     */
    public boolean isKnownProperty(String identifier) throws SQLException;

    /*
     * Register the specified property
     */
    public void registerProperty(String property, int type, boolean readOnly) throws SQLException;


    /*
     * Store the details of the specified transfer
     */
    public void storeTransfer(String identifier, String endpoint) throws SQLException;

    /*
     * Retrieve the job associated with the specified endpoint
     */
    public String getTransfer(String endpoint) throws SQLException;

    /**
     * Check whether the specified transfer has completed
     */
    public boolean isCompleted(String jobid) throws SQLException;

    /**
     * Check whether the specified transfer has completed
     */
    public boolean isCompletedByEndpoint(String jobid) throws SQLException;

    /*
     * Store the original view of the specified object
     */
    public void setView(String identifier, String view) throws SQLException;

    /*
     * Get the original view of the object with the specified identifier
     */
    public String getView(String identifier) throws SQLException;

    /*
     * Resolve a location from the specified endpoint
     */
    public String resolveLocation(String endpoint) throws SQLException;

    /*
     * Mark the specified transfer as complete
     */
    public void completeTransfer(String endpoint) throws SQLException;

    /*
     * Mark the specified transfer as complete
     */
    public void completeTransfer(String endpoint, boolean updateStatus) throws SQLException;

    /*
     * Return the identifier associated with the transfer
     */
    public String resolveTransfer(String endpoint) throws SQLException;

    /*
     * Return the creation date of a transfer
     */
    public long getCreated(String endpoint) throws SQLException;

    /*
     * Return the identifier associated with the specified location
     */
    public String resolveIdentifier(String location) throws SQLException;

    /*
     * Update the specified property
     */
    public void updateProperty(String property, int type) throws SQLException;

    /*
     * Update the specified property
     */
    public void updateProperty(String property, int type, boolean readOnly) throws SQLException;

    /*
     * Get the properties of the specified type
     */
    public String[] getProperties(int type) throws SQLException;

    /*
     * Get the property type of the specified node
     */
    public String getPropertyType(String identifier) throws SQLException;

    /*
     * Check whether the property is read/only
     */
    public boolean isReadOnly(String property) throws SQLException;

    /*
     * Get the value of a property
     */
    public String getPropertyValue(String identifier, String property) throws SQLException;

    /*
     * Get the value of several properties at once; useful if they are built-in properties
     */
    public String[] getPropertyValues(String identifier, String[] properties) throws SQLException;

    /*
     * Get the direct children of the specified container node
     */
    public String[] getChildren(String identifier) throws SQLException, VOSpaceException;

    /*
     * Get the direct children nodes of the specified container node
     */
    public String[] getChildrenNodes(String identifier) throws SQLException, VOSpaceException;

    /*
     * Get the direct children nodes of the specified container node as a node object
     */
    public ca.nrc.cadc.vos.Node[] getChildrenNodesJDOM2(String identifier) throws SQLException, VOSpaceException;

    /*
     * Get all the children of the specified container node
     */
    public String[] getAllChildren(String identifier) throws SQLException, VOSpaceException;

    /*
     * Store a result associated with a Job
     */
    public void addResult(String identifier, String result) throws SQLException;

   /*
     * Get a result associated with a Job
     */
    public String getResult(String identifier) throws SQLException;

    /*
     *     Get the specified node
     */
    public String getNode(String identifier) throws SQLException, VOSpaceException;

    /*
     * Check the status of a capability (active or not)
     */
    public int isActive(String identifier, String capability) throws SQLException;


    /*
     * Set the status of a capability (active or not)
     */
    public void setActive(String identifier, String capability, int port) throws SQLException;


    /*
     * Register the capabilities
     */
    public void registerCapability(String identifier, String capability) throws SQLException;

    /*
     * Check whether the capability is registered
     */
    public boolean isKnownCapability(String capability) throws SQLException;

    /*
     * Get next available capability port
     */
    public int getCapPort() throws SQLException;

    /**
     * Check whether transfer associated with a Job exists
     */
    public boolean isTransfer(String identifier) throws SQLException;

    /**
     * Check whether transfer associated with a Job exists
     */
    public boolean isTransferByEndpoint(String identifier) throws SQLException;

    /**
     * Get the last modification time of the node
     */
    public long getLastModTime(String identifier) throws SQLException;

    /**
     * Return a NodeType based on node type id
     */
    public static NodeType getNodeType(int id) {
        for (NodeType t: NodeType.values()) {
            if (t.ordinal() == id) {
                return t;
            }
        }
        return null;
    }

}
