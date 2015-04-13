
package edu.caltech.vao.vospace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Scanner;

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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.caltech.vao.vospace.resource.*;
import uws.UWSException;
import uws.job.JobList;
import uws.service.actions.UWSAction;
import uws.service.UWSFactory;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.file.LocalUWSFileManager;

@Path("sync")
public class SyncResource extends VOSpaceResource {

    private UWSService uws = null;

    public SyncResource() throws VOSpaceException {
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
//	    uws.addJobList(new JobList("sync"));
	    uws.addJobList(new JobList("transfers"));
	    
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
	    e.printStackTrace(System.err);
	    // Display properly the caught UWSException:
	    resp.sendError(e.getHttpErrorCode(), e.getMessage());		
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
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
     * This method launches a transfer job.
     *
     * @param transfer the transfer object to launch
     * @return a Response instance indicating that the transfer job was created
     */
    @POST
//    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void postTransfer(@Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException {
	/*
	FakeHttpServletResponse runResp = new FakeHttpServletResponse(resp.isCommitted(), resp.getContentType());
	FakeHttpServletRequest firstReq = new FakeHttpServletRequest("POST", new UrlInfo("http://localhost:8080/", "vospace-2.0/vospace/", "transfers"), req.getSession(), req.getRemoteAddr(), req.getScheme(), req.getServerName(), req.getServerPort(), req.getHeader("User-Agent"));
	Scanner s = new Scanner(req.getInputStream(), "UTF-8").useDelimiter("\\A");
        String body = s.hasNext() ? s.next() : "";
	firstReq.setBody(body);
	executeRequest(firstReq, runResp);
	// Create a request that looks like PHASE=RUN
	String location = runResp.getHeader("Location");
	String jobId = location.substring(location.lastIndexOf("/") + 1);
	// Get job description
	HttpClient client = new HttpClient();
	GetMethod get = new GetMethod(location);
	int statusCode = client.executeMethod(get);
	// POST PHASE=RUN
	FakeHttpServletRequest runReq = new FakeHttpServletRequest("POST", new UrlInfo("http://localhost:8080/", "vospace-2.0/vospace/", "sync/" + jobId + "/phase"), req.getSession(), req.getRemoteAddr(), req.getScheme(), req.getServerName(), req.getServerPort(), req.getHeader("User-Agent"));
	Map<String, String> params = new HashMap<String, String>(){{put("PHASE", "RUN");}};
	runReq.setParameters(params);
	executeRequest(runReq, resp);
	*/
	try {
	    Map<String, String[]> extraParams = new TreeMap<String, String[]>();
	    extraParams.put("PHASE", new String[] {"RUN"});
	    HttpServletRequest newReq = new FilteredRequest((HttpServletRequest) req, extraParams);
	    RequestDispatcher dispatch = req.getRequestDispatcher("/vospace/transfers");
	    FakeHttpServletResponse runResp = new FakeHttpServletResponse(resp.isCommitted(), resp.getContentType());
	    dispatch.forward(newReq, runResp);	
	    String location = runResp.getHeader("Location");
	    String jobId = location.substring(location.lastIndexOf("/") + 1);
	    resp.sendRedirect("http://localhost:8080/vospace-2.0/vospace/transfers/" + jobId + "/results/transferDetails");
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	}
    }


    /**
     * This method launches a transfer job.
     *
     * @param transfer the transfer object to launch
     * @return a Response instance indicating that the transfer job was created
     */
    @Path("{jobid}/phase")
    @POST
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void postTransfer(@Context HttpServletRequest req, @Context HttpServletResponse resp, @PathParam("jobid") String id) throws IOException {
	executeRequest(req, resp);
    }
}
