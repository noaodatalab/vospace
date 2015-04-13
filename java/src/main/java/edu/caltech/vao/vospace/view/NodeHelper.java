
package edu.caltech.nvo.vospace.view;

import edu.caltech.nvo.vospace.ContainerNotFoundFaultMessage;
import edu.caltech.nvo.vospace.InternalFaultMessage;
import edu.caltech.nvo.vospace.InvalidUriFaultMessage;
import edu.caltech.nvo.vospace.LinkFoundFaultMessage;
import edu.caltech.nvo.vospace.VOSpaceHandler;

import net.ivoa.xml.voSpaceContractV11Rc1.ContainerNotFoundFaultDocument;
import net.ivoa.xml.voSpaceContractV11Rc1.InternalFaultDocument;
import net.ivoa.xml.voSpaceContractV11Rc1.InvalidUriFaultDocument;
import net.ivoa.xml.voSpaceContractV11Rc1.LinkFoundFaultDocument;

import net.ivoa.xml.voSpaceTypesV11Rc1.CapabilityType;
import net.ivoa.xml.voSpaceTypesV11Rc1.CapabilityListType;
import net.ivoa.xml.voSpaceTypesV11Rc1.ContainerNodeType;
import net.ivoa.xml.voSpaceTypesV11Rc1.ContainerNotFoundFaultType;
import net.ivoa.xml.voSpaceTypesV11Rc1.CopyNodeResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.CreateNodeResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.DataNodeType;
import net.ivoa.xml.voSpaceTypesV11Rc1.FindNodeType;
import net.ivoa.xml.voSpaceTypesV11Rc1.FindNodesResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.GetNodeResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.GetProtocolsResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.GetPropertiesResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.GetViewsResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.InvalidUriFaultType;
import net.ivoa.xml.voSpaceTypesV11Rc1.LinkNodeType;
import net.ivoa.xml.voSpaceTypesV11Rc1.LinkFoundFaultType;
import net.ivoa.xml.voSpaceTypesV11Rc1.ListNodesResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.MoveNodeResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.NodeListType;
import net.ivoa.xml.voSpaceTypesV11Rc1.NodeType;
import net.ivoa.xml.voSpaceTypesV11Rc1.PropertyListType;
import net.ivoa.xml.voSpaceTypesV11Rc1.PropertyReferenceListType;
import net.ivoa.xml.voSpaceTypesV11Rc1.PropertyReferenceType;
import net.ivoa.xml.voSpaceTypesV11Rc1.PropertyType;
import net.ivoa.xml.voSpaceTypesV11Rc1.ProtocolErrorType;
import net.ivoa.xml.voSpaceTypesV11Rc1.ProtocolListType;
import net.ivoa.xml.voSpaceTypesV11Rc1.ProtocolType;
import net.ivoa.xml.voSpaceTypesV11Rc1.PullToVoSpaceResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.PushToVoSpaceResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.SetNodeResponseType;
import net.ivoa.xml.voSpaceTypesV11Rc1.StructuredDataNodeType;
import net.ivoa.xml.voSpaceTypesV11Rc1.TransferType;
import net.ivoa.xml.voSpaceTypesV11Rc1.UnstructuredDataNodeType;
import net.ivoa.xml.voSpaceTypesV11Rc1.ViewListType;
import net.ivoa.xml.voSpaceTypesV11Rc1.ViewType;

import edu.caltech.nvo.vospace.meta.*;
import edu.caltech.nvo.vospace.protocol.ProtocolHandler;
import edu.caltech.nvo.vospace.view.TransformEngine;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;

public class NodeHelper {

    protected final static String NODE = "Node";
    protected final static String DATANODE = "DataNode";
    protected final static String UNSTRUCTURED_DATANODE = "UnstructuredDataNode";
    protected final static String STRUCTURED_DATANODE = "StructuredDataNode";
    protected final static String CONTAINER_NODE = "ContainerNode";
    protected final static String LINK_NODE = "LinkNode";

    private MetaStore store;


    private final static String NULL_NODE = ".auto";
    private final static String BASE = "file:///tmp";
    private final static boolean PRESERVE_DATA = false;
    private final static boolean OVERWRITE_DATA = true;
    private final static boolean STATUS_BUSY = true;
    private final static boolean STATUS_FREE = false;
    private final static int PROPERTIES_SPACE_ACCEPTS = 1;
    private final static int PROPERTIES_SPACE_PROVIDES = 2;
    private final static int PROPERTIES_SPACE_CONTAINS = 4;
    private final static String INHERITABLE_PROPERTY = "10";

