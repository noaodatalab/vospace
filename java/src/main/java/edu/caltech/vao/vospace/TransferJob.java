
package edu.caltech.vao.vospace;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;


import uws.UWSException;
import uws.job.JobThread;
import uws.job.ErrorType;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;
import uws.job.ExecutionPhase;

import edu.caltech.vao.vospace.meta.MetaStore;
import edu.caltech.vao.vospace.meta.MetaStoreFactory;
import edu.caltech.vao.vospace.capability.Capability;
import edu.caltech.vao.vospace.protocol.ProtocolHandler;
import edu.caltech.vao.vospace.storage.StorageManager;
import edu.caltech.vao.vospace.storage.StorageManagerFactory;
import edu.caltech.vao.vospace.xml.ContainerNode;
import edu.caltech.vao.vospace.xml.DataNode;
import edu.caltech.vao.vospace.xml.Node;
import edu.caltech.vao.vospace.xml.NodeFactory;
import edu.caltech.vao.vospace.xml.Protocol;
import edu.caltech.vao.vospace.xml.Transfer;


/**
  * A class to represent a transfer job.
  */
public class TransferJob extends JobThread {

    private final static boolean STATUS_BUSY = true;
    private final static boolean STATUS_FREE = false;

    private Transfer transfer;
    private NodeUtil utils;
    private MetaStore store;
    private StorageManager backend;
    private NodeFactory factory;
    private VOSpaceManager manager;
    private String USER = "";
    private ArrayList<String> SUPPORTED;

    public TransferJob(UWSJob j) throws UWSException {
	super(j);
    }
    
