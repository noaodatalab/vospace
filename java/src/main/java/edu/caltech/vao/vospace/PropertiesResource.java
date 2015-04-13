
package edu.caltech.vao.vospace;

import java.util.ArrayList;

import java.sql.SQLException;
import java.sql.ResultSet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.caltech.vao.vospace.meta.MetaStore;
import edu.caltech.vao.vospace.xml.*;

@Path("properties")
public class PropertiesResource extends VOSpaceResource {

    public PropertiesResource() throws VOSpaceException {
        super();
    }

    /**
    * This method retrieves the list of properties
    *
    * @return the list of properties
    */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public String getProperties() throws VOSpaceException {
	try {
	    MetaStore store = manager.getMetaStore();
	    StringBuffer sbuf = new StringBuffer("<properties xmlns=\"http://www.ivoa.net/xml/VOSpace/v2.0\"><accepts>");
	    ResultSet props = store.getProperties(manager.PROPERTIES_SPACE_ACCEPTS);
	    addProperties(sbuf, props);
	    sbuf.append("</accepts><provides>");
	    props = store.getProperties(manager.PROPERTIES_SPACE_PROVIDES);
	    addProperties(sbuf, props);
	    sbuf.append("</provides><contains>");
	    props = store.getProperties(manager.PROPERTIES_SPACE_CONTAINS);
	    addProperties(sbuf, props);
	    sbuf.append("</contains></properties>");
	    return sbuf.toString();
	} catch (SQLException e) {
	    throw new VOSpaceException(e.getMessage());
	}
    }

    public void addProperties(StringBuffer sbuf, ResultSet list) throws VOSpaceException {
	try {
	    while (list.next()) {
		sbuf.append("<property uri=\"" + list.getString("identifier") + "\"/>");
	    }
	} catch (SQLException e) {
	    throw new VOSpaceException(e.getMessage());
	}
    }

}