
package edu.caltech.vao.vospace;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpStatus;

import org.codehaus.stax2.XMLStreamReader2;

import edu.caltech.vao.vospace.meta.*;
import edu.caltech.vao.vospace.capability.Capability;
import edu.caltech.vao.vospace.protocol.ProtocolHandler;
import edu.caltech.vao.vospace.storage.StorageManager;
import edu.caltech.vao.vospace.storage.StorageManagerFactory;
import edu.caltech.vao.vospace.view.TransformEngine;
import edu.caltech.vao.vospace.xml.*;
import edu.caltech.vao.vospace.VOSpaceException.VOFault;

public class VOSpaceManager {

    private final static String AUTO_NODE = ".auto";
    private final static String NULL_NODE = ".null";
    private final static String BASE = "file:///tmp";
    private final static String ROOT = "vos://";
    private final static boolean PRESERVE_DATA = false;
    private final static boolean OVERWRITE_DATA = true;
    private final static boolean STATUS_BUSY = true;
    private final static boolean STATUS_FREE = false;
    protected String BASE_URL = "http://localhost:8080/vospace";
    protected String ROOT_NODE = "vos://";
    protected final static int PROPERTIES_SPACE_ACCEPTS = 1;
    protected final static int PROPERTIES_SPACE_PROVIDES = 2;
    protected final static int PROPERTIES_SPACE_CONTAINS = 4;
    private final static String INHERITABLE_PROPERTY = "10";

    protected ArrayList<Views.View> SPACE_ACCEPTS_IMAGE;
    protected ArrayList<Views.View> SPACE_ACCEPTS_TABLE;
    protected ArrayList<Views.View> SPACE_ACCEPTS_ARCHIVE;
    protected ArrayList<Views.View> SPACE_ACCEPTS_OTHER;
    protected ArrayList<Views.View> SPACE_PROVIDES_IMAGE;
    protected ArrayList<Views.View> SPACE_PROVIDES_TABLE;
    protected ArrayList<Views.View> SPACE_PROVIDES_ARCHIVE;
    protected ArrayList<Views.View> SPACE_PROVIDES_OTHER;
    protected HashMap<String, Capability> CAPABILITIES;
    protected HashMap<String, Process> PROCESSES;

    protected ArrayList<Protocol> SPACE_CLIENT_PROTOCOLS;
    protected ArrayList<Protocol> SPACE_SERVER_PROTOCOLS;
    protected HashMap<String, ProtocolHandler> PROTOCOLS;

    protected String CAPABILITY_EXE;
    protected int CAPABILITY_PORT;
    protected String CAPABILITY_BASE;
    private String STAGING_LOCATION;
    private Pattern VOS_PATTERN;
    private Pattern TOKEN_PATTERN;
    private String SPACE_AUTH;
    private String AUTH_URL;
    private MetaStore store;
    private StorageManager backend;
    private TransformEngine engine;
    protected final String BASEURI;
    private String USER;
    boolean structure = false;
    private NodeFactory nfactory;

    private static VOSpaceManager ref;

    /* VOSpaceNodeManager
     */
    public static VOSpaceManager getInstance(String propFile) throws VOSpaceException{
        if (ref == null) ref = new VOSpaceManager(propFile);
        return ref;
    }

    public static VOSpaceManager getInstance() throws VOSpaceException {
        if (ref == null) throw new VOSpaceException(
                new NullPointerException("VOSpaceManager could not be initialized."));
        return ref;
    }

