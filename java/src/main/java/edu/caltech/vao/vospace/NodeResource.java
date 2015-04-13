
package edu.caltech.vao.vospace;

import java.io.InputStream;
import java.io.PrintWriter;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javax.xml.bind.JAXBElement;

import com.ximpleware.*;
import com.ximpleware.xpath.*;

import edu.caltech.vao.vospace.xml.*;

@Path("nodes")
public class NodeResource extends VOSpaceResource {

    private final String ROOTNODE = "vos://nvo.caltech!vospace";

    public NodeResource() throws VOSpaceException {
	super();
    }

    /**
     * This method retrieves the root node.
     * 
     * @return the root node 
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
	public Node getNode(@QueryParam("detail") String detail, @QueryParam("limit") int limit) throws VOSpaceException {
	Node node = manager.getNode(ROOTNODE, detail, limit);
	return node;
    }

    /**
     * This method retrieves the specified node.
     * 
     * @param nodeid The VOSpace identifier for the node to return.
     * @return the specified node {nodeid:[^/]+?}
     */
    @Path("{nodeid: .*}")
    @GET
    @Produces(MediaType.APPLICATION_XML)
	public Node getNode(@PathParam("nodeid") String nodeid, @QueryParam("detail") String detail, @QueryParam("limit") int limit) throws VOSpaceException {
	    Node node = manager.getNode(getId(nodeid), detail, limit);
	return node;
    }

    /**
     * This method updates the specified node's properties.
     * 
     * @param node The node to update.
     * @return the updated node
     */
    @Path("{nodeid: .*}")
    @POST
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_XML)
    public Node updateNode(@PathParam("nodeid") String nodeid, Node node) throws VOSpaceException {
	if (!node.getUri().equals(getId(nodeid))) throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, "A specified URI is invalid");
	Node newNode = manager.create(node, true);
	return newNode;
    }

    /**
     * This method creates the specified node.
     * 
     * @param node The node to create (contents of HTTP PUT).
     * @return the created node
     */
    @Path("{nodeid: .*}")
    @PUT
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_XML)
    public Response putNode(@PathParam("nodeid") String nodeid, Node node) throws VOSpaceException {	
	if (!node.getUri().equals(getId(nodeid))) throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, "A specified URI is invalid");
	Node newNode = manager.create(node, false);
	try {
	    URI nodeUri = new URI(newNode.getUri());
  	    return Response.created(nodeUri).entity(newNode).build();
	} catch (Exception e) {
	    throw new VOSpaceException(e.getMessage());
	}
    }

    /**
     * This method deletes the specified node
     * 
     * @param nodeid The node to delete
     */
    @Path("{nodeid: .*}")
    @DELETE
    public void deleteNode(@PathParam("nodeid") String nodeid) throws VOSpaceException {
	manager.delete(getId(nodeid));
    }

    private String getId(String nodeid) {
	return ROOTNODE + "/" + nodeid;
    }

}
