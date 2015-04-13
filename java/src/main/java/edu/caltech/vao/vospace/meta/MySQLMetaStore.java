/**
 * MySQLMetaStore.java
 * Author: Matthew Graham (Caltech)
 * Version: Original (0.1) - 23 August 2006
 */

package edu.caltech.vao.vospace.meta;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;

import edu.caltech.vao.vospace.xml.Node;

/**
 * This class represents a metadata store for VOSpace based on the MySQL
 * open source database
 */
public class MySQLMetaStore implements MetaStore{

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
    private Connection connection;

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
	    // Get connection
	    connection = getConnection();
        } catch (Exception e) {	
	    e.printStackTrace();
	    //	    log.error(e.getMessage());
	}
    }

   /* 
    * Get a db connection
    */
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) { 
	    connection = DriverManager.getConnection("jdbc:apache:commons:dbcp:connPool");
	} 
	return connection;
    }

    /* 
     * Get the job with the specified identifier
     * @param jobID The ID of the job to get
     * @return The requested job or <i>null</i> if there is no job with the given ID.
     */
    public String getJob(String jobID) throws SQLException {
	String query = "select job from jobs where identifier = '" + jobID + "'";
	ResultSet result = execute(query);
	if (result.next()) {
	    return result.getString(1);
	} else {
	    return null;
	}
    }

    /* 
     * Add the job with the specified identifier
     * @param jobID The ID of the job to get
     * @param job The XML string representation of the job
     */
    public void addJob(String jobID, String job) throws SQLException {
        Statement statement = getConnection().createStatement();
	String query = "insert into jobs (identifier, job) values ('" + jobID + "', '" + job + "')";
	statement.executeUpdate(query);
    }

   /*
     * Check whether the object with the specified identifier is in the store
     * @param identifier The ID of the node to check
     * @return whether the node is stored or not
     */
    public boolean isStored(String identifier) throws SQLException {
        String query = "select identifier from nodes where identifier = '" + identifier + "'";
	ResultSet result = execute(query);
	if (result.next()) {
	    return true;
	} else {
	    return false;
	}	
    }

    /*
     * Get the type of the object with the specified identifier
     * @param identifier The ID of the node 
     * @return the type of the node
     */
    public int getType(String identifier) throws SQLException{
	int type = -1;
        String query = "select type from nodes where identifier = '" + identifier + "'";
	try {
	    ResultSet result = execute(query);
	    if (result.next()) type = result.getInt(1);
	} catch (SQLException e) {}
	return type;
    }

    /*
     * Check whether the specified property is known to the service
     * @param identifier The ID of the property
     * @return whether the property is known
     */
    public boolean isKnownProperty(String identifier) throws SQLException{
	boolean known = false;
        String query = "select * from metaproperties where identifier = '" + identifier + "'";
	ResultSet result = execute(query);
	if (result.next())
	    known = true;
	return known;
    }

    /*
     * Register the specified property
     * @param property The property identifier
     * @param type The property type
     * @param readOnly Is the property read-only?
     */
    public void registerProperty(String property, int type, boolean readOnly) throws SQLException {
        Statement statement = getConnection().createStatement();
	int wp = 0;
	if (readOnly) wp = 1;
	String query = "insert into metaproperties (identifier, type, readOnly) values ('" + property + "', " + type + ", " + wp + ")";
	statement.executeUpdate(query);
    }





    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, Object metadata) throws SQLException {
        Statement statement = getConnection().createStatement();
	if (metadata instanceof String) {
	    String query = "insert into nodes (identifier, type, creationDate, node) values ('" + identifier + "', '" + type + "', cast(now() as datetime), '" + (String) metadata + "')"; 
	    statement.executeUpdate(query);
	    storeProperties((String) metadata);
	}
    }

    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, String owner, Object metadata) throws SQLException {
        Statement statement = getConnection().createStatement();
	if (metadata instanceof String) {
	    String query = "insert into nodes (identifier, type, owner, creationDate, node) values ('" + identifier + "', '" + type + "', '" + owner + "', cast(now() as datetime), '" + (String) metadata + "')"; 
	    statement.executeUpdate(query);
	    storeProperties((String) metadata);
	}
    }

    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, String owner, String location, Object metadata) throws SQLException {
        Statement statement = getConnection().createStatement();
	if (metadata instanceof String) {
	    String query = "insert into nodes (identifier, type, owner, location, creationDate, node) values ('" + identifier + "', '" + type + "', '" + owner + "', '" + location + "', cast(now() as datetime), '" + (String) metadata + "')"; 
	    statement.executeUpdate(query);
	    storeProperties((String) metadata);
	}
    }

    /*
     * Store the metadata for the specified identifier
     */
    public void storeData(String identifier, int type, String view, String owner, String location, Object metadata) throws SQLException {
        Statement statement = getConnection().createStatement();
	if (metadata instanceof String) {
	    String query = "insert into nodes (identifier, type, view, owner, location, creationDate, node) values ('" + identifier + "', '" + type + "', '" + view + "', '" + owner + "', '" + location + "', cast(now() as datetime), '" + (String) metadata + "')"; 
	    statement.executeUpdate(query);
	    storeProperties((String) metadata);
	}
    }



    public String checkData(String[] identifiers, int limit) throws SQLException {
	Statement statement = getConnection().createStatement();
	String whereQuery = null, token = null;
	// Get count
	for (int i = 0; i < identifiers.length; i++) {
	    if (i == 0) whereQuery = "where";
	    if (identifiers[i].contains("*")) {
	        whereQuery += " identifier like '" + identifiers[i].replace("*", "%") + "'";
	    } else {
	        whereQuery += " identifier = '" + identifiers[i] + "'";
            }
	    if (i != identifiers.length - 1) whereQuery += " or ";
	}
	String query = "select count(identifier) from nodes " + whereQuery;
	ResultSet countResult = execute(query);
	countResult.next();
	int count = countResult.getInt(1); 
	if (limit < count) {
	    token = UUID.randomUUID().toString();
	    String createToken = "insert into listings (token, offset, count, whereQuery) values ('" + token + "', " + 0 + ", " + count + ", '" + whereQuery.replace("'", "\\'") + "')";
	    statement.executeUpdate(createToken);
	}
	return token;
    }
   
    public boolean getAllData(String token, int limit) throws SQLException {
	String query = "select offset, count from listings where token = '" + token + "'";
	ResultSet result = execute(query);
	result.next();
	int offset = result.getInt(1);
	int count = result.getInt(2);
	if (offset + limit >= count) {
	    return true;
	} else {
	    Statement statement = getConnection().createStatement();
            String updateOffset = "update listings set offset = " + (offset + limit) + " where token ='" + token + "'";
            statement.executeUpdate(updateOffset);
	    return false;
	}
    }

    /*
     * Retrieve the metadata for the specified identifier at the specified
     * level of detail
     */
    public ResultSet getData(String[] identifiers, String token, int limit) throws SQLException {
        Statement statement = getConnection().createStatement();
	String query = null, whereQuery = null;
	int count = 0, offset = 0;
	// Get count
	for (int i = 0; i < identifiers.length; i++) {
	    if (i == 0) whereQuery = "where";
	    if (identifiers[i].contains("*")) {
	        whereQuery += " identifier like '" + identifiers[i].replace("*", "%") + "'";
	    } else {
	        whereQuery += " identifier = '" + identifiers[i] + "'";
            }
	    if (i != identifiers.length - 1) whereQuery += " or ";
	}
	if (token != null) {
            String tokenQuery = "select offset, count, updateDate, whereQuery from listings where token = '" + token + "'";
            ResultSet tokenResult = execute(tokenQuery);
            if (tokenResult.next()) {
	        offset = tokenResult.getInt(1);	
		count = tokenResult.getInt(2);
		whereQuery = tokenResult.getString(4);
            } else {
                throw new SQLException("Invalid token");
            } 
        }
        // Construct listing query
	query = "select node from nodes ";
//	query += whereQuery + " order by identifier ";
	query += whereQuery + " order by type ";
	if (limit > 0) query += " limit " + limit;
	if (offset > 0) query += " offset " + offset;
	System.err.println(query);
        ResultSet result = execute(query);	
	return result;
    }

    /*
     *     Get the specified node
     */
    public String getNode(String identifier) throws SQLException {
	String query = "select node from nodes where identifier = '" + identifier + "'";
	ResultSet result = execute(query);
	result.next();
	String node = result.getString(1);
	return node;
    }


    /*
     * Get the direct children of the specified container node
     */
    public String[] getChildren(String identifier) throws SQLException {
	ArrayList<String> children = new ArrayList<String>();
	String query = "select identifier from nodes where identifier like '" + identifier + "%'";
	ResultSet result = execute(query);
	while (result.next()) {
	    String child = result.getString(1);
	    if (!child.equals(identifier) && !child.substring(identifier.length() + 1).contains("/")) {
		children.add(child);
	    }
	}
	return children.toArray(new String[0]);
    }


     /*
     * Get the direct children nodes of the specified container node
     */
    public String[] getChildrenNodes(String identifier) throws SQLException {
	ArrayList<String> children = new ArrayList<String>();
	String query = "select node from nodes where identifier like '" + identifier + "/%' and identifier not like '" + identifier + "/%/%'";
	ResultSet result = execute(query);
	while (result.next()) {
	    String child = result.getString(1);
	    //	    if (!child.equals(identifier) && !child.substring(identifier.length() + 1).contains("/")) {
	    children.add(child);
	    //}
	}
	return children.toArray(new String[0]);
    }

    
    /*
     * Get all the children of the specified container node
     */
    public String[] getAllChildren(String identifier) throws SQLException {
	ArrayList<String> children = new ArrayList<String>();
	String query = "select identifier from nodes where identifier like '" + identifier + "%'";
	ResultSet result = execute(query);
	while (result.next()) {
	    String child = result.getString(1);
	    if (!child.equals(identifier)) {
		children.add(child);
	    }
	}
	return children.toArray(new String[0]);
    }

    
    /*
     * Remove the metadata for the specified identifier
     * @param identifier The (root) identifier of the node(s) to delete
     */
    public void removeData(String identifier) throws SQLException {
        String query = "delete from nodes where identifier like '" + identifier + "%'";
	Statement statement = getConnection().createStatement();
	statement.executeUpdate(query);
    }

    /*
     * Update the metadata for the specified identifier
     */
    public void updateData(String identifier, Object metadata) throws SQLException {
	Statement statement = getConnection().createStatement();
	if (metadata instanceof String) {
	    String node = updateProperties((String) metadata);
	    String encode = node.replace("\"", "'");
	    String query = "update nodes set node = \"" + encode + "\" where identifier = '" + identifier + "'";
	    statement.executeUpdate(query);
	}
    }

    /*
     * Update the metadata for the specified identifier including updating the
     * identifier
     */
    public void updateData(String identifier, String newIdentifier, Object metadata) throws SQLException {
        Statement statement = getConnection().createStatement();
	if (metadata instanceof String) {
	    String node = updateProperties((String) metadata);
	    String query = "update nodes set identifier = '" + newIdentifier + "', node = '" + node + "' where identifier = '" + identifier + "'"; 
	    statement.executeUpdate(query);
	}
    }

    /*
     * Update the metadata for the specified identifier including updating the
     * identifier and the location
     */
    public void updateData(String identifier, String newIdentifier, String newLocation, Object metadata) throws SQLException {
        Statement statement = getConnection().createStatement();
	if (metadata instanceof String) {
	    String node = updateProperties((String) metadata);
	    String query = "update nodes set identifier = '" + newIdentifier + "', location = '" + newLocation + "', node = '" + node + "' where identifier = '" + identifier + "'"; 
	    statement.executeUpdate(query);
	}
    }

    /*
     * Get a token
     */
    public String getToken(String[] identifiers) throws SQLException {
	Statement statement = getConnection().createStatement();
	String whereQuery = null, query = null;
	for (int i = 0; i < identifiers.length; i++) {
	    if (i == 0) whereQuery += "where";
	    if (identifiers[i].contains("*")) {
	        whereQuery += " identifier like '" + identifiers[i].replace("*", "%") + "'";
	    } else {
	        whereQuery += " identifier = '" + identifiers[i] + "'";
            }
	    if (i != identifiers.length - 1) query += " or ";
	}
	query = "select token from listings where whereQuery = '" + whereQuery.replace("'", "\\'") + "'";
	ResultSet tokenResult = execute(query);
	tokenResult.next();
	return tokenResult.getString(1);
    }

    /*
     * Get the physical location of the specified identifier
     */
    public String getLocation(String identifier) throws SQLException {
	String location = null;
        String query = "select location from nodes where identifier = '" + identifier + "'";
	ResultSet result = execute(query);
	result.next();
	location = result.getString(1);
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
	    query = "update nodes set status = 1 where identifier = '" + identifier + "'";
	} else {
	    query = "update nodes set status = 0 where identifier = '" + identifier + "'";
        }
	Statement statement = getConnection().createStatement();
	statement.executeUpdate(query);
    }


    /*
     * Store the details of the specified transfer
     */
    public void storeTransfer(String identifier, String endpoint) throws SQLException {
        Statement statement = getConnection().createStatement();
	String query = "insert into transfers (jobid, endpoint, created) values ('" + identifier + "', '" + endpoint + "', cast(now() as datetime))";
	statement.executeUpdate(query);
    }

    /*
     * Retrieve the job associated with the specified endpoint
     */
    public ResultSet getTransfer(String endpoint) throws SQLException {
	String job = null;
	String query = "select details, t.completed from results j, transfers t where t.jobid = j.identifier and t.endpoint like '%" + endpoint + "'";
	ResultSet result = execute(query);
	return result;
    }

    /**
     * Check whether the specified transfer has completed
     */
    public boolean isCompleted(String jobid) throws SQLException {
	boolean completed = false;
	String query = "select completed from transfers where jobid = '" + jobid + "'";
	ResultSet result = execute(query);
	if (result.next()) 
	    if (result.getDate(1) != null) completed = true;
	return completed;
    }


    /*
     * Store the original view of the specific object
     */
    public void setView(String identifier, String view) throws SQLException {
	Statement statement = getConnection().createStatement();
	String query = "update nodes set view = '" + view + "' where identifier = '" + identifier + "'";
	statement.executeUpdate(query);
    }

    /*
     * Get the original view of the object with the specified identifier
     */
    public String getView(String identifier) throws SQLException {
	String view = null;
        String query = "select view from nodes where identifier = '" + identifier + "'";
	ResultSet result = execute(query);
	result.next();
	view = result.getString(1);
	return view;
    }

    /*
     * Resolve a location from the specified endpoint
     */
    public String resolveLocation(String endpoint) throws SQLException {
	String location = null;
//	String query = "select n.location from nodes n, transfers t where n.identifier = t.identifier and locate('" + endpoint + "', t.endpoint) > 0 and timestampdiff(minute, t.created, now()) < 60 and completed is null";
	String query = "select location from transfers where locate('" + endpoint + "', endpoint) > 0 and timestampdiff(minute, created, now()) < 60 and completed is null";
	ResultSet result = execute(query);
	if (result.next()) location = result.getString(1);
	return location;
    }

    /*
     * Return the identifier associated with the specified location
     */
    public String resolveIdentifier(String location) throws SQLException {
	String identifier = null;
	String query = "select identifier from nodes where location like '" + location + "'";
	ResultSet result = execute(query);
	if (result.next()) identifier = result.getString(1);
	return identifier;
    }

    /*
     * Mark the specified transfer as complete
     */
    public void completeTransfer(String endpoint) throws SQLException {
	Statement statement = getConnection().createStatement();
	String query = "update transfers set completed = cast(now() as datetime) where endpoint like '%" +  endpoint + "'";
	statement.executeUpdate(query);
    }

    /*
     * Mark the specified transfer as complete
     */
    public void completeTransfer(String endpoint, boolean updateStatus) throws SQLException {
	/*	Statement statement = getConnection().createStatement();
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
	ResultSet result = execute(query);
	if (result.next()) identifier = result.getString(1);
	return identifier;
    }


    /*
     * Update the specified property
     */   
    public void updateProperty(String property, int type) throws SQLException {
	Statement statement = getConnection().createStatement();
	String query = "update metaproperties set type = " + type + " where identifier = '" + property + "'";
	statement.executeUpdate(query);
    }

    /*
     * Get the properties of the specified type
     */
    public ResultSet getProperties(int type) throws SQLException {
	String query = "select identifier, readOnly from metaproperties where type & "  + type + " = " + type;
	ResultSet result = execute(query);
	return result;
    }

    /*
     * Get the property type of the specified node
     */
    public String getPropertyType(String identifier) throws SQLException {
	String type = null;
	String query = "select type from metaproperties where identifier = '" + identifier + "'";
	ResultSet result = execute(query);
	if (result.next()) type = result.getString(1);
	return type;
    }
    
    /*
     * Check whether the property is read/only
     */
    public boolean isReadOnly(String property) throws SQLException {
	boolean readOnly = false;
	String query = "select readonly from metaproperties where identifier = '" + property + "'";
	ResultSet result = execute(query);
	if (result.next()) readOnly = result.getBoolean(1);
	return readOnly;
    }

    /*
     * Check the status of a capability (active or not)
     */
    public boolean isActive(String identifier, String capability) throws SQLException {
	boolean isActive = false;
	String query = "select active from capabilities where identifier = '" + identifier + "' and capability = '" + capability + "'";
	ResultSet result = execute(query);
	if (result.next()) isActive = result.getBoolean(1);
	return isActive;
    }


    /*
     * Set the status of a capability (active or not)
     */
    public void setActive(String identifier, String capability) throws SQLException {
	String query = "update capabilities set active = 1 where identifier = '" + identifier + "' and capability = '" + capability + "'";
	ResultSet result = execute(query);
    }


    /*
     * Register the capabilities
     */
    public void registerCapability(String identifier, String capability) throws SQLException {
	Statement statement = getConnection().createStatement();
	String query = "insert capabilities values('" + identifier + "', '" + capability + "', 0)";
	statement.executeUpdate(query);
    }

    
    /*
     * Execute a query on the store
     */
    private ResultSet execute(String query) throws SQLException {
	ResultSet result = null;
	Statement statement = getConnection().createStatement();
	boolean success = statement.execute(query);
	if (success) {
	    result = statement.getResultSet();
	}
	return result;
    }
 
    /*
     * Extract and store the properties from the specified node description
     */
    private void storeProperties(String nodeAsString) throws SQLException {
        Statement statement = getConnection().createStatement();
	try {
	    Node node = new Node(nodeAsString.getBytes());
	    String identifier = node.getUri();
	    HashMap<String, String> properties = node.getProperties();
	    for (Map.Entry<String, String> prop : properties.entrySet()) {
		String query = "insert into properties (identifier, property, value) values ('" + identifier + "', '" + prop.getKey() + "', '" + prop.getValue() + "')"; 
	        statement.executeUpdate(query);
	    }
 	    /*	    NodeType node = NodeType.Factory.parse(nodeAsString);
	    String identifier = node.getUri();
	    PropertyListType properties = node.getProperties();
	    for (PropertyType property : properties.getPropertyArray()) {
	        String query = "insert into properties (identifier, property, value) values ('" + identifier + "', '" + property.getUri() + "', '" + property.getStringValue() + "')"; 
	        statement.executeUpdate(query);
		} */
	} catch (Exception e) {}
    }


    /*
     * Extract and store the properties from the specified node description
     * @param nodeAsString String representation of node whose properties are to be stored
     * @return string representation of node with updated properties (deleted where specified) 
     */
    private String updateProperties(String nodeAsString) throws SQLException {
	Node node = null;
        Statement statement = getConnection().createStatement();
	try {
	    node = new Node(nodeAsString.getBytes());
	    String identifier = node.getUri();
	    HashMap<String, String> properties = node.getProperties();
	    for (Map.Entry<String, String> prop : properties.entrySet()) {
		String query = "update properties set value = '" + prop.getValue() + "' where identifier = '" + identifier + "' and property = '" + prop.getKey() + "'";
	        statement.executeUpdate(query);
	    }
	    // Check for deleted properties
	    String[] nilSet = node.get("/vos:node/vos:properties/vos:property[@xsi:nil = 'true']/@uri");
	    for (String delProp : nilSet) {
		String query = "delete from properties where identifier = '" + identifier + "' and property = '" + delProp + "'";
		statement.executeUpdate(query);
	     }
	    node.remove("/vos:node/vos:properties/vos:property[@xsi:nil = 'true']");
	} catch (Exception e) {}
	return node.toString();
    }

    
    /**
     * Store a result associated with a Job
     * @param identifier The job identifier
     * @param result The result associated with the job
     */
    public void addResult(String identifier, String result) throws SQLException {
	Statement statement = getConnection().createStatement();
	String query = "insert into results (identifier, details) values ('" + identifier + "', '" + result + "')";
	statement.executeUpdate(query);
    }

    
    /**
     * Get a result associated with a Job
     * @param identifier The job identifier
     * @return result The result associated with the job
     */
    public String getResult(String identifier) throws SQLException {
	String details = null;
	Statement statement = getConnection().createStatement();
	String query = "select details from results where identifier = '" + identifier + "'";
	ResultSet result = execute(query);
	if (result.next()) details = result.getString(1);
	return details;
    }


    /**
     * Check whether transfer associated with a Job exists
     */
    public boolean isTransfer(String identifier) throws SQLException {
        String query = "select identifier from transfers where jobid = '" + identifier + "'";
	ResultSet result = execute(query);
	if (result.next()) {
	    return true;
	} else {
	    return false;
	}	
    }
}