    private ViewListType SPACE_ACCEPTS_IMAGE;
    private ViewListType SPACE_ACCEPTS_TABLE;
    private ViewListType SPACE_ACCEPTS_ARCHIVE;
    private ViewListType SPACE_PROVIDES_IMAGE;
    private ViewListType SPACE_PROVIDES_TABLE;
    private ViewListType SPACE_PROVIDES_ARCHIVE;
    private ProtocolType[] SPACE_CLIENT_PROTOCOLS;
    private ProtocolType[] SPACE_SERVER_PROTOCOLS; 
    private CapabilityListType SPACE_CAPABILITIES;
    private PropertyReferenceListType KNOWN_PROPERTIES;
    private String SPACE_AUTH;
    private HashMap<String, ProtocolHandler> protocols;
    private TransformEngine engine;
    private ViewListType ACCEPTS_ANY;
    private String BASEURI;
    private String USER;
    private String STAGING_LOCATION;
    boolean structure = false;
    XmlOptions options;

    public NodeHelper(MetaStore store, Properties props) {
	try {
	    this.store = store;
            BASEURI = props.containsKey("space.baseuri") ? props.getProperty("space.baseuri") : BASE; 
	    ACCEPTS_ANY = getViewList("ivo://net.ivoa.vospace/views/any");
            SPACE_AUTH = props.containsKey("space.identifier") ? getId(props.getProperty("space.identifier")) : "vos://nvo.caltech!vospace";
            SPACE_ACCEPTS_ARCHIVE = getViewList(props.getProperty("space.accepts.archive"));
            SPACE_PROVIDES_ARCHIVE = getViewList(props.getProperty("space.provides.archive"));
            SPACE_CAPABILITIES = getCapabilityList(props.getProperty("space.capabilities"));
	    // XmlOption settings
	    Map namespaces = new HashMap();
	    namespaces.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
	    namespaces.put("http://www.net.ivoa/xml/VOSpaceTypes-v1.0rc5", "vos");
            options = new XmlOptions();
	    options.setSaveSuggestedPrefixes(namespaces);
            options.setSaveInner();
	    options.setSaveAggressiveNamespaces();
        } catch (Exception e) {
	    e.printStackTrace();
        }
    }

    /*
     * Return an InternalFault with the specified arguments
     */
    private InternalFaultMessage getInternalFault(String message) {
        InternalFaultDocument faultDoc = InternalFaultDocument.Factory.newInstance();
        return new InternalFaultMessage(message, faultDoc);
    }

    /*
     * Return an InvalidUriFault with the specified arguments
     */
    private InvalidUriFaultMessage getInvalidUriFault(String uri) {
        InvalidUriFaultDocument faultDoc = InvalidUriFaultDocument.Factory.newInstance();
        InvalidUriFaultType fault = faultDoc.addNewInvalidUriFault();
        fault.setValue(uri);
        return new InvalidUriFaultMessage("Invalid URI", faultDoc);
    }

    /*
     * Return a LinkFoundFault with the specified arguments
     */
    private LinkFoundFaultMessage getLinkFoundFault(String uri) {
        LinkFoundFaultDocument faultDoc = LinkFoundFaultDocument.Factory.newInstance();
        LinkFoundFaultType fault = faultDoc.addNewLinkFoundFault();
        fault.setUri(uri);
        return new LinkFoundFaultMessage("Link found", faultDoc);
    }

    /*
     * Return a ContainerNotFoundFault with the specified arguments
     */
    private ContainerNotFoundFaultMessage getContainerNotFoundFault(String uri) {
        ContainerNotFoundFaultDocument faultDoc = ContainerNotFoundFaultDocument.Factory.newInstance();
        ContainerNotFoundFaultType fault = faultDoc.addNewContainerNotFoundFault();
        fault.setUri(uri);
        return new ContainerNotFoundFaultMessage("ContainerNot found", faultDoc);
    }

