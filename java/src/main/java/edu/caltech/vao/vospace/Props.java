
package edu.caltech.vao.vospace;

import java.util.EnumMap;

public class Props {

    public enum Property { 
	TITLE, CREATOR, SUBJECT, DESCRIPTION, PUBLISHER, CONTRIBUTOR, DATE, TYPE, FORMAT,
	    IDENTIFIER, SOURCE, LANGUAGE, RELATION, COVERAGE, RIGHTS, AVAILABLE_SPACE, LENGTH, GROUPREAD, GROUPWRITE, ISPUBLIC
    }
    
    private static EnumMap<Property, String> propMap; 

    static {
	propMap = new EnumMap<Property, String>(Property.class);
	propMap.put(Property.TITLE, "ivo://ivoa.net/vospace/core#title");
	propMap.put(Property.CREATOR, "ivo://ivoa.net/vospace/core#creator");
	propMap.put(Property.SUBJECT, "ivo://ivoa.net/vospace/core#subject");
	propMap.put(Property.DESCRIPTION, "ivo://ivoa.net/vospace/core#description");
	propMap.put(Property.PUBLISHER, "ivo://ivoa.net/vospace/core#publisher");
	propMap.put(Property.CONTRIBUTOR, "ivo://ivoa.net/vospace/core#contributor");
	propMap.put(Property.DATE, "ivo://ivoa.net/vospace/core#date");
	propMap.put(Property.TYPE, "ivo://ivoa.net/vospace/core#type");
	propMap.put(Property.FORMAT, "ivo://ivoa.net/vospace/core#format");
	propMap.put(Property.IDENTIFIER, "ivo://ivoa.net/vospace/core#identifier");
	propMap.put(Property.SOURCE, "ivo://ivoa.net/vospace/core#source");
	propMap.put(Property.LANGUAGE, "ivo://ivoa.net/vospace/core#language");
	propMap.put(Property.RELATION, "ivo://ivoa.net/vospace/core#relation");
	propMap.put(Property.COVERAGE, "ivo://ivoa.net/vospace/core#coverage");
	propMap.put(Property.RIGHTS, "ivo://ivoa.net/vospace/core#rights");
	propMap.put(Property.AVAILABLE_SPACE, "ivo://ivoa.net/vospace/core#availableSpace");
	propMap.put(Property.LENGTH, "ivo://ivoa.net/vospace/core#length");
	propMap.put(Property.GROUPREAD, "ivo://ivoa.net/vospace/core#groupread");
	propMap.put(Property.GROUPWRITE, "ivo://ivoa.net/vospace/core#groupwrite");
	propMap.put(Property.ISPUBLIC, "ivo://ivoa.net/vospace/core#ispublic");
    }

    public static String get(Property prop) {
	return propMap.get(prop);
    }

    public static Property fromString(String str) {
	for (Property prop: Property.values()) {
	    if (prop.toString().equals(str.trim())) 
		return prop;
	}
	return null;
    }
}
