/**
 * TransformEngine.java
 * Author: Matthew Graham (Caltech)
 * Version: Original (0.1) - 19 June 2007
 *          0.2 - 10 April 2008 - Added tar/tar.gz support
 */

package edu.caltech.vao.vospace.view;

import edu.caltech.vao.vospace.meta.MetaStore;

import uk.ac.starlink.table.*;

import ij.*;
import ij.io.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Properties;

/**
 * This class creates a copy of a data object in a different format
 */
public class TransformEngine {

    private HashMap<String, String> TABLE_FORMATS = new HashMap<String, String>();
    private HashMap<String, String> IMAGE_FORMATS = new HashMap<String, String>();
    private HashMap<String, String> ARCHIVE_FORMATS = new HashMap<String, String>();
    public HashMap<String, String> VIEWS = new HashMap<String, String>();
    private String stagingLocation;
    private MetaStore meta;
    private Properties props;

    public TransformEngine(String stagingLocation) {
	TABLE_FORMATS.put("ivo://ivoa.net/vospace/views/tabular#votable-1.0", "VOTable");
	TABLE_FORMATS.put("ivo://ivoa.net/vospace/views/tabular#votable-1.1", "VOTable");
	TABLE_FORMATS.put("ivo://ivoa.net/vospace/views/tabular#fits-table", "FITS");
	TABLE_FORMATS.put("ivo://ivoa.net/vospace/views/tabular#ascii", "ASCII");
	TABLE_FORMATS.put("ivo://ivoa.net/vospace/views/tabular#csv", "CSV");
	TABLE_FORMATS.put("ivo://ivoa.net/vospace/views/tabular#ipac", "IPAC");
	TABLE_FORMATS.put("ivo://ivoa.net/vospace/views/tabular#wdc", "WDC");
	IMAGE_FORMATS.put("ivo://ivoa.net/vospace/views/image#fits", "FITS");
	IMAGE_FORMATS.put("ivo://ivoa.net/vospace/views/image#jpeg", "JPEG");
	IMAGE_FORMATS.put("ivo://ivoa.net/vospace/views/image#gif", "GIF");
	IMAGE_FORMATS.put("ivo://ivoa.net/vospace/views/image#ppm", "PPM");
	IMAGE_FORMATS.put("ivo://ivoa.net/vospace/views/image#png", "PNG");
	IMAGE_FORMATS.put("ivo://ivoa.net/vospace/views/image#tiff", "TIFF");
        ARCHIVE_FORMATS.put("ivo://ivoa.net/vospace/views/archive#plain", "PLAIN");
	ARCHIVE_FORMATS.put("ivo://ivoa.net/vospace/views/archive#tar", "TAR");
	ARCHIVE_FORMATS.put("ivo://ivoa.net/vospace/views/archive#tar-gz", "TAR_GZ");
	this.stagingLocation = stagingLocation;
	for (String view : TABLE_FORMATS.keySet()) {
	    VIEWS.put(TABLE_FORMATS.get(view), view);
	}
	for (String view : IMAGE_FORMATS.keySet()) {
	    VIEWS.put(IMAGE_FORMATS.get(view), view);
	}
	for (String view : ARCHIVE_FORMATS.keySet()) {
	    VIEWS.put(ARCHIVE_FORMATS.get(view), view);
	}
    }

    public TransformEngine(MetaStore meta, Properties props) {
	this(props.getProperty("space.staging_area"));
	this.meta = meta;
	this.props = props;
    }

    /*
     * Convert the object at the specified location from the given old format
     * to the given new format
     */
    public String transform(String location, String oldFormat, String newFormat) throws TableFormatException, IOException, URISyntaxException {
	String newLocation = stagingLocation + location.substring(location.lastIndexOf("/"));
	if (TABLE_FORMATS.containsKey(oldFormat)) {	
	    StarTable table = new StarTableFactory().makeStarTable(location, TABLE_FORMATS.get(oldFormat));
	    newLocation = new URL(newLocation).getPath() + "_" + TABLE_FORMATS.get(newFormat);
	    new StarTableOutput().writeStarTable(table, newLocation, TABLE_FORMATS.get(newFormat));
	} else if (IMAGE_FORMATS.containsKey(oldFormat)) {
	    Opener opener = new Opener();
	    ImagePlus image = opener.openImage(new URL(location).getPath());
	    FileSaver saver = new FileSaver(image);
	    newLocation = new URL(newLocation).getPath() + "_" + IMAGE_FORMATS.get(newFormat);
	    if (IMAGE_FORMATS.get(newFormat).equals("GIF")) {
		saver.saveAsGif(newLocation);
	    } else if (IMAGE_FORMATS.get(newFormat).equals("JPEG")) {
		saver.saveAsJpeg(newLocation);
	    } else if (IMAGE_FORMATS.get(newFormat).equals("PPM")) {
		saver.saveAsPgm(newLocation);
	    } else if (IMAGE_FORMATS.get(newFormat).equals("PNG")) {
		saver.saveAsPng(newLocation);
	    } else if (IMAGE_FORMATS.get(newFormat).equals("TIFF")) {
		saver.saveAsTiff(newLocation);
	    }
	    /* commented out for the moment
	} else if (ARCHIVE_FORMATS.containsKey(oldFormat)) {
	    TarHandler handler = new TarHandler(meta, props);
	    boolean compress = ARCHIVE_FORMATS.get(oldFormat).equals("TAR_GZ");
	    handler.load(newLocation, "file://" + location, compress);
	} else if (ARCHIVE_FORMATS.containsKey(newFormat)) {
	    TarHandler handler = new TarHandler();
	    newLocation = new URL(newLocation).getPath() + "_" + ARCHIVE_FORMATS.get(newFormat);
	    boolean compress = ARCHIVE_FORMATS.get(newFormat).equals("TAR_GZ");
	    handler.save(location, "file://" + newLocation, compress);
	    */
        } 
	return newLocation;
    }
}
