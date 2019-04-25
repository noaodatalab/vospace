/**
 * MySQLMetaStore.java
 * Author: Matthew Graham (Caltech)
 * Version: Original (0.1) - 23 August 2006
 */

package edu.caltech.vao.vospace.meta;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;

import edu.caltech.vao.vospace.xml.Node;
import edu.caltech.vao.vospace.xml.NodeFactory;
import edu.caltech.vao.vospace.xml.LinkNode;
import edu.caltech.vao.vospace.xml.DataNode;
import edu.caltech.vao.vospace.NodeType;
import edu.caltech.vao.vospace.Props;
import edu.caltech.vao.vospace.VOSpaceException;
import edu.caltech.vao.vospace.VOSpaceManager;

/**
 * This class represents a metadata store for VOSpace based on the MySQL
 * open source database
 */
public class MySQLMetaStore implements MetaStore {
    private static final String DEFAULT_DB_URL = "localhost/vospace";
    private static final String DEFAULT_DB_UID = "dba";
    private static final String DEFAULT_DB_PWD = "dba";
    public static final int MIN_DETAIL = 1;
    public static final int PROPERTY_DETAIL = 2;
    public static final int MAX_DETAIL = 3;
    private String DB_URL;
    private String DB_UID;
    private String DB_PWD;
    private String connectionURL;
    private String[] propertyColumns = null;
    private int STOREID = 0;
    private int CONNID = 0;

    /**
     * Construct a basic MySQLMetaStore
     */
    public MySQLMetaStore(Properties props) {
        try {
            // Load the jdbc driver
            Class.forName("com.mysql.jdbc.Driver");
            // Enable logging
            DriverManager.setLogStream(null);
            // Connection URL
            DB_URL = props.containsKey("server.meta.dburl") ? props.getProperty("server.meta.dburl") : DEFAULT_DB_URL;
            DB_UID = props.containsKey("server.meta.dbuid") ? props.getProperty("server.meta.dbuid") : DEFAULT_DB_UID;
            DB_PWD = props.containsKey("server.meta.dbpwd") ? props.getProperty("server.meta.dbpwd") : DEFAULT_DB_PWD;
            connectionURL = "jdbc:mysql://" + DB_URL + "?" + "user=" + DB_UID + "&" + "password=" + DB_PWD;
            // Get object pool
            ObjectPool connectionPool = new GenericObjectPool(null);
            // Get connection factory
            ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectionURL, null);
            PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);
            // Get pooling driver
            Class.forName("org.apache.commons.dbcp.PoolingDriver");
            PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
            driver.registerPool("connPool", connectionPool);
        } catch (Exception e) {
            e.printStackTrace();
            //      log.error(e.getMessage());
        }
    }


    /*
     * Standardize the VOSpace identifier
     * @param id The id of the node
     */
    public String fixId(String identifier) {
        return identifier.replace("~", "!");
    }

    /*
     * Escape the identifier when used in a 'LIKE' SQL statement; also standardizes the identifier
     * @param id The id of the node
     */
    public String escapeId(String identifier) {
        return StringUtils.replaceEach(identifier, new String[]{"~","%","_"}, new String[]{"!","\\%","\\_"});
    }

    /*
     * Escape a generic string when used in a 'LIKE' SQL statement
     * @param string The string to escape
     */
    public String escapeStr(String string) {
        return StringUtils.replaceEach(string, new String[]{"%","_"}, new String[]{"\\%","\\_"});
    }

    /*
     * Calculate the directory depth of an identifier
     */
    protected int getIdDepth(String identifier) {
        return StringUtils.countMatches(identifier, "/") - 3;
    }

    /*
     * Set the id of the store
     * @param id The id of the store
     */
    public void setStoreID(int id) {
        STOREID = id;
    }


   /*
    * Get a db connection
    */
    private Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:apache:commons:dbcp:connPool");
