/**
 * MySQLMetaStore.java
 * Author: Matthew Graham (Caltech)
 * Version: Original (0.1) - 23 August 2006
 */

package edu.caltech.vao.vospace.meta;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import ca.nrc.cadc.vos.NodeProperty;
import ca.nrc.cadc.vos.VOSURI;
import org.apache.commons.dbcp2.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import edu.caltech.vao.vospace.xml.Node;
import edu.caltech.vao.vospace.xml.NodeFactory;
import edu.caltech.vao.vospace.xml.LinkNode;
import edu.caltech.vao.vospace.xml.DataNode;
import edu.caltech.vao.vospace.NodeType;
import edu.caltech.vao.vospace.Props;
import edu.caltech.vao.vospace.VOSpaceException;
import edu.caltech.vao.vospace.VOSpaceManager;

import static edu.noirlab.datalab.vos.Utils.*;


/**
 * This class represents a metadata store for VOSpace based on the MySQL
 * open source database
 */
public class MySQLMetaStore implements MetaStore {
    private static final String DEFAULT_DB_URL = "localhost/vospace";
    private static final String DEFAULT_DB_UID = "dba";
    private static final String DEFAULT_DB_PWD = "dba";
    private PoolingDataSource<PoolableConnection> dataSource;
    private static Logger logger = Logger.getLogger(MySQLMetaStore.class.getName());
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

