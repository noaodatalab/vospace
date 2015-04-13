/**
 * StorageManagerFactory.java
 * Author: Matthew Graham (Caltech)
 * Version: Original (0.1) - 20 April 2011
 */

package edu.caltech.vao.vospace.storage;

import java.lang.reflect.*;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import edu.caltech.vao.vospace.VOSpaceException;

/** 
 * This class presents a factory for creating StorageManagers
 */
public class StorageManagerFactory {

    private static StorageManagerFactory ref;
    private Map backends;
    static Properties props;

    /* 
     * Construct a basic StorageManagerFactory: load the properties file 
     */
    private StorageManagerFactory(Properties props)  {
	try {
	    this.props = props;
	    backends = new HashMap<String, String>();
	    Enumeration<String> backendTypes = (Enumeration<String>) props.propertyNames();
	    while (backendTypes.hasMoreElements()) {
	        String backendType = backendTypes.nextElement();
	        if (backendType.startsWith("backend.type.")) {
		    register(backendType, props.getProperty(backendType));
		}
            }
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
    }

    /*
     * Get a StorageManagerFactory
     */
    public static StorageManagerFactory getInstance(String propFile) {
	try {
	    props = new Properties();
 	    props.load(new FileInputStream(propFile));
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
        if (ref == null) ref = new StorageManagerFactory(props);
	return ref;
    }

    /*
     * Get a StorageManagerFactory
     */
    public static StorageManagerFactory getInstance(Properties props) {
        if (ref == null) ref = new StorageManagerFactory(props);
	return ref;
    }

    /*
     * Get a StorageManagerFactory
     */
    public static StorageManagerFactory getInstance() throws VOSpaceException {
	if (ref == null) throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, "Store factory cannot be initialized.");
	return ref;
    }

    /*
     * Get a StorageManager
     */
    public StorageManager getStorageManager(String type) {
	StorageManager ms = null;
	try {
	    ms = (StorageManager) Class.forName((String) backends.get(type)).getConstructor(Properties.class).newInstance(props);
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
        return ms;
    }

    /*
     * Get a StorageManager
     */
    public StorageManager getStorageManager() {
	try {
	    String type = props.getProperty("backend.type");
	    return getStorageManager("backend.type." + type);
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
	return null;
    }

    /*
     * Register a StorageManager
     */
    public void register(String name, String type) {
	backends.put(name, type);
    }

    /*
     * Deregister a StorageManager
     */
    public void deregister(String name) {
	backends.remove(name);
    }

    /*
     * Prevent cloning
     */
    public Object clone() throws CloneNotSupportedException {
	throw new CloneNotSupportedException();
    }
}