    private VOSpaceManager(String propFile) throws VOSpaceException {
        try {
            // Get property file
            Properties props = new Properties();
            props.load(new FileInputStream(propFile));
            // Set space properties
            ROOT_NODE = props.containsKey("space.rootnode") ? props.getProperty("space.rootnode") : ROOT;
            BASEURI = props.containsKey("space.baseuri") ? props.getProperty("space.baseuri") : BASE;
            STAGING_LOCATION = props.containsKey("space.staging_area") ? props.getProperty("space.staging_area") : BASE;
            structure = Boolean.parseBoolean(props.getProperty("space.supports.structure"));
            if (structure) engine = new TransformEngine(STAGING_LOCATION);
            String httpUrl = props.getProperty("server.http.url");
            BASE_URL = httpUrl.substring(0, httpUrl.lastIndexOf("/") + 1);
            AUTH_URL = props.containsKey("server.auth.url") ? props.getProperty("server.auth.url") : "";
            SPACE_ACCEPTS_IMAGE = getViewList(props.getProperty("space.accepts.image"));
            SPACE_ACCEPTS_TABLE = getViewList(props.getProperty("space.accepts.table"));
            SPACE_ACCEPTS_ARCHIVE = getViewList(props.getProperty("space.accepts.archive"));
            SPACE_ACCEPTS_OTHER = getViewList(props.getProperty("space.accepts.other"));
            SPACE_PROVIDES_IMAGE = getViewList(props.getProperty("space.provides.image"));
            SPACE_PROVIDES_TABLE = getViewList(props.getProperty("space.provides.table"));
            SPACE_PROVIDES_ARCHIVE = getViewList(props.getProperty("space.provides.archive"));
            SPACE_PROVIDES_OTHER = getViewList(props.getProperty("space.provides.other"));
            SPACE_AUTH = props.containsKey("space.identifier") ? getId(props.getProperty("space.identifier")) : "vos://";
            registerProtocols(props);
            registerCapabilities(props);
            // Set metadata store
            MetaStoreFactory factory = MetaStoreFactory.getInstance(propFile);
            String storeType = props.getProperty("store.type");
            store = factory.getMetaStore("store.type." + storeType);
            // Set backend storage
            StorageManagerFactory smf = StorageManagerFactory.getInstance(propFile);
            String backendType = props.getProperty("backend.type");
            backend = smf.getStorageManager("backend.type." + backendType);
            // Placeholder to authenticate to the backend storage
            // backend.authenticate(...)
            // Get known properties
            Props.initialize(Thread.currentThread().getContextClassLoader().getResource("ivoa_props.properties").toURI().getRawPath());
            for (String prop: Props.allProps()) {
                checkProperty(Props.getURI(prop), Props.getAttributes(prop), Props.isReadOnly(prop));
            }
            // Identifier regex
            VOS_PATTERN = Pattern.compile("vos://\\w[\\w\\-_\\.!~\\*'\\(\\)\\+=]{2,}(![\\w\\-_\\.!~\\*'\\(\\)\\+=]+(/[\\w\\-_\\.!~\\*'\\(\\)\\+=]+)*)+");
            TOKEN_PATTERN = Pattern.compile("\\w+\\.\\d+\\.\\d+\\..*");
            nfactory = NodeFactory.getInstance();
        } catch (VOSpaceException ve) {
            throw ve;
        } catch (Exception e) {
            throw new VOSpaceException(e);
        }
    }

