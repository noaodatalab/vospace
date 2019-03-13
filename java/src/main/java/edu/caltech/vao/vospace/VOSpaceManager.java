
package edu.caltech.vao.vospace;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

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
        if (ref == null) throw new VOSpaceException(new NullPointerException("VOSpaceManager could not be initialized."));
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
            for (Props.Property prop : Props.Property.values()) {
                checkProperty(Props.get(prop));
            }
            // Identifier regex
            VOS_PATTERN = Pattern.compile("vos://[\\w\\d][\\w\\d\\-_\\.!~\\*'\\(\\)\\+=]{2,}(![\\w\\d\\-_\\.!~\\*'\\(\\)\\+=]+(/[\\w\\d\\-_\\.!~\\*'\\(\\)\\+=]+)*)+");
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
        if (!validId(uri)) throw new VOSpaceException(VOFault.InvalidURI);
        // Is the parent a valid container?
        // FIXME: We need to check for a LinkNode in the path here.
        if (!validParent(uri)) throw new VOSpaceException(VOFault.ContainerNotFound);
        try {
            // Does node already exist?
            boolean exists = store.isStored(uri);
            if (exists && !overwrite) throw new VOSpaceException(VOFault.DuplicateNode);
            // Check specified node type
            if (exists) {
                int type = store.getType(uri);
                if (type != NodeType.getIdByUri(node.getType())) throw new VOSpaceException(VOFault.PermissionDenied, "The node type cannot be changed.");
            }
            NodeType type = NodeType.NODE;
            // Is a service-generated name required?
            if (uri.endsWith(AUTO_NODE)) {
                node.setUri(uri.substring(0, uri.length() - AUTO_NODE.length()) + UUID.randomUUID().toString());
                uri = node.getUri();
            }
            // Clear any <accepts>, <provides>, <capabilities> and <nodes> that the user might specify
            if (node instanceof DataNode) {
                type = NodeType.DATA_NODE;
                DataNode datanode = (DataNode) node;
                datanode.removeAccepts();
                datanode.removeProvides();
                datanode.removeCapabilities();
                // Set <accepts> for UnstructuredDataNode
                if (node instanceof UnstructuredDataNode) {
                    type = NodeType.UNSTRUCTURED_DATA_NODE;
                    datanode.addAccepts(Views.get(Views.View.ANY));
                }
                // Set <accepts> for StructuredDataNode
                if (node instanceof StructuredDataNode) {
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
                if (node instanceof ContainerNode) {
                    type = NodeType.CONTAINER_NODE;
                    for (Views.View view: SPACE_ACCEPTS_ARCHIVE) {
                        datanode.addAccepts(Views.get(view));
                    }
                    for (Views.View view: SPACE_PROVIDES_ARCHIVE) {
                        datanode.addProvides(Views.get(view));
                    }
                    // Clear any children that may be set
                    datanode.remove("/vos:node/vos:nodes/*");
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
                    throw new VOSpaceException(VOFault.InvalidURI, "The requested target URI is invalid");
                }
            }
            // Check properties
            if (node.hasProperties()) {
                for (String propUri: node.getProperties().keySet()) {
                    if (!checkProperty(propUri)) throw new VOSpaceException(VOFault.PermissionDenied, "The property " + propUri + " is read only");
                }
            }
            // Set properties (date at least)
            if (!exists) {
                node.setProperty(Props.get(Props.Property.DATE), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()));
                // Inherit permissions from parent if none set
                HashMap<String, String> nodeProps = node.getProperties();
                String parent = uri.substring(0, uri.lastIndexOf("/"));
                String grpRd = Props.get(Props.Property.GROUPREAD);
                String grpWr = Props.get(Props.Property.GROUPWRITE);
                String isPub = Props.get(Props.Property.ISPUBLIC);
                String parentGrpRd = store.getPropertyValue(parent, grpRd);
                String parentGrpWr = store.getPropertyValue(parent, grpWr);
                String parentIsPub = store.getPropertyValue(parent, isPub);
                if (nodeProps.get(grpRd) == "") node.setProperty(grpRd, parentGrpRd);
                if (nodeProps.get(grpWr) == "") node.setProperty(grpWr, parentGrpWr);
                if (nodeProps.get(isPub) == "") node.setProperty(isPub, parentIsPub);
                node.setProperty(Props.get(Props.Property.LENGTH), "0");
                node.setProperty(Props.get(Props.Property.MD5), "");
                //          node.setProperty(Props.get(Props.Property.LENGTH), Long.toString(backend.size(getLocation(node.getUri()))));
            }
            // Store node
            if (exists) {
                store.updateData(uri, node.toString());
            } else {
                String view = getView(uri);
                String location = getLocation(uri);
                //              store.storeData(uri, type.ordinal(), USER, getLocation(uri), node.toString());
                store.storeData(uri, type.ordinal(), view, owner, location, node.toString());
                for (String capUri: node.getCapabilities()) {
                    store.registerCapability(uri, capUri);
                }
//              if (type.equals(NodeType.CONTAINER_NODE) && overwrite) {
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
            throw new VOSpaceException(e);
        }
        return node;
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
        // FIXME: Somewhere here we need to check if one of the parents is a LinkNode!
        try {
            String[] result = store.getData(new String[] {identifier}, null, limit);
            if (result.length == 0) throw new VOSpaceException(VOFault.NodeNotFound);
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
                        int length = 0;
                        for (String child: childNodes) {
                            length += child.length();
                        }
                        StringBuilder children = new StringBuilder(length);
                        for (String child: childNodes) {
                            children.append(child);
                        }
                        container.addNode(children.toString());
//                      for (String child: store.getChildren(identifier)) {
//                          Node cnode = nfactory.getNode(store.getNode(child));
//                          cnode = setLength(cnode);
//                          container.addNode(cnode.toString());
//                      }
                    }
                }
                // Set properties
                node = setLength(node);
                if (!(node instanceof ContainerNode)) node = setMD5(node);
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
        String length = Props.get(Props.Property.LENGTH);
        String lengthUri = "/vos:node/vos:properties/vos:property[@uri = \"" + length + "\"]";
