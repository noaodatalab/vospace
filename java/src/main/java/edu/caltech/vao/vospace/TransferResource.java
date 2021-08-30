
package edu.caltech.vao.vospace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.caltech.vao.vospace.meta.MetaStore;
import edu.caltech.vao.vospace.meta.MetaStoreFactory;

import edu.caltech.vao.vospace.resource.*;
import org.apache.log4j.Logger;
import uws.UWSException;
import uws.job.JobList;
import uws.job.UWSJob;
import uws.job.ExecutionPhase;
import uws.job.user.JobOwner;
import uws.job.user.DefaultJobOwner;
import uws.service.actions.UWSAction;
import uws.service.UserIdentifier;
import uws.service.UWSFactory;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.file.LocalUWSFileManager;

@Path("transfers")
public class TransferResource extends VOSpaceResource {

    private static Logger log = Logger.getLogger(NodeResource.class);

    private UWSService uws = null;

    public TransferResource() throws VOSpaceException {
	super();
    }

    /**
     * Retrieve a UWS to use
     *
     * @param req the HTTP Request
     * @return the BasicUWS associated with the request
     */
    private UWSService getUWS(HttpServletRequest req) throws UWSException {

	// Get the current servlet context
	ServletContext context = req.getSession(true).getServletContext();

	// Fetch the UWS from the current session:
	UWSService uws = (UWSService)context.getAttribute("UWSService");

	// Initialize our UWS:
	if (uws == null){

	    // Create the Universal Worker Service:
	    UWSFactory factory = new TransferJobFactory();
	    LocalUWSFileManager fileManager = new LocalUWSFileManager(new File("/tmp/uws"));
	    uws = new UWSService(factory, fileManager);
	    uws.setDescription("This UWS aims to manage one (or more) JobList(s) of Transfers." + "Transfer is a kind of Job dealing with a data transfer within a VOSpace");

	    // Attach the user identification method (based on the HTTP Header)
	    uws.setUserIdentifier(new UserIdentifier() {
		    private static final long serialVersionUID = 1L;

		    @Override
		    public JobOwner extractUserId(UWSUrl urlInterpreter, HttpServletRequest request) throws UWSException {
			String token = request.getHeader("X-DL-AuthToken");
			if (token == null) {
			    return null;
			} else {
			    return new DefaultJobOwner(token);
			}
		    }

		    public JobOwner restoreUser(String id, String pseudo, Map<String, Object> otherData) throws UWSException {
			return new DefaultJobOwner(id);
		    }
	        }
	    );

	    // Create the job list
	    uws.addJobList(new JobList("transfers"));
//	    uws.addJobList(new JobList("sync"));

	    // Add this UWS to the current session:
	    context.setAttribute("UWSService", uws);
	}
	return uws;
    }


    private void executeRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	try {
	    uws = getUWS(req);
	    boolean done = uws.executeRequest(req, resp);
	} catch (UWSException e) {
	    // Display properly the caught UWSException:
	    resp.sendError(e.getHttpErrorCode(), e.getMessage());
	}
    }


    /**
     * This method retrieves the specified transfer.
     *
     * @return the transfer JAXB object
     */
    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void getTransfer(@Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException {
	executeRequest(req, resp);
    }


    /**
     * This method retrieves the specified transfer.
     *
     * @param jobid The identifier for the transfer job to return.
     * @return the transfer JAXB object
     */
    @Path("{jobid}")
    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void getTransfer(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id, @HeaderParam("X-DL-AuthToken") String authToken) throws IOException {
    log.info("getTransfer[jobID:" + id + "]");
	validateToken(authToken, resp);
	executeRequest(req, resp);
    }

    /**
     * This method retrieves the specified transfer.
     *
     * @param jobid The identifier for the transfer job to return.
     * @return the transfer JAXB object
     */
    @Path("{jobid}/results")
    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void getResults(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id, @HeaderParam("X-DL-AuthToken") String authToken) throws IOException {
    log.info("getResults[jobID:" + id + "]");
	validateToken(authToken, resp);
	executeRequest(req, resp);
    }


    /**
     * This method retrieves details for the specified transfer.
     *
     * @param jobid The identifier for the transfer job to return.
     * @return the transfer JAXB object
     */
    @Path("{jobid}/results/transferDetails")
    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public String getResultsDetails(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id, @HeaderParam("X-DL-AuthToken") String authToken) throws VOSpaceException, IOException {
    log.info("getResultsDetails[jobID:" + id + "]");
	validateToken(authToken, resp);
	String details = null;
        try {
	    // Check job status first
	    UWSService uws = getUWS(req);
	    JobList jobs = uws.getJobList("transfers");
	    UWSJob job = jobs.getJob(id);
	    MetaStore store = MetaStoreFactory.getInstance().getMetaStore();
	    while (job.getPhase() == ExecutionPhase.EXECUTING && details == null) {
		Thread.sleep(100);
		details = store.getResult(id);
	    }
	    if (details == null) details = "<vos:transfer xmlns:vos=\"http://www.ivoa.net/xml/VOSpace/v2.0\"></vos:transfer>";
	    return details;
        } catch (VOSpaceException ve) {
            throw ve;
        } catch (Exception e) {
            throw new VOSpaceException(e);
        }
    }


    /**
     * This method launches a transfer job.
     *
     * @param transfer the transfer object to launch
     * @return a Response instance indicating that the transfer job was created
     */
    @POST
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void postTransfer(@Context HttpServletRequest req, @Context HttpServletResponse resp, @HeaderParam("X-DL-AuthToken") String authToken) throws IOException {
    log.info("postTransfer");
	validateToken(authToken, resp);
	executeRequest(req, resp);
    }


    /**
     * This method launches a transfer job.
     *
     * @param transfer the transfer object to launch
     * @return a Response instance indicating that the transfer job was created
     */
    @Path("{jobid}/phase")
    @POST
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void postTransfer(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id, @HeaderParam("X-DL-AuthToken") String authToken) throws IOException {
    log.info("postTransfer[jobId:" + id + "]");
	validateToken(authToken, resp);
	executeRequest(req, resp);
    }


    /**
     * This method retrieve the execution status (phase) of a transfer job.
     *
     * @param jobid The identifier for the transfer job to return.
     * @return the transfer JAXB object
     */
    @Path("{jobid}/phase")
    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void getPhase(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id, @HeaderParam("X-DL-AuthToken") String authToken) throws IOException {
    log.info("getPhase[jobId:" + id + "]");
	validateToken(authToken, resp);
	executeRequest(req, resp);
    }


    /**
     * This method retrieve the error summary of a transfer job.
     *
     * @param jobid The identifier for the transfer job to return.
     * @return the transfer JAXB object
     */
    @Path("{jobid}/error")
    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void getError(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id, @HeaderParam("X-DL-AuthToken") String authToken) throws IOException {
    log.info("getError[jobId:" + id + "]");
	validateToken(authToken, resp);
	executeRequest(req, resp);
    }


    private void validateToken(String authToken, HttpServletResponse resp) throws IOException{
	try {
  	    manager.validateToken(authToken);
	} catch (VOSpaceException e) {
	    resp.sendError(e.getStatusCode(), e.getMessage());
	}
    }

}
