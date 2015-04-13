
package edu.caltech.vao.vospace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Consumes;
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
import uws.UWSException;
import uws.job.JobList;
import uws.service.actions.UWSAction;
import uws.service.UWSFactory;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.file.LocalUWSFileManager;

@Path("transfers")
public class TransferResource extends VOSpaceResource {

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
    public void getTransfer(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id) throws IOException {
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
    public void getResults(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id) throws IOException {
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
    public String getResultsDetails(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id) throws VOSpaceException {
        try {
            MetaStore store = MetaStoreFactory.getInstance().getMetaStore();
	    String details = null;
	    // Check whether transfer exists in db yet
	    while (!store.isTransfer(id)) Thread.sleep(10);
	    // Get details - loop if necessary (db latency issues)
	    details = store.getResult(id);
	    while (details == null) {
		Thread.sleep(10);
		details = store.getResult(id);
	    }
	    return details;
        } catch (Exception e) {
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
        }    
    }


    /**
     * This method launches a transfer job.
     *
     * @param transfer the transfer object to launch
     * @return a Response instance indicating that the transfer job was created
     */
    @POST
    //    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void postTransfer(@Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException {
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
    public void postTransfer(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id) throws IOException {
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
    public void getPhase(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id) throws IOException {
	executeRequest(req, resp);
    }

}