//      boolean setLength = false;
//      if (!node.has(lengthUri)) {
//          setLength = true;
//      } else if (node.get(lengthUri)[0].equals("0")) {
//          setLength = true;
//      }
//      if (setLength) {
        node.setProperty(length, Long.toString(backend.size(getLocation(node.getUri()))));
//      }
        return node;
    }


    /**
     * Set the MD5 property on the specified node
     * Need to optimize this for large files where a stored value is better
     */
    public Node setMD5(Node node) throws VOSpaceException {
        String md5 = Props.get(Props.Property.MD5);
        String md5Uri = "/vos:node/vos:properties/vos:property[@uri = \"" + md5 + "\"]";
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
            if (md5val != null) node.setProperty(md5, md5val);
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
            // FIXME: We need to check for a LinkNode as a parent here!
            if (!exists) throw new VOSpaceException(VOFault.NodeNotFound);
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
            throw new VOSpaceException(e);
        }
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
     * if not, add it - and that the property is also not read only
     * @param uri The identifier of the property to check
     * @return whether a successful property check or not
     */
    private boolean checkProperty(String uri) throws SQLException {
        boolean success = true;
        if (!store.isKnownProperty(uri))
            store.registerProperty(uri, PROPERTIES_SPACE_CONTAINS, false);
        if (store.isReadOnly(uri)) success = false;
        return success;
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
            if (System.currentTimeMillis() - created > 3600000) expired = true;
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
                node.setProperty(Props.get(Props.Property.LENGTH), size);
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
            newNode.setProperty(Props.get(Props.Property.LENGTH), Long.toString(backend.size(location)));
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
    public void validateToken(String authToken) throws VOSpaceException {
        // Validates a security token
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(AUTH_URL + "/isValidToken?token=" + authToken);
        try {
            int statusCode = client.executeMethod(get);
            if (statusCode != HttpStatus.SC_OK) throw new VOSpaceException(VOFault.PermissionDenied, "The provided DataLab token is invalid");
        } catch (IOException e) {
            throw new VOSpaceException(e);
        }
    }


    /**
     * Validate the requested access associated with the given DataLab token
     * @param authToken The authority token associated with the user
     * @param node The identifier of the node being accessed
     * @param isRead The mode of access - read/write
     */
    public void validateAccess(String authToken, String node, boolean isRead) throws VOSpaceException {
        try {
            // If node does not exist, check write access to parent
            boolean exists = store.isStored(node);
            if (!exists) {
                String parent = node.substring(0, node.lastIndexOf("/"));
                node = parent;
            }
            // Get owner and groups for requested node
            String groups = "";
            if (isRead) {
                groups = store.getPropertyValue(node, Props.get(Props.Property.GROUPREAD));
            } else {
                groups = store.getPropertyValue(node, Props.get(Props.Property.GROUPWRITE));
            }
            String owner = store.getOwner(node);
            // Validates the access request
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(AUTH_URL + "/hasAccess?token=" + authToken + "&owner=" + owner + "&groups=" + groups);
            int statusCode = client.executeMethod(get);
            if (statusCode != HttpStatus.SC_OK) throw new VOSpaceException(VOFault.PermissionDenied);
        } catch (IOException e) {
            throw new VOSpaceException(e);
        } catch (SQLException e) {
            throw new VOSpaceException(e);
        }
    }

}
