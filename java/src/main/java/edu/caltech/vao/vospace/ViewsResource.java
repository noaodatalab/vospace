
package edu.caltech.vao.vospace;

import java.util.ArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.caltech.vao.vospace.xml.*;

@Path("views")
public class ViewsResource extends VOSpaceResource {

    public ViewsResource() throws VOSpaceException {
        super();
    }

    /**
    * This method retrieves the list of supported views
    *
    * @return the list of supported views
    */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public String getViews() throws VOSpaceException {
	StringBuffer sbuf = new StringBuffer("<views xmlns=\"http://www.ivoa.net/xml/VOSpace/v2.0\"><accepts>");
	addViews(sbuf, manager.SPACE_ACCEPTS_IMAGE);
	addViews(sbuf, manager.SPACE_ACCEPTS_TABLE);
	addViews(sbuf, manager.SPACE_ACCEPTS_ARCHIVE);
	sbuf.append("</accepts><provides>");
	addViews(sbuf, manager.SPACE_PROVIDES_IMAGE);
	addViews(sbuf, manager.SPACE_PROVIDES_TABLE);
	addViews(sbuf, manager.SPACE_PROVIDES_ARCHIVE);
	sbuf.append("</provides></views>");
	return sbuf.toString();
    }

    public void addViews(StringBuffer sbuf, ArrayList<Views.View> list) {
	for (Views.View view: list) {
	    sbuf.append("<view uri=\"" + Views.get(view) + "\"/>");
	}	
    }

}