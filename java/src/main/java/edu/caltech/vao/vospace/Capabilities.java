
package edu.caltech.vao.vospace;

import java.util.EnumMap;

public class Capabilities {

    public enum Capability {  
    }
    
    private static EnumMap<Capability, String> capMap; 

    static {
	capMap = new EnumMap<Capability, String>(Capability.class);
	//	capMap.put(View.ANY, "ivo://ivoa.net/vospace/core#anyview");
    }

    public static String get(Capability cap) {
	return capMap.get(cap);
    }

    public static Capability fromString(String str) {
	for (Capability cap: Capability.values()) {
	    if (cap.toString().equals(str.trim())) 
		return cap;
	}
	return null;
    }
}