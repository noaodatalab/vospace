
package edu.caltech.vao.vospace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.URI;
import org.apache.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.caltech.vao.vospace.meta.MetaStore;
import edu.caltech.vao.vospace.meta.MetaStoreFactory;

@Path("results")
public class ResultsResource extends VOSpaceResource {

    private static Logger log = Logger.getLogger(ResultsResource.class);

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
    public String getDetails(@PathParam("jobid") String jobid, @HeaderParam("X-DL-AuthToken") String authToken) throws VOSpaceException {
    log.info("getDetails[jobID=" + jobid + "]");
	manager.validateToken(authToken);
	try {
	    MetaStore store = MetaStoreFactory.getInstance().getMetaStore();
            String details = store.getResult(jobid);
	    return details;
    } catch (VOSpaceException ve) {
        throw ve;
	} catch (Exception e) {
	    throw new VOSpaceException(e, jobid);
	}
    }

}
