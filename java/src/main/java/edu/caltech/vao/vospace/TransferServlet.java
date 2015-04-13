package edu.caltech.vao.vospace;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.caltech.vao.vospace.resource.*;
import uws.UWSException;
import uws.job.JobList;
import uws.service.UWSFactory;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.file.LocalUWSFileManager;

public class TransferServlet extends HttpServlet {

/** The UWSService used by all requests. */
private UWSService service = null;

/** Uniq initialization of the servlet.
 * This function is always called before service(..., ...) at the first use of the servlet. 
 */
    public void init(ServletConfig config) throws ServletException {
	try {
	    // Create the UWS service: (just once since this function is called just once by Tomcat)
       
	    // Create the Universal Worker Service:
	    UWSFactory factory = new TransferJobFactory();
	    LocalUWSFileManager fileManager = new LocalUWSFileManager(new File("/tmp/uws"));
	    service = new UWSService(factory, fileManager);
	    service.setDescription("This UWS aims to manage one (or more) JobList(s) of Transfers." + "Transfer is a kind of Job dealing with a data transfer within a VOSpace");
	    
	    // Create the job list
	    service.addJobList(new JobList("transfers"));

	} catch (UWSException e) {
	    throw new ServletException("...", e);
	}
    }

    /** Function called when any request is sent to this servlet. */
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
	try{
	    // Just forward the request to the UWS service:
	    service.executeRequest(req, resp);
	} catch(UWSException e) {
	    resp.sendError(e.getHttpErrorCode(), e.getMessage());
	}
    }
}