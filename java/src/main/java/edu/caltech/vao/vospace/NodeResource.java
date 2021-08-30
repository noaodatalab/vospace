
package edu.caltech.vao.vospace;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import edu.caltech.vao.vospace.xml.*;
import edu.caltech.vao.vospace.VOSpaceException.VOFault;

@Path("nodes")
public class NodeResource extends VOSpaceResource {

    //    private final String ROOTNODE = "vos://nvo.caltech!vospace";
    private final String ROOTNODE = "vos://datalab.noao.edu!vospace";

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
    public edu.noirlab.datalab.vos.Node getNode(@QueryParam("detail") String detail, @QueryParam("limit") int limit) throws VOSpaceException {
        edu.noirlab.datalab.vos.Node node = manager.getNodeJDOM2(manager.ROOT_NODE, detail, limit);
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
    public edu.noirlab.datalab.vos.Node getNode(@PathParam("nodeid") String nodeid, @QueryParam("detail") String detail, @QueryParam("limit") int limit, @HeaderParam("X-DL-AuthToken") String authToken) throws VOSpaceException {
        String id = getId(nodeid);
        manager.validateAccess(authToken, id, true);
        edu.noirlab.datalab.vos.Node node = manager.getNodeJDOM2(id, detail, limit);
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
    public Node updateNode(@PathParam("nodeid") String nodeid, Node node, @HeaderParam("X-DL-AuthToken") String authToken) throws VOSpaceException {
        String id = getId(nodeid);
        manager.validateAccess(authToken, id, false);
        if (!node.getUri().equals(id)) throw new VOSpaceException(VOFault.InvalidURI);
        try {
            Node newNode = manager.create(node, getUser(authToken), true);
            return newNode;
        } catch (VOSpaceException ve) {
            throw ve;
        } catch (Exception e) {
            throw new VOSpaceException(e, id);
        }
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
    public Response putNode(@PathParam("nodeid") String nodeid, Node node, @HeaderParam("X-DL-AuthToken") String authToken) throws VOSpaceException {
        String id = getId(nodeid);
        manager.validateAccess(authToken, id, false);
        if (!node.getUri().equals(id)) throw new VOSpaceException(VOFault.InvalidURI);
        try {
            Node newNode = manager.create(node, getUser(authToken), false);
            URI nodeUri = new URI(newNode.getUri());
            return Response.created(nodeUri).entity(newNode).build();
        } catch (VOSpaceException ve) {
            throw ve;
        } catch (Exception e) {
            throw new VOSpaceException(e, id);
        }
    }

    /**
     * This method deletes the specified node
     *
     * @param nodeid The node to delete
     */
    @Path("{nodeid: .*}")
    @DELETE
    public void deleteNode(@PathParam("nodeid") String nodeid, @HeaderParam("X-DL-AuthToken") String authToken) throws VOSpaceException {
        String id = getId(nodeid);
        manager.validateAccess(authToken, id, false);
        manager.delete(id);
    }

    private String getId(String nodeid) {
        return manager.ROOT_NODE + "/" + nodeid;
    }

    private String getUser(String authToken) {
        String[] parts = authToken.split("\\.");
        return parts[0];
    }
}