//      System.err.println("Getting connection: " + STOREID + "-" + ++CONNID);
        return connection;
    }

    /*
     * Get the job with the specified identifier
     * @param jobID The ID of the job to get
     * @return The requested job or <i>null</i> if there is no job with the given ID.
     */
    public String getJob(String jobID) throws SQLException {
        String job = null;
        String query = "select job from jobs where identifier = '" + jobID + "'";
        job = getAsString(query);
        return job;
    }

    /*
     * Add the job with the specified identifier
     * @param jobID The ID of the job to get
     * @param job The XML string representation of the job
     */
    public void addJob(String jobID, String job) throws SQLException {
        String query = "insert into jobs (identifier, job) values ('" + jobID + "', '" + job + "')";
        update(query);
    }

   /*
     * Check whether the object with the specified identifier is in the store
     * @param identifier The ID of the node to check
     * @return whether the node is stored or not
     */
    public boolean isStored(String identifier) throws SQLException {
        boolean isStored = false;
        String query = "select identifier from nodes where identifier = '" + fixId(identifier) + "'";
        isStored = extantEntry(query);
        return isStored;
    }

    /*
     * Get the type of the object with the specified identifier
     * @param identifier The ID of the node
     * @return the type of the node
     */
    public int getType(String identifier) throws SQLException {
        int type = -1;
        String query = "select type from nodes where identifier = '" + fixId(identifier) + "'";
        type = getAsInt(query);
        return type;
    }

    /*
     * Get the owner of the object with the specified identifier
     * @param identifier The Id of the node
     * @return the owner of the node
     */
    public String getOwner(String identifier) throws SQLException {
        String owner = "";
        String query = "select owner from nodes where identifier = '" + fixId(identifier) + "'";
        owner = getAsString(query);
        return owner;
    }

    /*
     * Check whether the specified property is known to the service
     * @param identifier The ID of the property
     * @return whether the property is known
     */
    public boolean isKnownProperty(String identifier) throws SQLException {
        boolean known = false;
        String query = "select * from metaproperties where identifier = '" + identifier + "'";
        known = extantEntry(query);
        return known;
    }

    /*
     * Register the specified property
     * @param property The property identifier
     * @param type The property type
     * @param readOnly Is the property read-only?
     */
    public void registerProperty(String property, int type, boolean readOnly) throws SQLException {
        int wp = 0;
        if (readOnly) wp = 1;
        String query = "insert into metaproperties (identifier, type, readOnly) values ('" + property + "', " + type + ", " + wp + ")";
        update(query);
    }

    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, Object metadata) throws SQLException, VOSpaceException {
        if (metadata instanceof String) {
            // String query = "insert into nodes (identifier, type, creationDate, node) values ('" + fixId(identifier)
            //      + "', '" + type + "', cast(now() as datetime), '" + (String) metadata + "')";
            String fixedId = fixId(identifier);
            String query = "insert into nodes (identifier, depth, type, creationDate) values ('"
                    +fixedId+ "', " +getIdDepth(fixedId)+ ", " +type+ ", cast(now() as datetime))";
            update(query);
            storeProperties((String) metadata);
        }
    }

    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, String owner, Object metadata) throws SQLException, VOSpaceException {
        if (metadata instanceof String) {
            // String query = "insert into nodes (identifier, type, owner, creationDate, node) values ('" + fixId(identifier)
            //      + "', '" + type + "', '" + owner + "', cast(now() as datetime), '" + (String) metadata + "')";
            String fixedId = fixId(identifier);
            String query = "insert into nodes (identifier, depth, type, owner, creationDate) values ('"
                    +fixedId+ "', " +getIdDepth(fixedId)+ ", " +type+ ", '" +owner+ "', cast(now() as datetime))";
            update(query);
            storeProperties((String) metadata);
        }
    }

    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, String owner, String location, Object metadata) throws SQLException, VOSpaceException {
        if (metadata instanceof String) {
            // String query = "insert into nodes (identifier, type, owner, location, creationDate, node) values ('" + fixId(identifier)
            //      + "', '" + type + "', '" + owner + "', '" + location + "', cast(now() as datetime), '" + (String) metadata + "')";
            String fixedId = fixId(identifier);
            String query = "insert into nodes (identifier, depth, type, owner, location, creationDate) values ('"
                    +fixedId+ "', " +getIdDepth(fixedId)+ ", " +type+ ", '" +owner+ "', '" +location+ "', cast(now() as datetime))";
            update(query);
            storeProperties((String) metadata);
        }
    }

    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, String view, String owner, String location, Object metadata) throws SQLException, VOSpaceException {
        if (metadata instanceof String) {
            // String query = "insert into nodes (identifier, type, view, owner, location, creationDate, node) values ('" + fixId(identifier)
            //      + "', '" + type + "', '" + view + "', '" + owner + "', '" + location + "', cast(now() as datetime), '" + (String) metadata + "')";
            String fixedId = fixId(identifier);
            String query = "insert into nodes (identifier, depth, type, view, owner, location, creationDate) values ('"
                    +fixedId+ "', " +getIdDepth(fixedId)+ ", " +type+ ", '" +view+ "', '" +owner+ "', '" +location+ "', cast(now() as datetime))";
            update(query);
            storeProperties((String) metadata);
        }
    }


    public String checkData(String[] identifiers, int limit) throws SQLException {
        String whereQuery = null, token = null;
        // Get count
        for (int i = 0; i < identifiers.length; i++) {
            if (i == 0) whereQuery = "where";
            if (identifiers[i].contains("*")) {
                whereQuery += " identifier like '" + escapeId(identifiers[i]).replace("*", "%") + "'";
            } else {
                whereQuery += " identifier = '" + fixId(identifiers[i]) + "'";
            }
            if (i != identifiers.length - 1) whereQuery += " or ";
        }
        String query = "select count(identifier) from nodes " + whereQuery;
        int count = getAsInt(query);
        if (limit < count) {
            token = UUID.randomUUID().toString();
            String createToken = "insert into listings (token, offset, count, whereQuery) values ('" + token + "', " + 0 + ", " + count + ", '" + whereQuery.replace("'", "\\'") + "')";
            update(createToken);
        }
        return token;
    }

    public boolean getAllData(String token, int limit) throws SQLException {
        boolean allData = false;
        String query = "select offset, count from listings where token = '" + token + "'";
        ResultSet result = null;
        try {
            result = execute(query);
            result.next();
            int offset = result.getInt(1);
            int count = result.getInt(2);
            if (offset + limit >= count) {
                allData = true;
            } else {
                String updateOffset = "update listings set offset = " + (offset + limit) + " where token ='" + token + "'";
                update(updateOffset);
            }
        } finally {
            result.close();
        }
        return allData;
    }

    /*
     * Retrieve the metadata for the specified identifier at the specified
     * level of detail
     */
    public String[] getData(String[] identifiers, String token, int limit) throws SQLException, VOSpaceException {
        String query = null, whereQuery = null;
        int count = 0, offset = 0;
        // Get count
        for (int i = 0; i < identifiers.length; i++) {
            if (i == 0) whereQuery = "where";
            if (identifiers[i].contains("*")) {
                whereQuery += " identifier like '" + escapeId(identifiers[i]).replace("*", "%") + "'";
            } else {
                whereQuery += " identifier = '" + fixId(identifiers[i]) + "'";
            }
            if (i != identifiers.length - 1) whereQuery += " or ";
        }
        if (token != null) {
            String tokenQuery = "select offset, count, updateDate, whereQuery from listings where token = '" + token + "'";
            ResultSet tokenResult = null;
            try {
                tokenResult = execute(tokenQuery);
                if (tokenResult.next()) {
                    offset = tokenResult.getInt(1);
                    count = tokenResult.getInt(2);
                    whereQuery = tokenResult.getString(4);
                } else {
                    throw new SQLException("Invalid token");
                }
            } finally {
                closeResult(tokenResult);
            }
        }
        // Construct listing query
        // query = "select node from nodes ";
        query = "select identifier, type from nodes ";
        //      query += whereQuery + " order by identifier ";
        query += whereQuery + " order by type ";
        if (limit > 0) query += " limit " + limit;
        if (offset > 0) query += " offset " + offset;
        // String[] nodes = getAsStringArray(fixId(query));
        ResultSet result = null;
        ArrayList<String> nodes = new ArrayList<String>();
        try {
            result = execute(query);
            while (result.next()) {
                String nodeId = result.getString(1);
                int nodeType = result.getInt(2);
                nodes.add(createNode(nodeId, nodeType));
            }
        } finally {
            closeResult(result);
        }
        return nodes.toArray(new String[0]);
    }

    /*
     * Get the target of a link node
     */
    public String getTarget(String linkId) throws SQLException {
        String lquery = "select target from links where identifier = '" + fixId(linkId) + "'";
        return getAsString(lquery);
    }

    /*
     * Create the specified node string by combining the other data stored in the database
     */
    private String createNode(String identifier, int type) throws SQLException, VOSpaceException {
        // Create a new Node object of the proper type
        String fixedId = fixId(identifier);
	    Node node = NodeFactory.getInstance().getNodeByType(NodeType.getUriById(type));
        // Set the URI for the node to the identifier
        node.setUri(fixedId);
        // Get the Properties for the node, and set them in the Node object
        String[] propNames = Props.allProps();
        // First build a query of all column names to get all column values
        String query = "select " + StringUtils.join(propNames, ",") + " from properties where identifier = '" + fixedId + "'";
        // Execute the query and set the property values in the Node.
        ResultSet result = null;
        try {
            result = execute(query);
            if (result.next()) {
                String value = null;
                for (String name: propNames) {
                    value = result.getString(name);
                    if (value != null) node.setProperty(Props.getURI(name), value);
                }
            }
        } finally {
            closeResult(result);
        }
        query = "select property, value from addl_props where identifier = '" + fixedId + "'";
        // Execute the query and set the property values in the Node.
        try {
            result = execute(query);
            while (result.next()) {
                String property = result.getString(1);
                String value = result.getString(2);
                if (value != null) node.setProperty(property, value);
            }
        } finally {
            closeResult(result);
        }
        if (node instanceof LinkNode) ((LinkNode) node).setTarget(getTarget(fixedId));
        // Set the Views and Capabilities; unfortunately requires the VOSpaceManager
        if (node instanceof DataNode) VOSpaceManager.getInstance().addViewsAndCapabilities((DataNode) node);
        // Return the Node cast back to String
        return node.toString();
    }

    /*
     * Get the specified node
     */
    public String getNode(String identifier) throws SQLException, VOSpaceException {
        /* String node = null;
        String query = "select node from nodes where identifier = '" + fixId(identifier) + "'";
        node = getAsString(query);
        return node; */
        return createNode(identifier, getType(identifier));
    }


    /*
     * Get the direct children of the specified container node
     */
    public String[] getChildren(String identifier) throws SQLException {
        String query = "select identifier from nodes where depth = " + (getIdDepth(identifier) + 1)
                + " and identifier like '" + escapeId(identifier) + "/%'";
        return getAsStringArray(query);
        /*
        ArrayList<String> children = new ArrayList<String>();
        String query = "select identifier from nodes where identifier like '" + escapeId(identifier) + "/%'";
        for (String child: getAsStringArray(query)) {
            if (!child.equals(fixId(identifier)) && !child.substring(identifier.length() + 1).contains("/")) {
                children.add(child);
            }
        }
        return children.toArray(new String[0]);
        */
    }


    /*
     * Get the direct children nodes of the specified container node
     */
    public String[] getChildrenNodes(String identifier) throws SQLException, VOSpaceException {
        /* String query = "select node from nodes where identifier like '" + fixId(identifier) + "/%' and identifier not like '" + fixId(identifier) + "/%/%'";
        String[] children = getAsStringArray(query);
        return children; */
        String query = "select identifier, type from nodes where depth = " + (getIdDepth(identifier) + 1)
                + " and identifier like '" + escapeId(identifier) + "/%'";
        ResultSet result = null;
        ArrayList<String> children = new ArrayList<String>();
        try {
            result = execute(query);
            while (result.next()) {
                String childId = result.getString(1);
                int childType = result.getInt(2);
                children.add(createNode(childId, childType));
            }
        } finally {
            closeResult(result);
        }
        return children.toArray(new String[0]);
    }


    /*
     * Get all the children of the specified container node
     */
    public String[] getAllChildren(String identifier) throws SQLException {
        ArrayList<String> children = new ArrayList<String>();
        String query = "select identifier from nodes where identifier like '" + escapeId(identifier) + "/%'";
        for (String child : getAsStringArray(query)) {
            if (!child.equals(fixId(identifier))) {
                children.add(child);
            }
        }
        return children.toArray(new String[0]);
    }

    /*
     * Remove the metadata for the specified identifier
     * @param identifier The (root) identifier of the node(s) to delete
     * @param container Does the identifier refer to a container?
     * Returns a list of links which pointed to the deleted identifier and were
     * also removed in the process of removing the identifier.
     */
    public String[] removeData(String identifier, boolean container) throws SQLException {
        String query = "";
        ArrayList<String> removedLinks = new ArrayList<String>();
        if (container) {
            String escaped = escapeId(identifier);
            query = "delete from nodes where identifier like '" + escaped + "/%'";
            update(query);
            query = "delete from properties where identifier like '" + escaped + "/%'";
            update(query);
            query = "delete from addl_props where identifier like '" + escaped + "/%'";
            update(query);
            query = "delete from links where identifier like '" + escaped + "/%'";
            update(query);
            // Find all the links to the container contents
            query = "select identifier from links where target like '" + escaped + "/%'";
            Collections.addAll(removedLinks, getAsStringArray(query));
        }
        identifier = fixId(identifier);
        query = "delete from nodes where identifier = '" + identifier + "'";
        update(query);
        query = "delete from properties where identifier = '" + identifier + "'";
        update(query);
        query = "delete from addl_props where identifier = '" + identifier + "'";
        update(query);
        query = "delete from links where identifier = '" + identifier + "'";
        update(query);
        // Find all the links to the identifier
        query = "select identifier from links where target = '" + identifier + "'";
        Collections.addAll(removedLinks, getAsStringArray(query));
        // Recursively remove all the links to the nodes we removed.
        for (String link: (ArrayList<String>)removedLinks.clone()) {
            // System.out.println("Removing " + link);
            Collections.addAll(removedLinks, removeData(link, false));
        }
        if (identifier.endsWith("_cap.conf")) {
            String parent = identifier.substring(0, identifier.lastIndexOf("/"));
            String shortCap = identifier.substring(identifier.lastIndexOf("/") + 1, identifier.lastIndexOf("_cap.conf"));
            query = "update capabilities set active = 0 where identifier = '" + parent + "' and capability like '%" + escapeStr(shortCap) + "'";
            update(query);
        }
        return removedLinks.toArray(new String[removedLinks.size()]);
    }

    /*
     * Update the metadata for the specified identifier
     */
    public void updateData(String identifier, Object metadata) throws SQLException, VOSpaceException {
        if (metadata instanceof String) {
            String node = updateProperties((String) metadata);
            // Force an update to lastModificationTime
            String query = "update nodes set lastModificationDate=cast(now() as datetime) where identifier = '" + fixId(identifier) + "'";
            update(query);
            /* String encode = node.replace("\"", "'");
            String query = "update nodes set node = \"" + encode + "\" where identifier = '" + fixId(identifier) + "'";
            update(query); */
        }
    }

    /*
     * Update the metadata for the specified identifier including updating the
     * identifier
     */
    public void updateData(String identifier, String newIdentifier, Object metadata) throws SQLException, VOSpaceException {
        if (metadata instanceof String) {
            String node = updateProperties((String) metadata);
            String fixedId = fixId(identifier);
            String fixedNewId = fixId(newIdentifier);
            String query = "update nodes set identifier = '" + fixedNewId + "' where identifier = '" + fixedId + "'";
            update(query);
            /* String encode = node.replace("\"", "'");
            String query = "update nodes set identifier = '" + newIdentifier + "', node = \"" + encode + "\" where identifier = '" + fixId(identifier) + "'";
            update(query); */
            query = "update properties set identifier = '" + fixedNewId + "' where identifier = '" + fixedId + "'";
            update(query);
            query = "update addl_props set identifier = '" + fixedNewId + "' where identifier = '" + fixedId + "'";
            update(query);
        }
    }

    /*
     * Update the metadata for the specified identifier including updating the
     * identifier and the location
     */
    public void updateData(String identifier, String newIdentifier, String newLocation, Object metadata) throws SQLException, VOSpaceException {
        if (metadata instanceof String) {
            String node = updateProperties((String) metadata);
            String fixedId = fixId(identifier);
            String fixedNewId = fixId(newIdentifier);
            String query = "update nodes set identifier = '" + fixedNewId + "', location = '" + newLocation + "' where identifier = '" + fixedId + "'";
            update(query);
            /* String encode = node.replace("\"", "'");
            String query = "update nodes set identifier = '" + fixId(newIdentifier) + "', location = '" + newLocation + "', node = \"" + encode + "\" where identifier = '" + fixId(identifier) + "'";
            update(query); */
            query = "update properties set identifier = '" + fixedNewId + "' where identifier = '" + fixedId + "'";
            update(query);
            query = "update addl_props set identifier = '" + fixedNewId + "' where identifier = '" + fixedId + "'";
            update(query);
        }
    }

    /*
     * Get a token
     */
    public String getToken(String[] identifiers) throws SQLException {
        String whereQuery = null, query = null;
        for (int i = 0; i < identifiers.length; i++) {
            if (i == 0) whereQuery += "where";
            if (identifiers[i].contains("*")) {
                whereQuery += " identifier like '" + escapeId(identifiers[i]).replace("*", "%") + "'";
            } else {
                whereQuery += " identifier = '" + fixId(identifiers[i]) + "'";
            }
            if (i != identifiers.length - 1) query += " or ";
        }
        String token = null;
        query = "select token from listings where whereQuery = '" + whereQuery.replace("'", "\\'") + "'";
        token = getAsString(fixId(query));
        return token;
    }

    /*
     * Get the physical location of the specified identifier
     */
    public String getLocation(String identifier) throws SQLException {
        String location = null;
        String query = "select location from nodes where identifier = '" + fixId(identifier) + "'";
        location = getAsString(query);
        return location;
    }

    /**
     * Set the physical location of the specified identifier
     */
    public void setLocation(String identifier, String location) {
    }

    /*
     * Get the status of the object with the specified identifier
     */
    public boolean getStatus(String identifier) throws SQLException {
        return false;
    }

    /*
     * Set the status of the object with the specified identifier
     */
    public void setStatus(String identifier, boolean status) throws SQLException {
        String query = null;
        if (status) {
            query = "update nodes set status = 1 where identifier = '" + fixId(identifier) + "'";
        } else {
            query = "update nodes set status = 0 where identifier = '" + fixId(identifier) + "'";
        }
        update(query);
    }


    /*
     * Store the details of the specified trafnsfer
     */
    public void storeTransfer(String identifier, String endpoint) throws SQLException {
        String query = "insert into transfers (jobid, endpoint, created) values ('" + fixId(identifier) + "', '" + endpoint + "', cast(now() as datetime))";
        update(query);
    }

    /*
     * Retrieve the job associated with the specified endpoint
     */
    public String getTransfer(String endpoint) throws SQLException {
        String query = "select details from results j, transfers t where t.jobid = j.identifier and t.endpoint like '%" + escapeStr(endpoint) + "'";
        String job = getAsString(query);
        return job;
    }

    /**
     * Check whether the specified transfer has completed
     */
    public boolean isCompleted(String jobid) throws SQLException {
        boolean completed = false;
        String query = "select completed from transfers where jobid = '" + jobid + "'";
        if (getAsDate(query) != null) completed = true;
        return completed;
    }

    /**
     * Check whether the specified transfer has completed
     */
    public boolean isCompletedByEndpoint(String endpoint) throws SQLException {
        boolean completed = false;
        String query = "select completed from transfers where endpoint like '%" + escapeStr(endpoint) + "'";
        if (getAsDate(query) != null) completed = true;
        return completed;
    }

    /*
     * Store the original view of the specific object
     */
    public void setView(String identifier, String view) throws SQLException {
        String query = "update nodes set view = '" + view + "' where identifier = '" + fixId(identifier) + "'";
        update(query);
    }

    /*
     * Get the original view of the object with the specified identifier
     */
    public String getView(String identifier) throws SQLException {
        String view = null;
        String query = "select view from nodes where identifier = '" + fixId(identifier) + "'";
        view = getAsString(query);
        return view;
    }

    /*
     * Resolve a location from the specified endpoint
     */
    public String resolveLocation(String endpoint) throws SQLException {
        String location = null;
//      String query = "select n.location from nodes n, transfers t where n.identifier = t.identifier and locate('" + endpoint + "', t.endpoint) > 0 and timestampdiff(minute, t.created, now()) < 60 and completed is null";
        String query = "select location from transfers where locate('" + endpoint + "', endpoint) > 0 and timestampdiff(minute, created, now()) < 60 and completed is null";
        location = getAsString(query);
        return location;
    }

    /*
     * Return the creation date of a transfer
     */
    public long getCreated(String endpoint) throws SQLException {
        long created = 0;
        String query = "select created from transfers where locate('" + endpoint + "', endpoint) > 0";
        created = getAsTime(query);
        return created;
    }

    /*
     * Return the identifier associated with the specified location
     */
    public String resolveIdentifier(String location) throws SQLException {
        String identifier = null;
        String query = "select identifier from nodes where location like '" + escapeStr(location) + "'";
        identifier = fixId(getAsString(query));
        return identifier;
    }

    /*
     * Mark the specified transfer as complete
     */
    public void completeTransfer(String endpoint) throws SQLException {
        String query = "update transfers set completed = cast(now() as datetime) where endpoint like '%" +  escapeStr(endpoint) + "'";
        update(query);
    }

    /*
     * Mark the specified transfer as complete
     */
    public void completeTransfer(String endpoint, boolean updateStatus) throws SQLException {
        /*      Statement statement = getConnection().createStatement();
        completeTransfer(endpoint);
        NodeType node = null;
        String identifier = resolveTransfer(endpoint);
        String query = "select node from nodes where identifier = '" + identifier + "'";
        ResultSet result = execute(query);
        if (result.next()) {
            try {
                node = NodeType.Factory.parse(result.getString(1));
                if (node instanceof DataNodeType) {
                    ((DataNodeType) node).setBusy(updateStatus);
                    updateData(identifier, node.xmlText(options));
                }
                setStatus(identifier, updateStatus);
            } catch (XmlException e) {
                throw new SQLException(e.getMessage());
            }
            }*/
    }

    /*
     * Return the identifier associated with the transfer
     */
    public String resolveTransfer(String endpoint) throws SQLException {
        String identifier = null;
        String query = "select identifier from transfers where locate('" + endpoint + "', endpoint) > 0";
        identifier = getAsString(query);
        return identifier;
    }


    /*
     * Update the specified property
     */
    public void updateProperty(String property, int type) throws SQLException {
        String query = "update metaproperties set type = " + type + " where identifier = '" + property + "'";
        update(query);
    }

    /*
     * Update the specified property
     */
    public void updateProperty(String property, int type, boolean readOnly) throws SQLException {
        String query = "update metaproperties set type = " + type + ", readonly = "
                + BooleanUtils.toInteger(readOnly) + " where identifier = '" + property + "'";
        update(query);
    }

    /*
     * Get the properties of the specified type
     */
    public String[] getProperties(int type) throws SQLException {
        String query = "select identifier from metaproperties where type & "  + type + " = " + type;
        String[] list = getAsStringArray(query);
        return list;
    }

    /*
     * Get the property type of the specified node
     */
    public String getPropertyType(String identifier) throws SQLException {
        String type = null;
        String query = "select type from metaproperties where identifier = '" + identifier + "'";
        type = getAsString(query);
        return type;
    }

    /*
     * Check whether the property is read/only
     */
    public boolean isReadOnly(String property) throws SQLException {
        boolean readOnly = false;
        String query = "select readonly from metaproperties where identifier = '" + property + "'";
        readOnly = getAsBoolean(query);
        return readOnly;
    }

    /*
     * Get the value of a property
     * Updated to get the property value from the column named by the property name
     */
    public String getPropertyValue(String identifier, String property) throws SQLException {
        String value = null;
        String columnName = Props.fromURI(property);
        if (columnName != null) {
            String query = "select " + columnName + " from properties where identifier = '" + fixId(identifier) + "'";
            value = getAsString(query);
        } else {
            String query = "select value from addl_props where identifier = '" + fixId(identifier) + "'" + " and property = '" + property + "'";
            value = getAsString(query);
        }
        return value;
    }

    /*
     * Get the value of a property
     * Updated to get the property value from the column named by the property name
     */
    public String[] getPropertyValues(String identifier, String[] properties) throws SQLException {
        ArrayList<String> columns = new ArrayList<String>();
        ArrayList<String> addl_props = new ArrayList<String>();
        HashMap<String, String> valMap = new HashMap();
        for (String s : properties) {
            String columnName = Props.fromURI(s);
            if (columnName != null) columns.add(columnName);
            else addl_props.add(s);
        }
        if (!columns.isEmpty()) {
            String query = "select " + StringUtils.join(columns, ",") + " from properties where identifier = '" + fixId(identifier) + "'";
            String[] values = getAsStringArray(query);
            for (int i = 0; i < values.length; i++) valMap.put(Props.getURI(columns.get(i)), values[i]);
        }
        if (!addl_props.isEmpty()) {
            String query = "select value from addl_props where identifier = '" + fixId(identifier)
                        + "' and property in ('" + StringUtils.join(addl_props, "','") + "')";
            String[] values = getAsStringArray(query);
            for (int i = 0; i < values.length; i++) valMap.put(addl_props.get(i), values[i]);
        }
        ArrayList<String> valList = new ArrayList<String>();
        for (String s : properties) valList.add(valMap.get(s));
        return valList.toArray(new String[0]);
    }

    /*
     * Check the status of a capability (active or not)
     */
    public int isActive(String identifier, String capability) throws SQLException {
        int isActive = 0;
        String query = "select active from capabilities where identifier = '" + fixId(identifier) + "' and capability = '" + capability + "'";
        isActive = getAsInt(query);
        return isActive;
    }


    /*
     * Set the status of a capability (active or not)
     */
    public void setActive(String identifier, String capability, int port) throws SQLException {
        String query = "update capabilities set active = " + port + " where identifier = '" + fixId(identifier) + "' and capability = '" + capability + "'";
        update(query);
    }


    /*
     * Register the capabilities
     */
    public void registerCapability(String identifier, String capability) throws SQLException {
        String query = "insert capabilities values('" + fixId(identifier) + "', '" + capability + "', 0)";
        update(query);
    }


    /*
     * Check whether the capability is registered
     */
    public boolean isKnownCapability(String capability) throws SQLException {
        boolean known = false;
        String query = "select identifier from capabilities where capability = '" + capability + "'";
        known = extantEntry(query);
        return known;
    }


    /*
     * Get next available capability port
     */
    public int getCapPort() throws SQLException {
        int port = -1;
        String query = "select max(active) from capabilities";
        port = getAsInt(query);
        //      if (port > 20000) port = port + 1;
        //      if (port == 0) port = 20001;
        return port;
    }


    /*
     * Execute a query on the store
     */
    private String getAsString(String query) throws SQLException {
        String ans = null;
        ResultSet result = null;
        try {
            result = execute(query);
            if (result.next()) ans = result.getString(1);
        } finally {
            closeResult(result);
        }
        return ans;
    }


    /*
     * Execute a query on the store
     */
    private int getAsInt(String query) throws SQLException {
        int ans = -1;
        ResultSet result = null;
        try {
            result = execute(query);
            if (result.next()) ans = result.getInt(1);
        } finally {
            closeResult(result);
        }
        return ans;
    }


    /*
     * Execute a query on the store
     */
    private boolean getAsBoolean(String query) throws SQLException {
        boolean ans = false;
        ResultSet result = null;
        try {
            result = execute(query);
            if (result.next()) ans = result.getBoolean(1);
        } finally {
            closeResult(result);
        }
        return ans;
    }


    /*
     * Execute a query on the store
     */
    private Date getAsDate(String query) throws SQLException {
        Date ans = null;
        ResultSet result = null;
        try {
            result = execute(query);
            if (result.next()) ans = result.getDate(1);
        } finally {
            closeResult(result);
        }
        return ans;
    }


    /*
     * Execute a query on the store
     */
    private long getAsTime(String query) throws SQLException {
        long ans = 0;
        ResultSet result = null;
        try {
            result = execute(query);
            if (result.next()) ans = result.getTimestamp(1).getTime();
        } finally {
            closeResult(result);
        }
        return ans;
    }


    /*
     * Execute a query on the store
     */
    private String[] getAsStringArray(String query) throws SQLException {
        String[] ans = null;
        ResultSet result = null;
        try {
            ArrayList<String> list = new ArrayList<String>();
            result = execute(query);
            while (result.next()) {
                list.add(result.getString(1));
            }
            ans = list.toArray(new String[0]);
        } finally {
            closeResult(result);
        }
        return ans;
    }


    /*
     * Execute a query on the store
     */
    private boolean extantEntry(String query) throws SQLException {
        boolean ans = false;
        ResultSet result = null;
        try {
            result = execute(query);
            if (result.next()) ans = true;
        } finally {
            closeResult(result);
        }
        return ans;
    }


    /*
     * Execute a query on the store
     */
    private ResultSet execute(String query) throws SQLException {
//        System.err.println(query);
        ResultSet result = null;
        Statement statement = getConnection().createStatement();
        boolean success = statement.execute(query);
        if (success) result = statement.getResultSet();
        return result;
    }

    /*
     * Insert/update query on the store
     */
    private void update(String query) throws SQLException {
//        System.err.println(query);
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            statement.executeUpdate(query);
        } finally {
            statement.close();
            connection.close();
//          System.err.println("*** Closing db connection: " + STOREID + "-" + CONNID);
        }
    }


    /*
     * Close all the db resources
     */
    private void closeResult(ResultSet result) throws SQLException {
        if (result != null) {
            Statement statement = result.getStatement();
            Connection connection = statement.getConnection();
            result.close();
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        }
//      System.err.println("*** Closing db connection: " + STOREID + "-" + CONNID);
    }


    /*
     * Extract and store the properties from the specified node description
     * Updated to store each property into a column of the properties table
     *      for IVOA standard properties.
     * Also, for a LinkNode, stores the link target in the links table.
     * @param nodeAsString String representation of node whose properties are to be stored
     */
    private void storeProperties(String nodeAsString) throws SQLException, VOSpaceException {
        Node node = NodeFactory.getInstance().getNode(nodeAsString);
        String identifier = fixId(node.getUri());
        if (node instanceof LinkNode) {
            String lquery = "insert into links (identifier, target) values ('" +
                            identifier + "', '" + ((LinkNode) node).getTarget() + "')";
            update(lquery);
        }
        HashMap<String, String> properties = node.getProperties();
        // Make sure the public read properties match at all times.
        String isPub = properties.get(Props.getURI(Props.ISPUBLIC));
        String pubRd = properties.get(Props.getURI(Props.PUBLICREAD));
        if (isPub != "" || pubRd != "") {
            String nodeIsPub = Boolean.toString(Boolean.parseBoolean(isPub) || Boolean.parseBoolean(pubRd));
            node.setProperty(Props.getURI(Props.ISPUBLIC), nodeIsPub);
            node.setProperty(Props.getURI(Props.PUBLICREAD), nodeIsPub);
        }
        ArrayList<String> columns = new ArrayList<String>();
        ArrayList<String> values = new ArrayList<String>();
        columns.add("identifier");
        values.add(identifier);
        for (Map.Entry<String, String> prop : properties.entrySet()) {
            String property = prop.getKey();
            String shortProp = Props.fromURI(property);
            if (shortProp != null) {
                if (!shortProp.equals("identifier")) {
                    columns.add(shortProp);
                    values.add(prop.getValue());
                }
            } else {
                String addquery = "insert into addl_props (identifier, property, value) values ('" + identifier + "', '" + property + "', '" + prop.getValue() + "')";
                update(addquery);
            }
        }
        String query = "insert into properties (" + StringUtils.join(columns, ",")
                    + ") values ('" + StringUtils.join(values, "','") + "')";
        update(query);
        /*      for (Map.Entry<String, String> prop : properties.entrySet()) {
            String query = "insert into properties (identifier, property, value) values ('" + identifier + "', '" + prop.getKey() + "', '" + prop.getValue() + "')";
            statement.executeUpdate(query);
        } */
        /*      NodeType node = NodeType.Factory.parse(nodeAsString);
        String identifier = node.getUri();
        PropertyListType properties = node.getProperties();
        for (PropertyType property : properties.getPropertyArray()) {
            String query = "insert into properties (identifier, property, value) values ('" + identifier + "', '" + property.getUri() + "', '" + property.getStringValue() + "')";
            statement.executeUpdate(query);
        } */
    }


    /*
     * Extract and store the properties from the specified node description
     * Updated to store each property into a column of the properties table
     *      for IVOA standard properties.
     * Also, for a LinkNode, stores the link target in the links table.
     * @param nodeAsString String representation of node whose properties are to be stored
     * @return string representation of node with updated properties (deleted where specified)
     */
    private String updateProperties(String nodeAsString) throws SQLException, VOSpaceException {
        Node node = NodeFactory.getInstance().getNode(nodeAsString);
        String identifier = fixId(node.getUri());
        if (node instanceof LinkNode) {
            String lquery = "update links set target = '" + ((LinkNode) node).getTarget() +
                            "' where identifier = '" + identifier + "'";
            update(lquery);
        }
        HashMap<String, String> properties = node.getProperties();
        ArrayList<String> updates = new ArrayList<String>();
        if (!properties.isEmpty()) {
            // Make sure the public read properties match at all times.
            String isPub = properties.get(Props.getURI(Props.ISPUBLIC));
            String pubRd = properties.get(Props.getURI(Props.PUBLICREAD));
            if (isPub != "" || pubRd != "") {
                String nodeIsPub = Boolean.toString(Boolean.parseBoolean(isPub) || Boolean.parseBoolean(pubRd));
                node.setProperty(Props.getURI(Props.ISPUBLIC), nodeIsPub);
                node.setProperty(Props.getURI(Props.PUBLICREAD), nodeIsPub);
            }
            for (Map.Entry<String, String> prop : properties.entrySet()) {
                String property = prop.getKey();
                String shortProp = Props.fromURI(property);
                if (shortProp != null) {
                    if (!shortProp.equals("identifier")) {
                        updates.add(shortProp + " = '" + prop.getValue() + "'");
                    }
                } else {
                    if (getPropertyValue(identifier, property) != null) {
                        String addquery = "update addl_props set value = '" + prop.getValue() + "' where identifier = '"
                                        + identifier + "' and property = '" + property + "'";
                        update(addquery);
                    } else {
                        String addquery = "insert into addl_props (identifier, property, value) values ('" + identifier + "', '" + property + "', '" + prop.getValue() + "')";
                        update(addquery);
                    }
                }
            }
        }
        /* for (Map.Entry<String, String> prop : properties.entrySet()) {
            String query = "update properties set value = '" + prop.getValue() + "' where identifier = '" + identifier + "' and property = '" + prop.getKey() + "'";
            statement.executeUpdate(query);
        } */
        // Check for deleted properties
        String[] nilSet = node.get("/vos:node/vos:properties/vos:property[@xsi:nil = 'true']/@uri");
        if (nilSet.length > 0) {
            for (String delProp : nilSet) {
                String shortProp = Props.fromURI(delProp);
                if (shortProp != null) {
                    updates.add(shortProp + " = NULL");
                } else {
                    String addquery = "delete from addl_props where identifier = '" + identifier + "' and property = '" + delProp + "'";
                    update(addquery);
                }
            }
        }
        /* for (String delProp : nilSet) {
            String query = "delete from properties where identifier = '" + identifier + "' and property = '" + delProp + "'";
            statement.executeUpdate(query);
        } */
        if (!updates.isEmpty()) {
            String query = "update properties set " + StringUtils.join(updates, ", ") + " where identifier = '" + identifier + "'";
            update(query);
        }
        node.remove("/vos:node/vos:properties/vos:property[@xsi:nil = 'true']");
        return node.toString();
    }


    /**
     * Store a result associated with a Job
     * @param identifier The job identifier
     * @param result The result associated with the job
     */
    public void addResult(String identifier, String result) throws SQLException {
        String query = "insert into results (identifier, details) values ('" + identifier + "', '" + result + "')";
        update(query);
    }


    /**
     * Get a result associated with a Job
     * @param identifier The job identifier
     * @return result The result associated with the job
     */
    public String getResult(String identifier) throws SQLException {
        String details = null;
        String query = "select details from results where identifier = '" + identifier + "'";
        details = getAsString(query);
        return details;
    }

    /**
     * Check whether transfer associated with a Job exists
     */
    public boolean isTransfer(String identifier) throws SQLException {
        boolean transfer = false;
        String query = "select identifier from transfers where jobid = '" + identifier + "'";
        transfer = extantEntry(query);
        return transfer;
    }

    /**
     * Check whether transfer associated with a Job exists
     */
    public boolean isTransferByEndpoint(String endpoint) throws SQLException {
        boolean transfer = false;
        String query = "select identifier from transfers where endpoint like '%" + escapeStr(endpoint) + "'";
        if (getAsString(query) != null) transfer = true;
        return transfer;
    }

    /**
     * Get the last modification time of the node
     */
    public long getLastModTime(String identifier) throws SQLException {
        String query = "select lastModificationDate from nodes where identifier = '" + fixId(identifier) + "'";
        long lastMod = getAsTime(query);
        return lastMod;
    }
}