    /**
     * Create the specified node
     * @param node The node to be created
     * @param owner The owner of the node
     * @param overwrite Whether to overwrite the node
     * @return The created node
     */
    public Node create(Node node, String owner, boolean overwrite) throws VOSpaceException {
        String uri = node.getUri();
        // Is identifier syntactically valid?
        if (!validId(uri)) throw new VOSpaceException(VOFault.InvalidURI, "", uri);
        // Is the parent a valid container?
        if (!validParent(uri)) {
            // Check for a LinkNode in the path.
            String linkedURI = resolveLinks(uri);
            if (linkedURI != null) throw new VOSpaceException(VOFault.LinkFoundFault, "", linkedURI);
            else throw new VOSpaceException(VOFault.ContainerNotFound, "", uri);
        }
        try {
            // Does node already exist?
            boolean exists = store.isStored(uri);
            if (exists && !overwrite) throw new VOSpaceException(VOFault.DuplicateNode, "", uri);
            // Check specified node type
            if (exists) {
                int type = store.getType(uri);
                if (type != NodeType.getIdByUri(node.getType())) throw new VOSpaceException(VOFault.PermissionDenied, "The node type cannot be changed.", uri);
            }
            NodeType type = NodeType.NODE;
            // Is a service-generated name required?
            if (uri.endsWith(AUTO_NODE)) {
                node.setUri(uri.substring(0, uri.length() - AUTO_NODE.length()) + UUID.randomUUID().toString());
                uri = node.getUri();
            }
            if (node instanceof DataNode) {
                // Clear any <accepts>, <provides>, <capabilities> and <nodes> that the user might specify
                DataNode datanode = (DataNode) node;
                datanode.removeAccepts();
                datanode.removeProvides();
                datanode.removeCapabilities();
                if (datanode instanceof ContainerNode) {
                    datanode.remove("/vos:node/vos:nodes/*");
                }
                // Add our <accepts>, <provides>, <capabilities> (though they will only be returned, not saved)
                type = addViewsAndCapabilities(datanode);
            }
            // Link node
            boolean localLink = false;
            String targetURI = null;
            if (node instanceof LinkNode) {
                type = NodeType.LINK_NODE;
                // Check the target of the link node to make sure it is valid
                targetURI = ((LinkNode)node).getTarget();
                localLink = targetURI.startsWith(ROOT_NODE);
                if (localLink && !store.isStored(targetURI)) {
                    // Check for a LinkNode in the path.
                    String linkedURI = resolveLinks(targetURI);
                    if (linkedURI != null) throw new VOSpaceException(VOFault.LinkFoundFault,
                            "The requested target URI contains a LinkNode.", "TARGET " + linkedURI);
                    else throw new VOSpaceException(VOFault.NodeNotFound,
                            "A Node does not exist with the requested target URI.", targetURI);
                }
            }
            // Check properties
            HashMap<String, String> nodeProps = node.getProperties();
            if (node.hasProperties()) {
                for (String propUri: nodeProps.keySet()) {
                    if (nodeProps.get(propUri) != "" && !checkProperty(propUri))
                            throw new VOSpaceException(VOFault.PermissionDenied,
                            "The property " + propUri + " is read only", uri);
                }
            }
            // Set properties (dates at least)
            String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());
            node.setProperty(Props.CTIME_URI, date);
            if (!exists) {
                node.setProperty(Props.DATE_URI, date);
                node.setProperty(Props.BTIME_URI, date);
                node.setProperty(Props.MTIME_URI, date);
                // Inherit permissions from parent if none set
                String parent = uri.substring(0, uri.lastIndexOf("/"));
                String grpRdVal = nodeProps.get(Props.GROUPREAD_URI);
                String grpWrVal = nodeProps.get(Props.GROUPWRITE_URI);
                String isPubVal = nodeProps.get(Props.ISPUBLIC_URI);
                String pubRdVal = nodeProps.get(Props.PUBLICREAD_URI);
                if (grpRdVal == null || grpRdVal == "") node.setProperty(Props.GROUPREAD_URI,
                        store.getPropertyValue(parent, Props.GROUPREAD_URI));
                if (grpWrVal == null || grpWrVal == "") node.setProperty(Props.GROUPWRITE_URI,
                        store.getPropertyValue(parent, Props.GROUPWRITE_URI));
                if ((isPubVal == null || isPubVal == "") && (pubRdVal == null || pubRdVal == "")) {
                    // If both are empty, set from parent; logical OR of the two properties
                    String parentIsPub = Boolean.toString(
                               Boolean.parseBoolean(store.getPropertyValue(parent, Props.ISPUBLIC_URI))
                            || Boolean.parseBoolean(store.getPropertyValue(parent, Props.PUBLICREAD_URI)));
                    node.setProperty(Props.ISPUBLIC_URI, parentIsPub);
                    node.setProperty(Props.PUBLICREAD_URI, parentIsPub);
                } else {
                    // Set them the same; logical OR of the two properties
                    String nodeIsPub = Boolean.toString(Boolean.parseBoolean(isPubVal) || Boolean.parseBoolean(pubRdVal));
                    node.setProperty(Props.ISPUBLIC_URI, nodeIsPub);
                    node.setProperty(Props.PUBLICREAD_URI, nodeIsPub);
                }
                node.setProperty(Props.LENGTH_URI, "0");
                node.setProperty(Props.MD5_URI, "");
                // node.setProperty(Props.get(Props.Property.LENGTH), Long.toString(backend.size(getLocation(node.getUri()))));
            }
            // Store node
            if (exists) {
                store.updateData(uri, node.toString());
            } else {
                String view = getView(uri);
                String location = getLocation(uri);
                // store.storeData(uri, type.ordinal(), USER, getLocation(uri), node.toString());
                store.storeData(uri, type.ordinal(), view, owner, location, node.toString());
                for (String capUri: node.getCapabilities()) {
                    store.registerCapability(uri, capUri);
                }
                // if (type.equals(NodeType.CONTAINER_NODE) && overwrite) {
                if (type.equals(NodeType.CONTAINER_NODE)) {
                    backend.createContainer(location);
                } else if (type.equals(NodeType.LINK_NODE)) {
                    // We only create a file if we have a local link
                    // Otherwise we'll just create the node and move on
                    if (localLink) {
                        String target = getLocation(targetURI);
                        backend.createLink(location, target);
                    }
                } else {
                    backend.touch(location);
                }
            }
            // if (type.equals(NodeType.CONTAINER_NODE) && !exists) {
                // boolean success = (new File(new URI(getLocation(node.getUri())))).mkdir();
                // backend.createContainer(getLocation(node.getUri()));
            // }
            // Check for deleted properties
            node.remove("/vos:node/vos:properties/vos:property[@xsi:nil = 'true']");
        } catch (VOSpaceException ve) {
            throw ve;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new VOSpaceException(e, uri);
        }
        return node;
    }

    public String addViewsAndCapabilitiesXMLStr(NodeType type) throws VOSpaceException {
        String prefix = null;
        List<String> accepts = new ArrayList<>();
        List<String> provides = new ArrayList<>();
        List<String> capabilities = new ArrayList<>();
        // Set <accepts> for UnstructuredDataNode
        if (type == NodeType.UNSTRUCTURED_DATA_NODE) {
            accepts.add(DataNode.buildViewXMLStr( prefix, Views.get(Views.View.ANY)));
        }
        // Set <accepts> for StructuredDataNode
        if (type == NodeType.STRUCTURED_DATA_NODE) {
            for (Views.View view: SPACE_ACCEPTS_IMAGE) {
                accepts.add(DataNode.buildViewXMLStr(prefix, Views.get(view)));
            }
            for (Views.View view: SPACE_ACCEPTS_TABLE) {
                accepts.add(DataNode.buildViewXMLStr(prefix, Views.get(view)));
            }
            for (Views.View view: SPACE_ACCEPTS_OTHER) {
                accepts.add(DataNode.buildViewXMLStr(prefix, Views.get(view)));
            }
            for (Views.View view: SPACE_PROVIDES_IMAGE) {
                provides.add(DataNode.buildViewXMLStr(prefix, Views.get(view)));
            }
            for (Views.View view: SPACE_PROVIDES_TABLE) {
                provides.add(DataNode.buildViewXMLStr(prefix, Views.get(view)));
            }
            for (Views.View view: SPACE_PROVIDES_OTHER) {
                provides.add(DataNode.buildViewXMLStr(prefix, Views.get(view)));
            }
        }
        // Set <accepts> for ContainerNode
        if (type == NodeType.CONTAINER_NODE) {
            for (Views.View view: SPACE_ACCEPTS_ARCHIVE) {
                accepts.add(DataNode.buildViewXMLStr(prefix, Views.get(view)));
            }
            for (Views.View view: SPACE_PROVIDES_ARCHIVE) {
                provides.add(DataNode.buildViewXMLStr(prefix, Views.get(view)));
            }
        }
        // Set capabilities
        if (CAPABILITIES.size() > 0) {
            for (String capUri: CAPABILITIES.keySet()) {
                Capability cap = (Capability) CAPABILITIES.get(capUri);
                if (cap.getApplicability().contains(type)) {
                    capabilities.add(
                            DataNode.addCapabilitiesXMLStr(prefix, capUri));
                }
            }
        }
        StringBuffer sb = new StringBuffer();
       /*
        if (accepts.size() == 0) {
            sb.append("<accepts/>");
        } else {
            sb.append("<accepts>");
            sb.append(accepts.toString());
            sb.append("</accepts>");
        }
        */
        addXMLToStringBuffer(sb, "accepts", accepts);
        addXMLToStringBuffer(sb, "provides", provides);
        addXMLToStringBuffer(sb, "capabilities", capabilities);

        return sb.toString();
    }

    private void addXMLToStringBuffer(StringBuffer sb, String parent, List<String> subelems) {
        if (subelems.size() == 0) {
            sb.append("<" + parent +"/>");
        } else {
            sb.append("<" + parent + ">");
            sb.append(String.join("", subelems));
            sb.append("</" + parent + ">");
        }

    }

    public NodeType addViewsAndCapabilities(DataNode datanode) throws VOSpaceException {
        NodeType type = NodeType.DATA_NODE;
        // Set <accepts> for UnstructuredDataNode
        if (datanode instanceof UnstructuredDataNode) {
            type = NodeType.UNSTRUCTURED_DATA_NODE;
            datanode.addAccepts(Views.get(Views.View.ANY));
        }
        // Set <accepts> for StructuredDataNode
        if (datanode instanceof StructuredDataNode) {
            type = NodeType.STRUCTURED_DATA_NODE;
            for (Views.View view: SPACE_ACCEPTS_IMAGE) {
                datanode.addAccepts(Views.get(view));
            }
            for (Views.View view: SPACE_ACCEPTS_TABLE) {
                datanode.addAccepts(Views.get(view));
            }
            for (Views.View view: SPACE_ACCEPTS_OTHER) {
                datanode.addAccepts(Views.get(view));
            }
            for (Views.View view: SPACE_PROVIDES_IMAGE) {
                datanode.addProvides(Views.get(view));
            }
            for (Views.View view: SPACE_PROVIDES_TABLE) {
                datanode.addProvides(Views.get(view));
            }
            for (Views.View view: SPACE_PROVIDES_OTHER) {
                datanode.addProvides(Views.get(view));
            }
        }
        // Set <accepts> for ContainerNode
        if (datanode instanceof ContainerNode) {
            type = NodeType.CONTAINER_NODE;
            for (Views.View view: SPACE_ACCEPTS_ARCHIVE) {
                datanode.addAccepts(Views.get(view));
            }
            for (Views.View view: SPACE_PROVIDES_ARCHIVE) {
                datanode.addProvides(Views.get(view));
            }
        }
        // Set capabilities
        if (CAPABILITIES.size() > 0) {
            for (String capUri: CAPABILITIES.keySet()) {
                Capability cap = (Capability) CAPABILITIES.get(capUri);
                if (cap.getApplicability().contains(type)) {
                    datanode.addCapabilities(capUri);
                }
            }
        }
        return type;
    }

    /**
     * Retrieve the specified node
     * @param identifier The identifier of the node to retrieve
     * @param detail The level of detail to apply to the node representation
     * @param limit The maximum number of results in the response
     * @return the retrieved node
     */
    public Node getNode(String identifier, String detail, int limit) throws VOSpaceException {
        Node node = null;
        // Is identifier syntactically valid?
        if (!validId(identifier)) throw new VOSpaceException(VOFault.InvalidURI);
        // Retrieve original node
        try {
            String[] result = store.getData(new String[] {identifier}, null, limit);
            if (result.length == 0) {
                // Check for a LinkNode in the path.
                String linkedURI = resolveLinks(identifier);
                if (linkedURI != null) throw new VOSpaceException(VOFault.LinkFoundFault, "", linkedURI);
                else throw new VOSpaceException(VOFault.NodeNotFound, "", identifier);
            }
            for (String item: result) {
                node = nfactory.getNode(item);
                detail = (detail == null) ? "max" : detail; // Try with min - MJG
                if (!detail.equals("max")) {
                    if (node instanceof DataNode) {
                        DataNode datanode = (DataNode) node;
                        datanode.removeAccepts();
                        datanode.removeProvides();
                        datanode.removeBusy();
                    }
                    node.removeCapabilities();
                    if (detail.equals("min")) {
                        node.removeProperties();
                    }
                } else {
                    if (node instanceof ContainerNode) {
                        ContainerNode container = (ContainerNode) node;
                        // Get children and check length property
                        String[] childNodes = store.getChildrenNodes(identifier);
                        container.addNode(StringUtils.join(childNodes));
//                      for (String child: store.getChildren(identifier)) {
//                          Node cnode = nfactory.getNode(store.getNode(child));
//                          cnode = setLength(cnode);
//                          container.addNode(cnode.toString());
//                      }
                    }
                }
                // Set properties
                node = setLength(node);
                if (!(node instanceof ContainerNode) && !(node instanceof LinkNode)) node = setMD5(node);
            }
        } catch (SQLException e) {
            throw new VOSpaceException(e);
        }
        return node;
    }


    /**
     * Set the length property on the specified node
     */
    public Node setLength(Node node) throws VOSpaceException {
//      String lengthUri = "/vos:node/vos:properties/vos:property[@uri = \"" + Props.LENGTH_URI + "\"]";
//      boolean setLength = false;
//      if (!node.has(lengthUri)) {
//          setLength = true;
//      } else if (node.get(lengthUri)[0].equals("0")) {
//          setLength = true;
//      }
//      if (setLength) {
        node.setProperty(Props.LENGTH_URI, Long.toString(backend.size(getLocation(node.getUri()))));
//      }
        return node;
    }


    /**
     * Set the MD5 property on the specified node
     * Need to optimize this for large files where a stored value is better
     */
    public Node setMD5(Node node) throws VOSpaceException {
        String md5Uri = "/vos:node/vos:properties/vos:property[@uri = \"" + Props.MD5_URI + "\"]";
        boolean setmd5 = false;
        if (!node.has(md5Uri)) {
            setmd5 = true;
        } else {
            String oldmd5 = node.get(md5Uri)[0];
            if (oldmd5.equals("") || oldmd5.equals("d41d8cd98f00b204e9800998ecf8427e")) {
            setmd5 = true;
            }
        }
        if (setmd5) {
            String md5val = backend.md5(getLocation(node.getUri()));
            if (md5val != null) node.setProperty(Props.MD5_URI, md5val);
        }
        return node;
    }



    /**
     * Delete the specified node
     * @param nodeid The identifier of the node to be deleted
     */
    public void delete(String identifier) throws VOSpaceException {
        // Is identifier syntactically valid?
        if (!validId(identifier)) throw new VOSpaceException(VOFault.InvalidURI);
        try {
            // Does node already exist?
            boolean exists = store.isStored(identifier);
            if (!exists) {
                // Check for a LinkNode in the path.
                String linkedURI = resolveLinks(identifier);
                if (linkedURI != null) throw new VOSpaceException(VOFault.LinkFoundFault, "", linkedURI);
                else throw new VOSpaceException(VOFault.NodeNotFound, "", identifier);
            }
            // Remove node
            boolean isContainer = (store.getType(identifier) == NodeType.CONTAINER_NODE.ordinal());
            String[] removedLinks = store.removeData(identifier, isContainer);
            backend.removeBytes(getLocation(identifier), isContainer);
            for (String link: removedLinks) {
                // System.out.println(link);
                backend.removeBytes(getLocation(link), false);
            }
        } catch (SQLException e) {
            throw new VOSpaceException(e);
        }
    }


    /**
     * Convert IVO identifier into VOSpace identifier
     * @param ivoid The IVO identifier to convert
     * @return The converted VOSpace identifier
     */
    private String getId(String ivoid) {
        return ivoid.replace("/", "!").replace("ivo:!!", "vos://");
    }

    /**
     * Check whether the specified identifier is valid
     * @param id The identifier to check
     * @return whether the identifier is valid or not
     */
    private boolean validId(String id) {
        Matcher m = VOS_PATTERN.matcher(id);
        return m.matches();
    }

    /**
     * Check whether the parent node of the specified identifier is valid:
     *   - it exists
     *   - it is a container
     * @param id The identifier to check
     * @return whether the parent node is valid or not
     */
    private boolean validParent(String id) throws VOSpaceException {
        try {
            String parent = id.substring(0, id.lastIndexOf("/"));
            if (store.getType(parent) != NodeType.CONTAINER_NODE.ordinal()) return false;
            return true;
        } catch (SQLException e) {
            throw new VOSpaceException(e, id);
        }
    }

    /**
     * Check whether any parent of the node is a LinkNode.
     * @param id The identifier to check
     * @return The canonical identifier of the node
     */
    public String resolveLinks(String id) throws VOSpaceException {
        if (id.startsWith(ROOT_NODE)) try {
            String parent = id;
            while (!store.isStored(parent) && !ROOT_NODE.equals(parent) && parent.contains("/")) {
                parent = parent.substring(0, parent.lastIndexOf("/"));
                if (store.getType(parent) == NodeType.LINK_NODE.ordinal()) {
                    String linkTarget = parent;
                    while (store.getType(linkTarget) == NodeType.LINK_NODE.ordinal()) {
                        linkTarget = store.getTarget(linkTarget);
                    }
                    // Recursively test for more links
                    String canonTest = linkTarget + id.substring(parent.length());
                    id = resolveLinks(canonTest);
                    return (id != null) ? id : canonTest;
                }
            }
        } catch (SQLException e) {
            throw new VOSpaceException(e, id);
        }
        return null;
    }

    /**
     * Generate a list of supported Views from the CSV list of args
     * @param args The CSV list of args
     * @return the corresponding list of views
     */
    private ArrayList<Views.View> getViewList(String args) {
        ArrayList<Views.View> list = new ArrayList<Views.View>();
        if (args != null) {
            String[] types = args.split(",");
            for (String type: types) {
                list.add(Views.fromString(type));
            }
        }
        return list;
    }

    /**
     * Generate a list of supported Capabilities from the CSV list of args
     * @param args The CSV list of args
     * @return the corresponding list of capabilities
     */
    private ArrayList<Capabilities.Capability> getCapabilityList(String args) {
        ArrayList<Capabilities.Capability> list = new ArrayList<Capabilities.Capability>();
        if (args != null) {
            String[] types = args.split(",");
            for (String type: types) {
                list.add(Capabilities.fromString(type));
            }
        }
        return list;
    }

    /**
     * Check whether the specified property is in the list of known properties -
     *      if not, add it - and that the property is also not read only
     * @param uri The identifier of the property to check
     * @return whether a successful property check or not
     */
    private boolean checkProperty(String uri) throws SQLException {
        if (!store.isKnownProperty(uri)) store.registerProperty(uri, PROPERTIES_SPACE_CONTAINS, false);
        return (!store.isReadOnly(uri));
    }

    /**
     * Check whether the specified property is in the list of known properties -
     *      if not, add it - and that the property is also not read only
     * @param uri The identifier of the property to check
     * @return whether a successful property check or not
     */
    private boolean checkProperty(String uri, int attrs, boolean readOnly) throws SQLException {
        if (!store.isKnownProperty(uri)) store.registerProperty(uri, attrs, readOnly);
        else store.updateProperty(uri, attrs, readOnly);
        return readOnly;
    }

    /**
     * Get a location for an object
     * @param identifier The identifier of the object
     * @return the corresponding physical location of the object
     */
    private String getLocation(String identifier) {
        String name = identifier.substring(identifier.lastIndexOf("!"));
        String dataname = name.substring(name.indexOf("/") + 1);
        return BASEURI + "/" + dataname;
    }

    /**
     * Resolve the specified location for a file
     * @param identifier The logical identifier for the file
     * @param viewCheck Whether to check if a view transformation should happen
     * @return the physical location for the file
     */
    protected String resolveLocation(String identifier, boolean viewCheck) throws VOSpaceException {
        String target = null;
        String view = null;
        try {
            if (store.isTransferByEndpoint(identifier)) {
                if (!store.isCompletedByEndpoint(identifier)) {
                    StringReader in = new StringReader(store.getTransfer(identifier));
                    XMLInputFactory xif = XMLInputFactory.newInstance();
                    XMLStreamReader2 xsr = (XMLStreamReader2) xif.createXMLStreamReader(in);
                    xsr.nextTag(); // Advance to first element - <vos:transfer>
                    while (xsr.hasNext()) { // Cycle through elements
                        int eventType = xsr.next();
                        if (eventType == XMLEvent.START_ELEMENT) {
                            String name = xsr.getLocalName();
                            if (name == "target") {
                                target = xsr.getElementText();
                                //                              break;
                            } else if (name == "view") { // Check view
                                view = xsr.getAttributeValue(null, "uri");
                                //                              break;
                            }
                        }
                    }
                } else {
                    throw new VOSpaceException(VOFault.InvalidURI, "The specified URI is no longer valid.");
                }
                // View transformation
                target = target.replace("~", "!");
                String location = store.getLocation(target);
                if (viewCheck && !view.equals("ivo://ivoa.net/vospace/core#defaultview")) {
                    String oldView = store.getView(target);
                    location = engine.transform(location, oldView, view);
                }
                // Make sure that a URI is returned
                if (!location.startsWith("file://")) location = "file://" + location;
                return location;
            } else {
                throw new VOSpaceException(VOFault.NodeNotFound, "The specified file cannot be found.");
            }
        } catch (VOSpaceException ve) {
            throw ve;
        } catch (Exception e) {
            throw new VOSpaceException(e);
        }
    }

    /**
     * Mark the location as having been used
     * @param endpoint The location to be marked as used
     */
    protected void invalidateLocation(String endpoint) throws VOSpaceException {
        try {
            store.completeTransfer(endpoint);
        } catch (Exception e) {
            throw new VOSpaceException(e);
        }
    }

    /**
     * Check whether the specified location has expired
     * @param identifier The logical identifier for the file
     * @return whether the location has expired or not
     */
    protected boolean hasExpired(String identifier) throws VOSpaceException {
        boolean expired = false;
        try {
            long created = store.getCreated(identifier);
            System.err.println(identifier + " " + Long.toString(created) + " " + (System.currentTimeMillis() - created));
            //if (System.currentTimeMillis() - created > 3600000) expired = true;
            if (System.currentTimeMillis() - created > 2000) expired = true;
            return expired;
        } catch (Exception e) {
            throw new VOSpaceException(e);
        }
    }


    /**
     * Infer the view from the file extension
     * @param uri The identifier of the object
     * @return the view of the object
     */
    public String getView(String uri) throws VOSpaceException {
        String extension = uri.substring(uri.lastIndexOf(".") + 1);
        String view = engine.VIEWS.get(extension.toUpperCase());
        if (view == null) view = "ivo://ivoa.net/vospace/views/blob";
        return view;
    }


    /**
     * Update the size property of a node
     */
    public void updateSize(String endpoint, String size) throws VOSpaceException {
        try {
            String result = store.getTransfer(endpoint);
            if (result != null) {
                Transfer transfer = new Transfer(result);
                String target = transfer.getTarget();
                Node node = nfactory.getNode(store.getNode(target));
                node.setProperty(Props.LENGTH_URI, size);
                // Update the timestamps for modification
                String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());
                node.setProperty(Props.CTIME_URI, date);
                node.setProperty(Props.MTIME_URI, date);
                store.updateData(target, node.toString());
            }
        } catch (VOSpaceException ve) {
            throw ve;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new VOSpaceException(e);
        }
    }


    /**
     * Register a node
     */
    public void registerNode(Node node, String user, String location) throws VOSpaceException {
        try {
            Node newNode = create(node, user, false);
            newNode.setProperty(Props.LENGTH_URI, Long.toString(backend.size(location)));
            store.updateData(newNode.getUri(), newNode.toString());
        } catch (VOSpaceException ve) {
            throw ve;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new VOSpaceException(e);
        }
    }


    /**
     * Register a set of protocols
     * @param props The property file containing details of the protocols and their handlers
     */
    private void registerProtocols(Properties props) throws VOSpaceException {
        try {
            PROTOCOLS = new HashMap<String, ProtocolHandler>();
            // Server protocols
            String[] serverProtocols = props.getProperty("space.protocol.server").split(",");
            SPACE_SERVER_PROTOCOLS = new ArrayList<Protocol>();
            for (String protocol : serverProtocols) {
                String protocolAcronym = protocol.trim().substring(0, protocol.trim().indexOf("-"));
                String baseurl = props.getProperty("server." + protocolAcronym + ".url");
                String protocolClass = props.getProperty("space.protocol.handler." + protocol.trim());
                ProtocolHandler protocolHandler = (ProtocolHandler) Class.forName(protocolClass).newInstance();
                protocolHandler.setBaseUrl(baseurl);
                PROTOCOLS.put(protocolHandler.getUri(), protocolHandler);
                Protocol pType = new Protocol();
                pType.setURI(protocolHandler.getUri());
                SPACE_SERVER_PROTOCOLS.add(pType);
            }
            // Client protocols
            String[] clientProtocols = props.getProperty("space.protocol.client").split(",");
            SPACE_CLIENT_PROTOCOLS = new ArrayList<Protocol>();
            for (String protocol : clientProtocols) {
                String protocolAcronym = protocol.trim().substring(0, protocol.trim().indexOf("-"));
                String baseurl = props.getProperty("server." + protocolAcronym + ".url");
                String protocolClass = props.getProperty("space.protocol.handler." + protocol.trim());
                ProtocolHandler protocolHandler = (ProtocolHandler) Class.forName(protocolClass).newInstance();
                // The protocol handlers make no distinction between client and
                // server roles so need to set base URL even though client use
                protocolHandler.setBaseUrl(baseurl);
                PROTOCOLS.put(protocolHandler.getUri(), protocolHandler);
                Protocol pType = new Protocol();
                pType.setURI(protocolHandler.getUri());
                SPACE_CLIENT_PROTOCOLS.add(pType);
            }
        } catch (VOSpaceException ve) {
            throw ve;
        } catch (Exception e) {
            throw new VOSpaceException(e);
        }
    }

    /**
     * Register a set of capabilities
     * @param props The property file containing details of the capabilities and their implementation
     */
    private void registerCapabilities(Properties props) throws VOSpaceException {
        try {
            CAPABILITY_EXE = props.getProperty("space.capability.exe");
            CAPABILITY_PORT = Integer.parseInt(props.getProperty("space.capability.port"));
            CAPABILITIES = new HashMap<String, Capability>();
            PROCESSES = new HashMap<String, Process>();
            String[] capabilities = props.getProperty("space.capabilities").split(",");
            for (String capability : capabilities) {
                String capKey = "space.capability." + capability.trim();
                String capUri = "";
                Capability capImpl = null;
                if (props.containsKey(capKey)) {
                    String capabilityClass = props.getProperty(capKey);
                    capImpl = getCapabilityImpl(capabilityClass, capKey, props);
                    capUri = capImpl.getUri();
                } else {
                    CAPABILITY_BASE = props.getProperty("space.capability.baseivorn");
                    String runnerClass = props.getProperty("space.capability.runner");
                    if (CAPABILITIES.containsKey(CAPABILITY_BASE + "#runner")) {
                        capImpl = (Capability) CAPABILITIES.get(CAPABILITY_BASE + "#runner");
                    } else {
                        capImpl = getCapabilityImpl(runnerClass, "space.capability.runner", props);
                    }
                    capUri = CAPABILITY_BASE + "#" + capability.trim();
                }
                CAPABILITIES.put(capUri, capImpl);
            }
        } catch (VOSpaceException ve) {
            throw ve;
        } catch (Exception e) {
            throw new VOSpaceException(e);
        }
    }


    private Capability getCapabilityImpl(String className, String capKey, Properties props) throws VOSpaceException {
        try {
            Capability capImpl = (Capability) Class.forName(className).newInstance();
            Map<String, String> params = new Hashtable<String, String>();
            for (String key: props.stringPropertyNames()) {
                if (key.startsWith(capKey + ".")) {
                    String parName = key.substring(key.lastIndexOf(".") + 1);
                    String parValue = props.getProperty(key);
                    params.put(parName, parValue);
                }
            }
            capImpl.setParams(params);
            return capImpl;
        } catch (VOSpaceException ve) {
            throw ve;
        } catch (Exception e) {
            throw new VOSpaceException(e);
        }
    }


    protected StorageManager getStorageManager() {
        return backend;
    }

    protected MetaStore getMetaStore() {
        return store;
    }

    protected void addProcess(String container, Process p) {
        PROCESSES.put(container, p);
    }

    protected Process getProcess(String container) {
        return PROCESSES.get(container);
    }

    /**
     * Check the update time of the node against the filesystem
     */
    public boolean hasBeenUpdated(String identifier) throws VOSpaceException {
        boolean updated = true;
        try {
            long dbTime = store.getLastModTime(identifier);
            if (dbTime != 0) {
                long osTime = backend.lastModified(store.getLocation(identifier));
                if (osTime - dbTime < 0) updated = false;
            }
        } catch (SQLException e) {
            throw new VOSpaceException(e);
        }
        return updated;
    }


    /**
     * Validate the provided DataLab token
     */
    private void checkTokenFormat(String authToken) throws VOSpaceException {
        // Validates a security token
        Matcher m = TOKEN_PATTERN.matcher(authToken);
        if (!m.matches()) throw new VOSpaceException(VOFault.PermissionDenied, "The provided DataLab token is invalid");
    }


    /**
     * Validate the provided DataLab token
     */
    public void validateToken(String authToken) throws VOSpaceException {
        if (AUTH_URL.startsWith("null://")) return;
        if (AUTH_URL == "" || AUTH_URL.startsWith("local://")) {
            checkTokenFormat(authToken);
        } else {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(AUTH_URL + "/isValidToken?token=" + authToken);
            try {
                int statusCode = client.executeMethod(get);
                if (statusCode != HttpStatus.SC_OK) throw new VOSpaceException(VOFault.PermissionDenied, "The provided DataLab token is invalid");
            } catch (IOException e) {
                throw new VOSpaceException(e);
            }
        }
    }


    /**
     * Validate the requested access associated with the given DataLab token
     * @param authToken The authority token associated with the user
     * @param node The identifier of the node being accessed
     * @param isRead The mode of access - read/write
     */
    public void validateAccess(String authToken, String node, boolean isRead) throws VOSpaceException {
        if (AUTH_URL.startsWith("null://")) return;
        if (AUTH_URL == "" || AUTH_URL.startsWith("local://")) checkTokenFormat(authToken);
        try {
            // If node does not exist, check write access to parent
            boolean exists = store.isStored(node);
            while (!exists) {
                node = node.substring(0, node.lastIndexOf("/"));
                exists = store.isStored(node);
            }
            if (ROOT_NODE.equals(node)) throw new VOSpaceException(VOFault.NodeNotFound);
            // Get owner and groups for requested node
            String[] authProps = store.getPropertyValues(node, new String[]{ Props.ISPUBLIC_URI,
                    Props.PUBLICREAD_URI, Props.GROUPREAD_URI, Props.GROUPWRITE_URI });
            String groups = "";
            if (isRead) {
                // Check the publicRead and isPublic properties and return if true
                if (Boolean.parseBoolean(authProps[0]) || Boolean.parseBoolean(authProps[1])) return;
                groups = authProps[2];
            } else {
                groups = authProps[3];
            }
            String owner = store.getOwner(node);
            if (AUTH_URL == "" || AUTH_URL.startsWith("local://")) {
                if (!Arrays.asList(StringUtils.split(groups, ",")).contains(owner)) throw new VOSpaceException(VOFault.PermissionDenied);
            } else {
                // Validates the access request
                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(AUTH_URL + "/hasAccess?token=" + authToken + "&owner=" + owner + "&groups=" + groups);
                int statusCode = client.executeMethod(get);
                if (statusCode != HttpStatus.SC_OK) throw new VOSpaceException(VOFault.PermissionDenied);
            }
        } catch (IOException e) {
            throw new VOSpaceException(e);
        } catch (SQLException e) {
            throw new VOSpaceException(e);
        }
    }

}
