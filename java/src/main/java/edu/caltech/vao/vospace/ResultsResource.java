
package edu.caltech.vao.vospace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.caltech.vao.vospace.meta.MetaStore;
import edu.caltech.vao.vospace.meta.MetaStoreFactory;

@Path("results")
public class ResultsResource extends VOSpaceResource {

    public ResultsResource() throws VOSpaceException {
	super();
    }

    /**
     * This method retrieves the specified data.
     * 
     * @param jobid The identifier for the data to return.
     * @return the specified data
     */
    @Path("{jobid}")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public String getDetails(@PathParam("jobid") String jobid) throws VOSpaceException {
	try {
	    MetaStore store = MetaStoreFactory.getInstance().getMetaStore();
            String details = store.getResult(jobid);
	    return details;
	} catch (Exception e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
	}
    }

}