    /*
     * Create a node
     */
    protected boolean createNode(String type, String identifier) throws InvalidUriFaultMessage, InternalFaultMessage, ContainerNotFoundFaultMessage {
	boolean success = true;
	String uri = identifier;
        if (!validId(uri)) throw getInvalidUriFault(uri);
	if (!validPath(uri)) throw getContainerNotFoundFault(uri);
	DataNodeType node = null;
        try {
	    if (type.equals(UNSTRUCTURED_DATANODE)) {
		node = UnstructuredDataNodeType.Factory.newInstance();
		node.setAccepts(ACCEPTS_ANY);
	    } else if (type.equals(CONTAINER_NODE)) {
		node = ContainerNodeType.Factory.newInstance();
		node.setAccepts(ACCEPTS_ANY);
		ViewListType accepts = node.addNewAccepts();
	        for (int i = 0; i < SPACE_ACCEPTS_ARCHIVE.sizeOfViewArray(); i++) {
		    ViewType view = accepts.addNewView();
		    view.setUri(SPACE_ACCEPTS_ARCHIVE.getViewArray(i).getUri());
		}
		ViewListType provides = node.addNewProvides();
	        for (int i = 0; i < SPACE_PROVIDES_ARCHIVE.sizeOfViewArray(); i++) {
		    ViewType view = provides.addNewView();
		    view.setUri(SPACE_PROVIDES_ARCHIVE.getViewArray(i).getUri());
		}
            }
	    node.setUri(identifier);
	    // Set capabilities
	    if (SPACE_CAPABILITIES.sizeOfCapabilityArray() > 0) {
	        CapabilityListType capabilities = node.addNewCapabilities();
                for (int i = 0; i < SPACE_CAPABILITIES.sizeOfCapabilityArray(); i++) {
	            CapabilityType capability = capabilities.addNewCapability();
	            capability.setUri(SPACE_CAPABILITIES.getCapabilityArray(i).getUri());
	        }
	    }
            // Store node
//	    if (exists) {
//		store.updateData(node.getUri(), node.xmlText(options));
//	    } else {
                store.storeData(node.getUri(), type, USER, getLocation(node.getUri()), node.xmlText(options));
//	    }
	} catch (SQLException e) {
	    throw getInternalFault(e.getMessage());
	}
	return success;
    }

    /*
     * Generate a ViewListType from the CSV list of args
     */
    private ViewListType getViewList(String args) {
        ViewListType views = ViewListType.Factory.newInstance();
        if (args != null) {
            String[] types = args.split(",");
            for (String type : types) {
                ViewType view = views.addNewView();
                view.setUri(type.trim());
            }
        }
        return views;
    }

    /*
     * Generate a CapabilityListType from the CSV list of args
     */
    private CapabilityListType getCapabilityList(String args) {
        CapabilityListType capabilities = CapabilityListType.Factory.newInstance();
        if (args != null) {
            String[] types = args.split(",");
            for (String type : types) {
                CapabilityType capability = capabilities.addNewCapability();
                capability.setUri(type.trim());
            }
        }
        return capabilities;
    }


   
    /*
     * Get a location for an object
     */
    private String getLocation(String identifier) {
	String name = identifier.substring(identifier.lastIndexOf("!"));
        String dataname = name.substring(name.indexOf("/") + 1);
        return BASEURI + "/" + dataname; 
    }