            // Get connection factory
            ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectionURL, null);
            PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,
                    null);

            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(100);
            config.setMaxIdle(5);
            config.setMinIdle(5);
            //PoolingDataSource expect an ObjectPool
            ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory, config);
            // Set the poolableConnectionFactory's pool property to the owning pool
            poolableConnectionFactory.setPool(connectionPool);

            this.dataSource = new PoolingDataSource<>(connectionPool);

        } catch (Exception e) {
            log_error(logger, e);
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
        return dataSource.getConnection();
        //Connection connection = DriverManager.getConnection("jdbc:apache:commons:dbcp:connPool");
//      System.err.println("Getting connection: " + STOREID + "-" + ++CONNID);
        //return connection;
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
     * Get the owner from the identifier ID string
     * @param identifier The Id of the node
     * @return the owner of the node
     */
    protected String getOwnerFromId(String identifier) throws VOSpaceException {
        int rootNodeLength = VOSpaceManager.getInstance().getRootNodeLength();
        String idPath = identifier.substring(rootNodeLength);
        String[] idTokens = idPath.split("\\/");
        if (idTokens.length > 0) {
            String owner = idTokens[1];
            return owner;
        } else {
            throw new VOSpaceException(VOSpaceException.VOFault.InvalidURI,
                    "Can not find owner in identifier after removing root node." +
                            "[" + idPath + "]");
        }
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
            closeResult(result);
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

    /**
    *  getDataJDOM2 is the JDOM2 version of the above getData, with the caveat
     *  that returns a list of object Nodes as opposed to XML strings.
     *  We found that doing the Object to XML serialization at the very end
     *  is more efficient.
    */
    public ca.nrc.cadc.vos.Node[] getDataJDOM2(String[] identifiers, String token, int limit) throws SQLException, VOSpaceException {
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
        query = "select identifier, type from nodes ";
        query += whereQuery + " order by type ";
        if (limit > 0) query += " limit " + limit;
        if (offset > 0) query += " offset " + offset;
        ResultSet result = null;
        ArrayList<ca.nrc.cadc.vos.Node> nodes = new ArrayList<ca.nrc.cadc.vos.Node>();
        try {
            result = execute(query);
            while (result.next()) {
                String nodeId = result.getString(1);
                int nodeType = result.getInt(2);
                nodes.add(createNodeJDOM2(nodeId, nodeType));
            }
        } finally {
            closeResult(result);
        }
        return nodes.toArray(new ca.nrc.cadc.vos.Node[0]);
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
     * Create the specified node object by combining the other data stored in the database
     * This node object can later be serialized to XML in one swap.
     */
    private ca.nrc.cadc.vos.Node createNodeJDOM2(String identifier, int type) throws SQLException, VOSpaceException {
        // Create a new Node object of the proper type
        String fixedId = fixId(identifier);
        //Node node = NodeFactory.getInstance().getNodeByType(NodeType.getUriById(type));
        ca.nrc.cadc.vos.Node node = null;
        try {
            node = NodeFactory.getInstance().getJDOM2NodeByType(type, new VOSURI(fixedId));
            // Get the Properties for the node, and set them in the Node object
            String[] propNames = Props.allProps();
            // First build a query of all column names to get all column values
            String query = "select " + StringUtils.join(propNames, ",") + " from properties where identifier = '" + fixedId + "'";
            // Execute the query and set the property values in the Node.
            ResultSet result = null;
            List<NodeProperty> properties = new ArrayList<>();
            try {
                result = execute(query);
                if (result.next()) {
                    String value = null;
                    for (String name : propNames) {
                        value = result.getString(name);
                        if (value != null) {
                            NodeProperty np = new NodeProperty("ivo://ivoa.net/vospace/core#" + name, value);
                            properties.add(np);
                        }
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
                    if (value != null) {
                        NodeProperty np = new NodeProperty("ivo://ivoa.net/vospace/core#" + property, value);
                        properties.add(np);
                    }
                }
            } finally {
                closeResult(result);
            }
            node.setProperties(properties);
            if (node instanceof ca.nrc.cadc.vos.LinkNode) {
                ((ca.nrc.cadc.vos.LinkNode) node).setTarget(new URI(getTarget(fixedId)));
            }
            // Set the Views and Capabilities; unfortunately requires the VOSpaceManager
            NodeType nodeType = MetaStore.getNodeType(type);
            if (viewsAndCaps.contains(nodeType)) {
                VOSpaceManager.getInstance().addViewsAndCapabilitiesJDOM2(node, nodeType);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new VOSpaceException(VOSpaceException.VOFault.InvalidURI, e.getLocalizedMessage());
        }
        // Return the Node
        return node;
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
    public String[] getChildren(String identifier) throws SQLException, VOSpaceException {
        String query = "select identifier from nodes force index (nod_own_dep_id_idx) where " +
                " owner = '" + getOwnerFromId(identifier) + "'" +
                " and depth = " + (getIdDepth(identifier) + 1)
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

        ca.nrc.cadc.vos.Node[] nodeArray = getChildrenNodesJDOM2(identifier);
        edu.noirlab.datalab.vos.VTDXMLNodeWriter instance = new edu.noirlab.datalab.vos.VTDXMLNodeWriter();
        ArrayList<String> nodeXMLArray = new ArrayList<String>();
        try {
            for (ca.nrc.cadc.vos.Node elem : nodeArray) {
                StringWriter sw = new StringWriter();
                instance.write(elem, sw, true);
                nodeXMLArray.add(sw.toString());
                sw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            throw new VOSpaceException(e);
        }

        return nodeXMLArray.toArray(new String[0]);
    }

    List<NodeType> viewsAndCaps = Arrays.asList(
            NodeType.DATA_NODE,
            NodeType.CONTAINER_NODE,
            NodeType.STRUCTURED_DATA_NODE,
            NodeType.UNSTRUCTURED_DATA_NODE);

    /*
     * Get the direct children nodes of the specified container node.
     * This is the alternative version to the original getChildrenNodes.
     * This new version, however, uses ca.nrd.cadc.vos.Node classes to
     * represent the Node object and it uses a subquery to join the Nodes,
     * properties and addl_properties table in one single SQL query.
     */
    public ca.nrc.cadc.vos.Node[] getChildrenNodesJDOM2(String identifier) throws SQLException, VOSpaceException {
        String nodesQuery= "select identifier, type from nodes force index (nod_own_dep_id_idx) where " +
                " owner = '" + getOwnerFromId(identifier) + "'" +
                " and depth = " + (getIdDepth(identifier) + 1) +
                " and identifier like '" + escapeId(identifier) + "/%'";

        String addlQuery = "select a.property, a.value, a.identifier from addl_props as a";
        String[] propNames = Props.allProps();
        String queryProp = String.format("select t.identifier, t.type," +
                StringUtils.join(Arrays.stream(propNames).map(s->"p." +s).
                        collect(Collectors.toList()), ",") +
                ", addl.property, addl.value from properties as p, (%s) as t " +
                " LEFT JOIN (%s, (%s) as t where a.identifier = t.identifier) as addl " +
                " on addl.identifier = t.identifier " +
                " where p.identifier = t.identifier", nodesQuery, addlQuery, nodesQuery);

        //format:
        // type,    node_uri  , busy,       groupread,
        // btime,   publicread, length,     ctime,
        // ispublic,mtime,      groupwrite, date
        ResultSet result = null;

        ArrayList<ca.nrc.cadc.vos.Node> nodeArray = new ArrayList<ca.nrc.cadc.vos.Node>();
        try {
            result = execute(queryProp);
            String[] formatArgs = new String[4];
            while (result.next()) {
                String childId = result.getString(1);
                int childTypeId = result.getInt(2);
                String fixedChildId = fixId(childId);
                formatArgs[0] = fixedChildId;
                ca.nrc.cadc.vos.Node nodeJDOM2 = NodeFactory.getInstance().getJDOM2NodeByType(childTypeId, new VOSURI(childId));
                String value = null;

                List<NodeProperty> properties = new ArrayList<>();
                for (String propertyName: propNames) {
                    value = result.getString("p." + propertyName);
                    //delete below line
                    if (value != null) {
                        NodeProperty np = new NodeProperty("ivo://ivoa.net/vospace/core#" + propertyName, value);
                        properties.add(np);
                    }
                }

                //additional properties
                String propertyName = result.getString("addl.property");
                value = result.getString("addl.value");
                if (propertyName !=null && value != null) {
                    properties.add(new NodeProperty("ivo://ivoa.net/vospace/core#"+propertyName, value));
                }
                nodeJDOM2.setProperties(properties);

                NodeType childType = MetaStore.getNodeType(childTypeId);
                if (NodeType.LINK_NODE == MetaStore.getNodeType(childTypeId)) {
                    String target = getTarget(fixedChildId);
                    URI targetURI = new URI("null");
                    if (target != null) {
                       targetURI = new URI(target);
                    } else {
                        logger.warn("LinkFoundFault: no target for [" + childTypeId + "]");
                    }
                    ((ca.nrc.cadc.vos.LinkNode) nodeJDOM2).setTarget(targetURI);
                }

                // Set the Views and Capabilities; unfortunately requires the VOSpaceManager
                if ( viewsAndCaps.contains(childType)) {
                    VOSpaceManager.getInstance().addViewsAndCapabilitiesJDOM2(nodeJDOM2, childType);
                }

                nodeArray.add(nodeJDOM2);
            }
        } catch (URISyntaxException e) {
            log_error(logger, "getChildrenNodesJDOM2 identifier [" + identifier + "]",  e);
            throw new VOSpaceException(VOSpaceException.VOFault.InternalFault,
                    "Error processing container [" + identifier + "]");
        } catch (RuntimeException e) {
            log_error(logger, "getChildrenNodesJDOM2 identifier [" + identifier + "]",  e);
            throw new VOSpaceException(VOSpaceException.VOFault.InternalFault,
                    "Error processing container [" + identifier + "]");
        } catch (Exception e) {
            log_error(logger, "getChildrenNodesJDOM2 identifier [" + identifier + "]",  e);
            throw new VOSpaceException(VOSpaceException.VOFault.InternalFault,
                    "Error processing container [" + identifier + "]");
        } finally {
            closeResult(result);
        }

        return nodeArray.toArray(new ca.nrc.cadc.vos.Node[0]);
    }

    /*
     * Get all the children of the specified container node
     */
    public String[] getAllChildren(String identifier) throws SQLException, VOSpaceException {
        ArrayList<String> children = new ArrayList<String>();
        // Note: Don't filter by depth in the where clause as 
        // we want all nodes and not only the immediate ones.
        String query = "select identifier from nodes where " +
                " owner = '" + getOwnerFromId(identifier) + "'" +
                " and identifier like '" + escapeId(identifier) + "/%'";
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
    public String[] removeData(String identifier, boolean container) throws SQLException, VOSpaceException {
        String query = "";
        ArrayList<String> removedLinks = new ArrayList<String>();
        if (container) {
            String escaped = escapeId(identifier);
            // Note: Don't filter by depth in the where clause as 
            // we want all nodes and not only the immediate ones.
            query = "delete from nodes where " +
                    " owner = '" + getOwnerFromId(identifier) + "'" +
                    " and identifier like '" + escaped + "/%'";
            update(query);
            query = "delete from properties where identifier like '" + escaped + "/%'";
            update(query);
            query = "delete from addl_props where identifier like '" + escaped + "/%'";
            update(query);
            query = "delete from links where identifier like '" + escaped + "/%'";
            update(query);
            query = "delete from capabilities where identifier like '" + escaped + "/%'";
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
        query = "delete from capabilities where identifier = '" + identifier + "'";
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
            String query = "update nodes set lastModificationDate=cast(now() as datetime), identifier = '" + fixedNewId
                        + "', depth = " + getIdDepth(fixedNewId) + " where identifier = '" + fixedId + "'";
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
            String query = "update nodes set lastModificationDate=cast(now() as datetime), identifier = '" + fixedNewId
                        + "', depth = " + getIdDepth(fixedNewId) + ", location = '" + newLocation + "' where identifier = '" + fixedId + "'";
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
        String query = "insert into transfers (jobid, rendpoint, created) " +
                "values ('" + fixId(identifier) + "', " +
                "'" + new StringBuilder(endpoint).reverse() + "', " +
                " cast(now() as datetime))";
        update(query);
    }

    /*
     * Retrieve the job associated with the specified endpoint
     */
    public String getTransfer(String endpoint) throws SQLException {
        //String query = "select details from results j, transfers t where t.jobid = j.identifier and t.endpoint like '%" + escapeStr(endpoint) + "'";
        String query = "select details from results j, transfers t where" +
                " t.jobid = j.identifier " +
                "and t.rendpoint " +
                "like '" + new StringBuilder(escapeStr(endpoint)).reverse() + "%'";
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
        //String query = "select completed from transfers where endpoint like '%" + escapeStr(endpoint) + "'";
        String query = "select completed from transfers where rendpoint like '" +
                new StringBuilder(escapeStr(endpoint)).reverse() + "%'";
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
        // ISS - NOTE: I didn't change the endpoint to rev_endpoint in this SQL
        // as it looks like this functionality is not used by anyone.
        String query = "select location from transfers where locate('" + endpoint + "', endpoint) > 0 and timestampdiff(minute, created, now()) < 60 and completed is null";
        location = getAsString(query);
        return location;
    }

    /*
     * Return the creation date of a transfer
     */
    public long getCreated(String endpoint) throws SQLException {
        long created = 0;
        String query = "select created from transfers where rendpoint " +
                "like '" + new StringBuilder(escapeStr(endpoint)).reverse() + "%'";
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
        String query = "update transfers set completed = cast(now() as datetime) " +
                "where rendpoint like '" +
                new StringBuilder(escapeStr(endpoint)).reverse() + "%'";
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
        String query = "select identifier from transfers where rendpoint " +
                "like '" + new StringBuilder(endpoint).reverse() + "%'";
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
            ResultSet result = null;
            try {
                result = execute(query);
                result.next();
                for (int i = 0; i < columns.size(); i++) valMap.put(Props.getURI(columns.get(i)), result.getString(i+1));
            } finally {
                closeResult(result);
            }
        }
        if (!addl_props.isEmpty()) {
            String query = "select value from addl_props where identifier = '" + fixId(identifier)
                        + "' and property in ('" + StringUtils.join(addl_props, "','") + "')";
            ResultSet result = null;
            try {
                result = execute(query);
                result.next();
                for (int i = 0; i < addl_props.size(); i++) valMap.put(addl_props.get(i), result.getString(i+1));
            } finally {
                closeResult(result);
            }
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
        // System.err.println(query);
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        int retries = 5;
        while (retries > 0) {
            try {
                long t0 = System.currentTimeMillis();
                connection = getConnection();
                statement = connection.createStatement();
                boolean success = statement.execute(query);
                if (success) result = statement.getResultSet();
                retries = 0;
                long dt = System.currentTimeMillis() - t0;
                // We want to see what queries take more than .5 seconds
                if (logger.isDebugEnabled() || dt >= 500) {
                    String logMsg = query + "t:[" +dt + "ms ] r:[" + retries + "]";
                    if (dt >= 500) {
                        logger.warn(logMsg);
                    } else {
                        logger.debug(logMsg);
                    }

                    if (dt >= 500 || logger.getLevel() == Level.TRACE) {
                        String trace = getTrace(Thread.currentThread().getStackTrace());
                        if (dt >= 500) {
                            logger.warn(trace);
                        } else {
                            logger.debug(trace);
                        }
                    }
                }
            } catch (SQLException e) {
                log_error(logger, "query [" + query + "], retries [" + retries + "]",  e);
                String sqlState = e.getSQLState();
                if (retries > 0 && ("08S01".equals(sqlState) || "40001".equals(sqlState))) {
                    // System.out.println(sqlState + " " + e.toString());
                    retries--;
                } else {
                    throw e;
                }
            } finally {
                if ( result == null ) {
                    try { if (statement != null) { statement.close(); } } catch (SQLException e) {
                        log_error(logger, e);
                    }
                    try { if (connection != null) { connection.close(); } } catch (SQLException e) {
                        log_error(logger, e);
                    }
                }
            }
        }
        return result;
    }

    /*
     * Insert/update query on the store
     */
    private void update(String query) throws SQLException {
        // System.err.println(query);
        Connection connection = null;
        Statement statement = null;
        int retries = 5;
        while (retries > 0) {
            try {
                long t0 = System.currentTimeMillis();
                connection = getConnection();
                statement = connection.createStatement();
                statement.executeUpdate(query);
                retries = 0;
                long dt = System.currentTimeMillis() - t0;
                // We want to see what queries take more than .5 seconds
                if (logger.isDebugEnabled() || dt >= 500) {
                    String logMsg = query + " t:[" + dt + "ms ] r:[" + retries + "]";

                    if (dt >= 500) {
                        logger.warn(logMsg);
                    } else {
                        logger.debug(logMsg);
                    }

                    if (dt >= 500 || logger.getLevel() == Level.TRACE) {
                        String trace = getTrace(Thread.currentThread().getStackTrace());
                        if (dt >= 500) {
                            logger.warn(trace);
                        } else {
                            logger.debug(trace);
                        }
                    }
                }
            } catch (SQLException e) {
                log_error(logger, "query [" + query + "], retries [" + retries + "]", e);
                String sqlState = e.getSQLState();
                if (retries > 0 && ("08S01".equals(sqlState) || "40001".equals(sqlState))) {
                    // System.out.println(sqlState + " " + e.toString());
                    retries--;
                } else {
                    throw e;
                }
            } finally {
                try { if (statement != null) { statement.close(); }} catch (SQLException e) {
                    log_error(logger, e);
                }
                try { if (connection != null) {connection.close(); }} catch (SQLException e) {
                    log_error(logger, e);
                }
//              System.err.println("*** Closing db connection: " + STOREID + "-" + CONNID);
            }
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
            try { if (statement != null) statement.close(); } catch (SQLException e) {
                log_error(logger, e);
            }
            try { if (connection != null) connection.close(); } catch (SQLException e) {
                log_error(logger, e);
            }
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
        String isPub = properties.get(Props.ISPUBLIC_URI);
        String pubRd = properties.get(Props.PUBLICREAD_URI);
        if (isPub != "" || pubRd != "") {
            String nodeIsPub = Boolean.toString(Boolean.parseBoolean(isPub) || Boolean.parseBoolean(pubRd));
            node.setProperty(Props.ISPUBLIC_URI, nodeIsPub);
            node.setProperty(Props.PUBLICREAD_URI, nodeIsPub);
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
            String isPub = properties.get(Props.ISPUBLIC_URI);
            String pubRd = properties.get(Props.PUBLICREAD_URI);
            if (isPub != "" || pubRd != "") {
                String nodeIsPub = Boolean.toString(Boolean.parseBoolean(isPub) || Boolean.parseBoolean(pubRd));
                node.setProperty(Props.ISPUBLIC_URI, nodeIsPub);
                node.setProperty(Props.PUBLICREAD_URI, nodeIsPub);
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
        //String query = "select identifier from transfers where endpoint like '%" + escapeStr(endpoint) + "'";
        String query = "select identifier from transfers where rendpoint like '" +
                new StringBuilder(escapeStr(endpoint)).reverse() + "%'";
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
