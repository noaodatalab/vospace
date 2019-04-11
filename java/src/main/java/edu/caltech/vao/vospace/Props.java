package edu.caltech.vao.vospace;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Properties;
import java.io.FileInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.BooleanUtils;

public class Props {
    // Some commonly used Properties
    public static String GROUPREAD = "groupread";
    public static String GROUPWRITE = "groupwrite";
    public static String PUBLICREAD = "publicread";
    public static String ISPUBLIC = "ispublic";     // Legacy publicread; kept same as ispublic whenever possible
    public static String LENGTH = "length";
    public static String MD5 = "MD5";
    public static String DATE = "date";
    public static String BTIME = "btime";   // creation time
    public static String CTIME = "ctime";   // metadata mod time
    public static String MTIME = "mtime";   // data mod time

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
        System.out.println(allProps());
    }

    private static HashMap<String, String> propertyURIs = new HashMap();
    private static HashMap<String, boolean[]> propertyAttrs = new HashMap();

    private Props() {}

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
