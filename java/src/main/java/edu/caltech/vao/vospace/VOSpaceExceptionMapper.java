
package edu.caltech.vao.vospace;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Exception mapper for mapping a VOSpaceException that was thrown by the service
 */
@Provider
public class VOSpaceExceptionMapper implements ExceptionMapper<VOSpaceException> {

    public Response toResponse(VOSpaceException e) {
        return Response.status(e.getStatusCode()).entity(e.getMessage()).build();
    }

}