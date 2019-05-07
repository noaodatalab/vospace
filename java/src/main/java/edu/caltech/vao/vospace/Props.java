package edu.caltech.vao.vospace;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Properties;
import java.io.FileInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.BooleanUtils;

public class Props {
    // Some commonly used Properties
    public static String GROUPREAD_URI;
    public static String GROUPWRITE_URI;
    public static String PUBLICREAD_URI;
    public static String ISPUBLIC_URI;     // Legacy publicread; kept same as ispublic whenever possibl
    public static String LENGTH_URI;
    public static String MD5_URI;
    public static String DATE_URI;
    public static String BTIME_URI;   // creation time
    public static String CTIME_URI;   // metadata mod time
    public static String MTIME_URI;   // data mod time

    public static void initialize(String propFile) throws VOSpaceException {
        try {
            // Get property file
            Properties props = new Properties();
            props.load (new FileInputStream(propFile));
            for (String k: props.stringPropertyNames()) {
                String[] s_attrs = StringUtils.split(props.getProperty(k), ',');
                if (s_attrs.length == 4) {
                    boolean[] attrs = new boolean[4];
                    for (int i=0; i < 4; i++) { attrs[i] = Boolean.parseBoolean(s_attrs[i]); }
                    propertyURIs.put(k, "ivo://ivoa.net/vospace/core#" + k);
                    propertyAttrs.put(k, attrs);
                }
            }
        } catch (Exception e) {
            throw new VOSpaceException(e);
        }
        GROUPREAD_URI = getURI("groupread");
        GROUPWRITE_URI = getURI("groupwrite");
        PUBLICREAD_URI = getURI("publicread");
        ISPUBLIC_URI = getURI("ispublic");
        LENGTH_URI = getURI("length");
        MD5_URI = getURI("MD5");
        DATE_URI = getURI("date");
        BTIME_URI = getURI("btime");
        CTIME_URI = getURI("ctime");
        MTIME_URI = getURI("mtime");
    }

    private static HashMap<String, String> propertyURIs = new HashMap();
    private static HashMap<String, boolean[]> propertyAttrs = new HashMap();

    private Props() {}

    public static boolean isIvoaProp(String shortName) {
        return propertyURIs.containsKey(shortName);
    }

    public static String[] allProps() {
        return propertyURIs.keySet().toArray(new String[0]);
    }

    public static String getURI(String propName) {
        return propertyURIs.get(propName);
    }

    public static String fromURI(String uri) {
        for (String k: propertyURIs.keySet()) {
            if (getURI(k).equals(uri.trim())) return k;
        }
        return null;
    }

    public static int getAttributes(String propName) {
        boolean[] attrs = propertyAttrs.get(propName);
        if (attrs != null) {
            return BooleanUtils.toInteger(attrs[0]) + 2 * BooleanUtils.toInteger(attrs[1])
                   + 4 * BooleanUtils.toInteger(attrs[2]);
        } else return -1;
    }

    public static boolean isReadOnly(String propName) {
        boolean[] attrs = propertyAttrs.get(propName);
        return (attrs != null) ? propertyAttrs.get(propName)[3] : false;
    }
}
