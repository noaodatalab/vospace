
package edu.caltech.vao.vospace;

/**
 * Any exception thrown by a VOSpace service may be associated with an HTTP status code and a  
 * VOSpace error type.
 */
public class VOSpaceException extends Exception {

    // Status codes
    public final static int SUCCESSFUL = 200;
    public final static int CREATED = 201;
    public final static int BAD_REQUEST = 400;
    public final static int PERMISSION_DENIED = 401;
    public final static int FORBIDDEN = 403;
    public final static int NOT_FOUND = 404;
    public final static int CONFLICT = 409;
    public final static int INTERNAL_SERVER_ERROR = 500;
    public final static int NOT_IMPLEMENTED = 501;
    public final static int SERVICE_UNAVAILABLE = 503;
    public final static int USER_ACCESS_DENIED = 504;

    protected int statusCode = NOT_FOUND;

    public VOSpaceException(String message) {
	super(message);
    }

    public VOSpaceException(Throwable t) {
	super(t);
    }

    public VOSpaceException(int status, String message) {
	this(message);
	if (status >= 0) 
	    statusCode = status;
    }

    public VOSpaceException(int status, Throwable t) {
	this(t);
	if (status >= 0) 
	    statusCode = status;
    }

    public VOSpaceException(int status, Throwable t, String message) {
	super(message, t);
	if (status >= 0)
	    statusCode = status;
    }

    public int getStatusCode() {
	return statusCode;
    }

}