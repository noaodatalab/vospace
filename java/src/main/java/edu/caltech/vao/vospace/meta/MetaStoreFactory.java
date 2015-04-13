/**
 * MetaStoreFactory.java
 * Author: Matthew Graham (Caltech)
 * Version: Original (0.1) - 26 June 2006
 */

package edu.caltech.vao.vospace.meta;

import java.lang.reflect.*;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import edu.caltech.vao.vospace.VOSpaceException;

/** 
 * This class presents a factory for creating MetaStores
 */
public class MetaStoreFactory {

    private static MetaStoreFactory ref;
    private Map stores;
    static Properties props;

    /* 
     * Construct a basic MetaStoreFactory: load the properties file and
     * initialize the db
     */
    private MetaStoreFactory(Properties props)  {
	try {
	    this.props = props;
	    stores = new HashMap<String, String>();
	    Enumeration<String> storeTypes = (Enumeration<String>) props.propertyNames();
	    while (storeTypes.hasMoreElements()) {
	        String storeType = storeTypes.nextElement();
	        if (storeType.startsWith("store.type.")) {
		    register(storeType, props.getProperty(storeType));
		}
            }
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
    }

    /*
     * Get a MetaStoreFactory
     */
    public static MetaStoreFactory getInstance(String propFile) {
	try {
	    props = new Properties();
 	    props.load(new FileInputStream(propFile));
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
        if (ref == null) ref = new MetaStoreFactory(props);
	return ref;
    }

    /*
     * Get a MetaStoreFactory
     */
    public static MetaStoreFactory getInstance(Properties props) {
        if (ref == null) ref = new MetaStoreFactory(props);
	return ref;
    }

    /*
     * Get a MetaStoreFactory
     */
    public static MetaStoreFactory getInstance() throws VOSpaceException {
	if (ref == null) throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, "Store factory cannot be initialized.");
	return ref;
    }

    /*
     * Get a MetaStore
     */
    public MetaStore getMetaStore(String type) {
	MetaStore ms = null;
	try {
	    ms = (MetaStore) Class.forName((String) stores.get(type)).getConstructor(Properties.class).newInstance(props);
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
        return ms;
    }

    /*
     * Get a MetaStore
     */
    public MetaStore getMetaStore() {
	try {
	    String type = props.getProperty("store.type");
	    return getMetaStore("store.type." + type);
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
	return null;
    }

    /*
     * Register a MetaStore
     */
    public void register(String name, String type) {
	stores.put(name, type);
    }

    /*
     * Deregister a MetaStore
     */
    public void deregister(String name) {
	stores.remove(name);
    }

    /*
     * Prevent cloning
     */
    public Object clone() throws CloneNotSupportedException {
	throw new CloneNotSupportedException();
    }
}
