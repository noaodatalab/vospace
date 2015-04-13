
package edu.caltech.vao.vospace;

import java.util.EnumMap;

public class Views {

    public enum View { 
	ANY, BINARY, DEFAULT, VOTABLE, JPEG, PNG, FITS, CSV, VOTABLE_1_1, FITS_TABLE, TAR 
    }
    
    private static EnumMap<View, String> viewMap; 

    static {
	viewMap = new EnumMap<View, String>(View.class);
	viewMap.put(View.ANY, "ivo://ivoa.net/vospace/core#anyview");
	viewMap.put(View.BINARY, "ivo://ivoa.net/vospace/core#binaryview");
	viewMap.put(View.DEFAULT, "ivo://ivoa.net/vospace/core#defaultview");
	viewMap.put(View.VOTABLE, "ivo://ivoa.net/vospace/core#votable");
	viewMap.put(View.JPEG, "ivo://ivoa.net/vospace/views#jpeg");
	viewMap.put(View.PNG, "ivo://ivoa.net/vospace/views#png");
	viewMap.put(View.FITS, "ivo://ivoa.net/vospace/views#fits");
	viewMap.put(View.CSV, "ivo://ivoa.net/vospace/views#csv");
	viewMap.put(View.VOTABLE_1_1, "ivo://ivoa.net/vospace/views#votable-1.1");
	viewMap.put(View.FITS_TABLE, "ivo://ivoa.net/vospace/views#fits-table");
	viewMap.put(View.TAR, "ivo://ivoa.net/vospace/views#tar");
    }

    public static String get(View view) {
	return viewMap.get(view);
    }

    public static View fromString(String str) {
	for (View view: View.values()) {
	    if (view.toString().equals(str.trim())) 
		return view;
	}
	return null;
    }

    public static View fromValue(String str) {
	for (View view: View.values()) {
	    if (viewMap.get(view).equals(str.trim())) 
		return view;
	}
	return null;
    }

}