    /*
     * Update a set of properties with the specified new set: the operation is
     * the union of existing values and new ones.
     */
    private PropertyListType updateProperties(PropertyListType oldProps, PropertyListType newProps) {
        PropertyListType propertyList = null;
	PropertyType[] oldProperties, newProperties;
        try {
            propertyList = PropertyListType.Factory.newInstance();
            oldProperties = (oldProps != null) ? oldProps.getPropertyArray() : new PropertyType[0];
            newProperties = (newProps != null) ? newProps.getPropertyArray() : new PropertyType[0];
            HashMap<String, PropertyType> properties = new HashMap<String, PropertyType>();
            // Go through old properties first
            for (PropertyType property: oldProperties) {
                properties.put(property.getUri(), property);
            }
            // Go through new properties
            int count = 0;
            for (PropertyType property: newProperties) {
                String propUri = property.getUri();
		checkProperty(propUri);
                if (properties.containsKey(propUri)) {
                    PropertyType existingProperty = properties.get(propUri);
                    // Assume that default is readonly=false
                    if ((existingProperty.isSetReadonly() && !existingProperty.getReadonly()) || !existingProperty.isSetReadonly()) {
                        existingProperty.setStringValue(property.getStringValue());
                    } else {
                        // Handle xsi:nill = true
                        if (newProps != null && newProps.isNilPropertyArray(count++)) {
                            properties.remove(propUri);
                        }
                    }
                } else {
                    properties.put(propUri, property);
                }
            }
            // Get updated union property list
            Collection<PropertyType> collection = properties.values();
            PropertyType[] unionProperties = (PropertyType[]) collection.toArray(new PropertyType[0]);
            propertyList.setPropertyArray(unionProperties);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return propertyList;
    }

    /*
     * Create a list of changed inheritable properties
     */
    private PropertyListType checkInheritableProperties(PropertyListType oldProps, PropertyListType newProps) {
	PropertyListType changedPropertyList = null;
	ArrayList<PropertyType> changedProperties = new ArrayList<PropertyType>();
	PropertyType[] oldProperties, newProperties;
	try {
	    oldProperties = (oldProps != null) ? oldProps.getPropertyArray() : new PropertyType[0];
	    newProperties = (newProps != null) ? newProps.getPropertyArray() : new PropertyType[0];
            HashMap<String, PropertyType> properties = new HashMap<String, PropertyType>();
            for (PropertyType property : oldProperties) {
                properties.put(property.getUri(), property);
            }
            for (PropertyType property : newProperties) {
                if (store.getPropertyType(property.getUri()).equals(INHERITABLE_PROPERTY) && !property.getStringValue().equals(properties.get(property.getUri()).getStringValue())) changedProperties.add(property);
            }
            changedPropertyList = PropertyListType.Factory.newInstance();
            changedPropertyList.setPropertyArray(changedProperties.toArray(new PropertyType[0]));
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
	return changedPropertyList;
    }

    /*
     * Set the specified property list with properties of the specified type
     */
    private void setProperties(PropertyReferenceListType properties, int type) throws SQLException {
	ResultSet result = store.getProperties(type);
	while (result.next()) {
	    PropertyReferenceType propRef = properties.addNewProperty();
	    propRef.setUri(result.getString(1));
	    if (result.getInt(2) == 0) {
	        propRef.setReadonly(false);
	    } else {
	        propRef.setReadonly(true);
	    }
        }
    }

    /*
     * Check whether the specified property is in the list of known properties
     */
    private void checkProperty(String uri) throws SQLException {
	PropertyReferenceType[] properties = KNOWN_PROPERTIES.getPropertyArray();
        if (properties.length == 0) {
            PropertyReferenceType newProperty = KNOWN_PROPERTIES.addNewProperty();
            newProperty.setUri(uri);
            store.addProperty(uri, PROPERTIES_SPACE_CONTAINS, false);
        } else {
            boolean newProp = true;
            for (PropertyReferenceType property : properties) {
                if (property.getUri().equals(uri)) {
                    newProp = false;
                    break;
                }
            }
            if (newProp) {
                PropertyReferenceType newProperty = KNOWN_PROPERTIES.addNewProperty();
                newProperty.setUri(uri);
                store.addProperty(uri, PROPERTIES_SPACE_CONTAINS, false);
            }
        }
    }

    /*
     * Get the subset of inheritable properties from a list of properties
     */
    private PropertyListType getInheritableProperties(PropertyListType props) throws SQLException {
	ArrayList<PropertyType> properties = new ArrayList<PropertyType>();
	for (PropertyType property : props.getPropertyArray()) {
	    if (store.getPropertyType(property.getUri()).equals(INHERITABLE_PROPERTY)) properties.add(property);
	}
	PropertyListType propList = PropertyListType.Factory.newInstance();
	propList.setPropertyArray(properties.toArray(new PropertyType[0]));
	return propList;
    }

    /*
     * Convert IVO identifier into VOSpace identifier
     */
    private String getId(String ivoid) {
        return ivoid.replace("/", "!").replace("ivo:!!", "vos://");
    }

    /*
     * Check identifier
     */
    private boolean validId(String id) {
        boolean validId = false;
        if ((id.contains("*") && id.lastIndexOf("/") < id.indexOf("*")) || (id.startsWith(SPACE_AUTH))) validId = true;
        return validId;
    }

    /* 
     * Check that path contains ContainerNodes and no LinkNodes
     */
    private boolean validPath(String uri) throws edu.caltech.nvo.vospace.InternalFaultMessage {
	boolean validPath = true;
	// vos://nvo.caltech!vospace/a/b/c/d/e
	String[] pathParts = uri.split("/");
	String baseUri = pathParts[0] + "//" + pathParts[2];
	String testUri = baseUri;
	int i = 3;
	while (validPath && i < pathParts.length - 1) {
	    testUri += "/" + pathParts[i];
	    try {
	        if (!store.getType(testUri).equals(CONTAINER_NODE)) validPath = false; 
	    } catch (SQLException e) {
	        getInternalFault(e.getMessage());
	    }
	    i++;
	}
	return validPath;
    }

}