   /**
     * Validate the transfer representation - check URIs, views and protocols
     */
    private void validateTransfer() throws UWSException {
	try{
	    // Check transfer details
	    String target = transfer.getTarget();
	    String direction = transfer.getDirection();
	    boolean external = !direction.startsWith("vos");
	    // Syntactically valid target and direction (move, copy)
	    if (!utils.validId(target)) throw new UWSException(UWSException.BAD_REQUEST, "The requested target URI is invalid");
	    if (!external && !utils.validId(direction)) throw new UWSException(UWSException.BAD_REQUEST, "The requested direction URI is invalid");
	    // Parent node
	    if (!external && !utils.validParent(direction)) throw new UWSException(UWSException.BAD_REQUEST, "The parent node is not valid");
	    // Existence
	    if (store.isStored(target)) {
		if (direction.equals("pushToVoSpace") || direction.equals("pullToVoSpace")) {
		    // Container
		    if (store.getType(target) == NodeType.CONTAINER_NODE.ordinal()) throw new UWSException(UWSException.BAD_REQUEST, "Data cannot be uploaded to a container"); 
		}
	    } else {
		if (!external || (direction.equals("pullFromVoSpace") || direction.equals("pushFromVoSpace"))) throw new UWSException(VOSpaceException.CONFLICT, "A Node does not exist with the requested URI"); 
	    }
	    if (!external && store.isStored(direction) && store.getType(direction) != NodeType.CONTAINER_NODE.ordinal()) { 
       		throw new UWSException(VOSpaceException.CONFLICT, "A Node already exists with the requested URI");		
	    }
	    if (external) {
		// Views
		String uri = transfer.getView().getURI();
		Views.View view = Views.fromValue(uri);
		if (view == null) throw new UWSException(UWSException.BAD_REQUEST, "The service does not support the requested View");
		if (!view.equals(Views.View.DEFAULT) && !manager.SPACE_ACCEPTS_IMAGE.contains(view) && !manager.SPACE_ACCEPTS_TABLE.contains(view) && !manager.SPACE_ACCEPTS_ARCHIVE.contains(view) && !manager.SPACE_ACCEPTS_OTHER.contains(view)) throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "The service does not support the requested View");
		// Protocols
		if (direction.equals("pushFromVoSpace") || direction.equals("pullToVoSpace")) {
		    checkProtocols(transfer.getProtocol(), manager.SPACE_SERVER_PROTOCOLS);
		} else if (direction.equals("pushToVoSpace") || direction.equals("pullFromVoSpace")) {
		    checkProtocols(transfer.getProtocol(), manager.SPACE_CLIENT_PROTOCOLS);
		}
	    }
	} catch (SQLException e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	} catch (VOSpaceException e) {
	    // Issue here with non-500 status codes not terminating job
	    throw new UWSException(e.getStatusCode(), e.getMessage());
	}
    }

    @Override
    /**
     * The main business logic of the data transfer
     */
    protected void jobWork() throws UWSException, InterruptedException {
     
	String target, direction;
	boolean status = false;
	long startInstance = System.currentTimeMillis();

	// Initialize and validate
	try {
	    // Set metadata store
	    MetaStoreFactory mfactory = MetaStoreFactory.getInstance();
	    store = mfactory.getMetaStore();
	    // Set backend storage
	    StorageManagerFactory smf = StorageManagerFactory.getInstance();
	    backend = smf.getStorageManager();
	    // Placeholder to authenticate to the backend storage
	    // backend.authenticate(...)
	    // Other managers and utilities
	    utils = new NodeUtil(store);
	    factory = NodeFactory.getInstance();
	    manager = VOSpaceManager.getInstance();
	    // Transfer details
	    // Workaround due to incorrect parsing of transfer representation
	    // by UWS library - only works with x-www-form-encoding
	    String parValue = (String) job.getAdditionalParameterValue("<vos:transfer xmlns:vos");
	    String document = "";
	    if (parValue != null) {
		document = "<vos:transfer xmlns:vos=" + parValue;
	    } else {
		parValue = (String) job.getAdditionalParameterValue("<transfer xmlns");
		document = "<transfer xmlns=" + parValue;
	    }
	    System.err.println(document);
	    transfer = new Transfer(document);
	    validateTransfer();
	} catch (VOSpaceException e) {
	    throw new UWSException(e.getStatusCode(), e.getMessage());
	}

	// Determine operation
	try {
	    target = transfer.getTarget();
	    direction = transfer.getDirection();
	} catch (VOSpaceException e) {
	    throw new UWSException(e.getStatusCode(), e.getMessage());
	}
	
	// Executing
	if (!isInterrupted()) {
	    try {
		if (direction.equals("pushToVoSpace")) {
		    pushToVoSpace();
		} else if (direction.equals("pullToVoSpace")) {
		    pullToVoSpace();
		} else if (direction.equals("pushFromVoSpace")) {
		    pushFromVoSpace();
		} else if (direction.equals("pullFromVoSpace")) {
		    pullFromVoSpace();
		} else if (transfer.isKeepBytes()) {
		    copyNode();
		} else if (!transfer.isKeepBytes()) {
		    moveNode();
		}
	    } catch (VOSpaceException e) {
		e.printStackTrace(System.err);
		throw new UWSException(e.getStatusCode(), e.getMessage());
	    } catch (SQLException e) {
		e.printStackTrace(System.err);
		throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	    }

	    if (direction.equals("pushToVoSpace") || direction.equals("pullFromVoSpace")) {

		String file = getLocation(target); 
		if (target.endsWith(".auto")) {
		    String jobId = getJobId();
		    try {
			String details = store.getResult(jobId);
			file = details.substring(details.indexOf("<vos:target>") + 12, details.indexOf("</vos:target>"));
			target = file;
			//			file = file.replace("vos://nvo.caltech!vospace", manager.BASEURI); 
			file = file.replace("vos://datalab.noao.edu!vospace", manager.BASEURI); 
		    } catch (SQLException e) {
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
		    }
		}

		// Loop activity
		String jobId = getJobId();
		while (!isInterrupted() && !status) {
		    try {
			Thread.sleep(1000);
			if (direction.equals("pushToVoSpace")) {
//			    status = checkLocation(file, startInstance);
			    status = store.isCompleted(jobId);
			} else if (direction.equals("pullFromVoSpace")) {
			    status = checkTime(startInstance);
			} 
		    } catch (SQLException e) {
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
		    } 
		}

		// Update length property for pushToVoSpace		
//		if (direction.equals("pushToVoSpace")) {
		try {
		    if (manager.hasBeenUpdated(target)) {
			Node node = getNode(target);
			node = manager.setLength(node);
			store.updateData(target, node.toString());
		    }
		} catch (SQLException e) {
		    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
		} catch (VOSpaceException e) {
		    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
		} catch (NullPointerException e) {
		    System.err.println("No node for target: " + target);
		}
	    } 

	    if (!isInterrupted()) {
		// Reset node status
	        if (direction.equals("pushToVoSpace") || direction.equals("pullToVoSpace")) { 
		    Node node = getNode(target);
		    if (node != null) setNodeStatus(node, STATUS_FREE);
		}
	    }
	}
	
	// Check whether any capabilities need to be triggered
	Node parent = getNode(target.substring(0, target.lastIndexOf("/")));
	if (!direction.equals("pushFromVoSpace") && !direction.equals("pullFromVoSpace")) {
	    try {
		for (String capability: parent.getCapabilities()) {
		    int port = store.isActive(parent.getUri(), capability);
		    if (port > 0) {
			trigger(target, capability, port);
		    }
		    // Does this need to wait for any capabilities to finish?
		}
	    } catch (SQLException e) {
		throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	    } catch (VOSpaceException e) {
		throw new UWSException(e.getStatusCode(), e.getMessage());	          }  
	}

	// Check whether capability enabled - assumes naming convention
	// for capability configuration file
	if (target.endsWith("cap.conf")) {
	    String shortCap = target.substring(target.lastIndexOf("/") + 1, target.lastIndexOf("cap.conf") - 1);
	    try {
		for (String capability: parent.getCapabilities()) {
		    if (capability.endsWith(shortCap)) {
			int port = store.getCapPort();
			if (port == 0 && manager.PROCESSES.size() == 0) {
			    String[] cmdArgs = new String[] {"python", manager.CAPABILITY_EXE, "--port", String.valueOf(manager.CAPABILITY_PORT), "--config", getLocation(target)};
			    System.err.println(manager.CAPABILITY_EXE + " " + "-port" + " " +  String.valueOf(manager.CAPABILITY_PORT) + " " + "-config" + " " + getLocation(target));
			    Process p = Runtime.getRuntime().exec(cmdArgs);
			    manager.addProcess(target, p);
			}
			store.setActive(parent.getUri(), capability, 1);
		    }
		}
	    } catch (Exception e) {
		throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	    }
//	    } catch (IOException e) {
//		throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
//	    } catch (SQLException e) {
//		throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
//	    } catch (VOSpaceException e) {
//		throw new UWSException(e.getStatusCode(), e.getMessage());	 //         }  
	}

	if (isInterrupted()) {
	    throw new InterruptedException();
	}
    }

    public void clearResources() {
	// Stop the job (if running)
	getJob().clearResources();

    }

    /**
     * Request a URL to send data to the space
     */
    private void pushToVoSpace() throws UWSException {
	// Request details
	Node node = null;
	try {
	    System.err.println(transfer.toString());
	    String target = transfer.getTarget();
	    // Create node (if necessary)
	    if (!store.isStored(target)) {
	        Node blankNode = factory.getDefaultNode();
	        blankNode.setUri(target);
	        node = manager.create(blankNode, false);
	    } else {
	        node = getNode(target);
	    }
	    String uri = node.getUri();
	    // Negotiate protocol details
	    completeProtocols(target, ProtocolHandler.SERVER);
	    // Register transfer endpoints
	    registerEndpoint();
	    // Set node status to busy
	    setNodeStatus(node, STATUS_BUSY);
	    // Add to results
	    if (target.endsWith(".auto")) transfer.setTarget(uri);
	    store.addResult(getJobId(), transfer.toString());  
	    if (target.endsWith(".auto")) {
		getJob().addResult(new Result(getJob(), "nodeDetails", manager.BASE_URL + "nodes/" + uri.substring(uri.lastIndexOf("/") + 1)));
	    }
	    getJob().addResult(new Result(getJob(), "transferDetails", manager.BASE_URL + "transfers/" + getJobId() + "/results/transferDetails"));
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Export data from the space
     */
    private void pushFromVoSpace() throws UWSException {
	// Request details
	try {
	    String target = transfer.getTarget();
	    Node node = getNode(target);
	    // Perform data transfer
	    performTransfer(node);
	} catch (Exception e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Import data into the space
     */
    private void pullToVoSpace() throws UWSException {
	// Request details
	Node node = null;
	try {
	    String target = transfer.getTarget();
	    // Create node (if necessary)
	    if (!store.isStored(target)) {
		Node blankNode = factory.getDefaultNode();
		blankNode.setUri(target);
		node = manager.create(blankNode, false);
	    } else {
		node = getNode(target);
	    }
	    // Negotiate protocol details
	    completeProtocols(target, ProtocolHandler.CLIENT);
	    // Set node status to busy
	    setNodeStatus(node, STATUS_BUSY);
	    // Perform data transfer
	    performTransfer(node);
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Request a URL to retrieve data from the space
     */
    private void pullFromVoSpace() throws UWSException {
	try {
	    // Request details
	    String target = transfer.getTarget();
	    // Negotiate protocol details
	    completeProtocols(target, ProtocolHandler.SERVER);
	    // Register transfer endpoints
	    registerEndpoint();
	    // Add to results
	    store.addResult(getJobId(), transfer.toString());
	    getJob().addResult(new Result(getJob(), "transferDetails", manager.BASE_URL + "transfers/" + getJobId() + "/results/transferDetails"));
	} catch (Exception e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /** 
     * Get the job id associated with this thread
     * @return jobId The job id for this task
     */
    private String getJobId() {
	return getJob().getJobId();
    }


    /**
     * Check whether the specified file has been modified within the past hour
     * @param location The file to check
     * @param start The start point of the hour long period
     * @return whether the file has been modified with the past hour
     */
    private boolean checkLocation(String location, long start) throws VOSpaceException {
	// Is the hour up?
	boolean changed = false;
	if (System.currentTimeMillis() - start < 3600000) {
	    // Any activity with the past five seconds?
	    //long lastModified = location.lastModified();
	    long lastModified = backend.lastModified(location); 
	    if (lastModified > start && System.currentTimeMillis() - lastModified > 5000) changed = true;
	} else {
	    changed = true;
	}
	return changed;
    }

    /**
     * Check whether it's been an hour
     * @param start The start point of the hour long period
     * @return whether the file has been modified with the past hour
     */
    private boolean checkTime(long start) throws UWSException {
	// Is the hour up?
	boolean changed = false;
	long diff = System.currentTimeMillis() - start;
	if (diff > 3600000) changed = true;
	if ((diff%5000) < 1000) {
	    try {
		if (store.isCompleted(getJobId())) changed = true;
	    } catch (SQLException e) {
		throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	    }
	}
	return changed;
    }

    /**
     * Move from the specified target to the direction
     */
    private void moveNode() throws VOSpaceException, SQLException, UWSException {
	// Request details
	String target = transfer.getTarget();
	String direction = transfer.getDirection();
	// Get node
	String[] result = store.getData(new String[] {target}, null, 0);
	if (result.length == 0) throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Node not in metastore");
	Node node = null;
	for (String item: result) {
	    node = factory.getNode(item);
	}
	// Check whether endpoint is reserved URI
	if (direction.endsWith(".null")) {
	    manager.delete(target);
	} else {
	    if (direction.endsWith(".auto")) direction = generateUri(direction, ".auto"); 
	    // Check whether endpoint is a container
	    if (isContainer(direction)) direction += target.substring(target.lastIndexOf("/"));
	    // Change identifier
	    node.setUri(direction);
	    // Move bytes
	    String newLocation = getLocation(direction);
	    if (!moveBytes(store.getLocation(target), newLocation)) throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Unable to move bytes between target and direction");
	    // Store update node
	    store.updateData(target, direction, newLocation, node.toString());
	    // Check if target is a container
	    if (node instanceof ContainerNode) {
	    // Move directory
//	    try {
//		System.err.println(target + " " + direction);
//		backend.moveBytes(target, direction);
//	    } catch (Exception e) {
//		throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
//	    }
		// Update metadata
		for (String child: store.getAllChildren(target)) {
		    // Update uri
		    Node childNode = manager.getNode(child, "max", 1);
		    childNode.setUri(child.replace(target, direction));
		    // Get new location
		    newLocation = getLocation(childNode.getUri());
		    // Store moved node
		    store.updateData(child, childNode.getUri(), getLocation(childNode.getUri()), childNode.toString());
		}
	    }
	}
	// Update job results with details
//	if (direction.endsWith(".auto")) {
//	  
//	}

    }

    /**
     * Copy from the specified target to the direction
     */
    private void copyNode() throws VOSpaceException, SQLException, UWSException {
	// Request details
	String target = transfer.getTarget();
	String direction = transfer.getDirection();
	// Get node
	String[] result = store.getData(new String[] {target}, null, 0);
	if (result.length == 0) throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Node not in metastore");
	Node node = null;
	for (String item: result) {
	    node = factory.getNode(item);	
	}
	// Check whether endpoint is reserved URI
	if (direction.endsWith(".null")) {
	    manager.delete(target);
	} else {
	    if (direction.endsWith(".auto")) direction = generateUri(direction, ".auto"); 
	    // Check whether endpoint is a container
	    if (isContainer(direction)) direction += target.substring(target.lastIndexOf("/"));
	    // Change identifier
	    node.setUri(direction);
	    // Copy bytes
	    String newLocation = getLocation(direction);
	    if (!copyBytes(store.getLocation(target), newLocation)) throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Unable to move bytes between target and direction");
	    // Store new node
	    store.storeData(direction, getType(node.getType()), USER, newLocation, node.toString());
	    // Check if target is a container
	    if (node instanceof ContainerNode) {
		// Move directory
		try {
		    //FileUtils.copyDirectory(new File(new URI(target)), new File(new URI(direction)));
		    backend.copyBytes(store.getLocation(target), newLocation);
		} catch (Exception e) {
		    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
		}
		// Update metadata
		for (String child: store.getAllChildren(target)) {
		    // Update uri
		    Node childNode = manager.getNode(child, "max", 1);
		    childNode.setUri(child.replace(target, direction));
		    // Get new location
		    newLocation = getLocation(childNode.getUri());
		    // Store copy node
		    store.storeData(childNode.getUri(), getType(childNode.getType()), USER, getLocation(childNode.getUri()), childNode.toString());
		}
	    }
	}
    }

    /**
     * Generate a URI replacing the specified part
     * @param uri The static part of the URI
     * @param remove The part of the URI to replace with an autogenerated part
     * @return the new URI
     */
    private String generateUri(String uri, String remove) {
	String newUri = uri.substring(0, uri.length() - remove.length()) + UUID.randomUUID().toString();
	return newUri;
    }

    /**
     * Check whether the specified uri is a container
     * @return whether the specified uri is a container
     */
    private boolean isContainer(String uri) throws SQLException {
	return store.getType(uri) == NodeType.CONTAINER_NODE.ordinal();
    }

    /**
     * Get a location for an object
     * @param identifier The identifier for the object whose location is sought
     * @return the location for the specified object
     */
    private String getLocation(String identifier) {
	String name = identifier.substring(identifier.lastIndexOf("!"));
        String dataname = name.substring(name.indexOf("/") + 1);
        return manager.BASEURI + "/" + dataname; 
    }

    /**
     * Move the bytes from the old location to the new location
     * @param oldLocation The location from which bytes are to be moved
     * @param newLocation The location to which bytes are to be moved
     * @return whether the move operation has been successful or not
     */
    private boolean moveBytes(String oldLocation, String newLocation) throws UWSException {
	boolean success = false;
	try {
	    //	    FileUtils.moveFile(new File(new URI(oldLocation)), new File(new URI(newLocation)));
	    backend.moveBytes(oldLocation, newLocation);
	    success = true;
	} catch (Exception e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
        }
	return success;
    }

    /**
     * Copy the bytes from the old location to the new location
     * @param oldLocation The location from which bytes are to be copied
     * @param newLocation The location to which bytes are to be copied
     * @return whether the copy operation has been successful or not
     */
    private boolean copyBytes(String oldLocation, String newLocation) throws UWSException {
	boolean success = false;
	try {
	    //	    FileUtils.copyFile(new File(new URI(oldLocation)), new File(new URI(newLocation)));
	    backend.copyBytes(oldLocation, newLocation);
	    success = true;
	} catch (Exception e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
        }
	return success;
    }

    /**
     * Get the NodeType integer for the node name
     * @param type The name of the node type
     * @return the ordinal value of the equivalent NodeType type
     */
    private int getType(String type) {
	if (type.equals("vos:Node")) {
	    return NodeType.NODE.ordinal();
	} else if (type.equals("vos:DataNode")) {
	    return NodeType.DATA_NODE.ordinal();
	} else if (type.equals("vos:ContainerNode")) {
	    return NodeType.CONTAINER_NODE.ordinal();
	} else if (type.equals("vos:UnstructuredDataNode")) {
	    return NodeType.UNSTRUCTURED_DATA_NODE.ordinal();
	} else if (type.equals("vos:StructuredDataNode")) {
	    return NodeType.STRUCTURED_DATA_NODE.ordinal();
	} else if (type.equals("vos:LinkNode")) {
	    return NodeType.LINK_NODE.ordinal();
	}
	return -1;
    }

    /**
     * Check that there is at least one supported protocol in the supplied list
     * @param request The list of protocols from the user
     * @param service The list of protocols that the service supports
     */
    private void checkProtocols(Protocol[] request, ArrayList<Protocol> service) throws UWSException {
	SUPPORTED = new ArrayList<String>();
	try {
	    ArrayList<String> uris = new ArrayList<String>();
	    for (Protocol reqProtocol : request) {
		uris.add(reqProtocol.getURI());
	    }
	    for (Protocol serProtocol : service) {
		if (uris.contains(serProtocol.getURI())) SUPPORTED.add(serProtocol.getURI());
	    }
	} catch (Exception e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	}
	if (SUPPORTED.size() == 0) throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "The service supports none of the requested Protocols");
    }

    /**
     * Fill in the operational details of any protocols to use in the supplied list
     * @param nodeUri The URI of the node involved in the data transfer
     */
    private void completeProtocols(String nodeUri, int mode) throws UWSException {
	try {
	    Protocol[] tranProtocols = transfer.getProtocol();
	    transfer.clearProtocols();
	    for (Protocol protocol : tranProtocols) {
		if (SUPPORTED.contains(protocol.getURI())) {
		    ProtocolHandler handler = manager.PROTOCOLS.get(protocol.getURI());
		    protocol = handler.admin(nodeUri, protocol, mode);
		    transfer.addProtocol(protocol);
		}
	    }
	} catch (Exception e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Get the specified node
     * @param identifier The id of the node to retrieve
     * @return the retrieved node
     */
    public Node getNode(String identifier) throws UWSException {
	Node node = null;
	try {
	    String[] result = store.getData(new String[] {identifier}, null, 0);
	    for (String item: result) {
		node = factory.getNode(item);
	    } 
	} catch (SQLException e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	} catch (VOSpaceException e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	}
	return node;
    }

    /*
     * Set the status for the node
     */
    private void setNodeStatus(Node node, boolean busy) throws UWSException {
        try {
            if (node instanceof DataNode) ((DataNode) node).setBusy(busy);
	    store.setStatus(node.getUri(), busy);
	    store.updateData(node.getUri(), node.toString());
	} catch (SQLException e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	} catch (VOSpaceException e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	}
    }

    /**
     * Register the transfer endpoints
     */
    private void registerEndpoint() throws UWSException {
	try {
	    String jobId = getJobId();
	    for (Protocol protocol : transfer.getProtocol()) {
		store.storeTransfer(jobId, protocol.getEndpoint()); 
	    }
	} catch (Exception e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	}
    }
    

    /**
     * Perform the transfer using the negotiated protocols
     * @param node The node associated with the data transfer
     */
    private void performTransfer(Node node) throws UWSException {
	// Loop through negotiated protocols until one works
	boolean success = false;
	try {
	    for (Protocol protocol : transfer.getProtocol()) {
		ProtocolHandler handler = manager.PROTOCOLS.get(protocol.getURI());
	        if (handler.invoke(protocol, getLocation(node.getUri()), backend)) {
	            success = true;
		    break;
	        }
	    }
	} catch (IOException e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	} catch (VOSpaceException e) {
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e);
	}	
	if (!success) throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "None of the requested protocols was successful");
    }


    /**
     * Trigger the specified capability on the specified container
     * @param identifier The identifier of the target node
     * @param capability The identifier of the parent capability to trigger
     * @param port The port number to send the notification to
     */
    private void trigger(String identifier, String capability, int port) throws UWSException {
	try {
	    Capability cap = manager.CAPABILITIES.get(capability);
	    cap.invoke(getLocation(identifier));
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "The capability " + capability + " was unable to complete on node " + identifier);
	}
    }

}
