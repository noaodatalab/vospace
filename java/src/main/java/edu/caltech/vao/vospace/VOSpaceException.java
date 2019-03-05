
package edu.caltech.vao.vospace;

/**
 * Any exception thrown by a VOSpace service may be associated with an HTTP status code and a
 * VOSpace error type.
 */
public class VOSpaceException extends Exception {

    // Status codes
    public final static int SUCCESSFUL = 200;
    public final static int CREATED = 201;
    public final static int BAD_REQUEST = 400; /* InvalidURI, TypeNotSupported, LinkFound, InvalidArgument */
    public final static int PERMISSION_DENIED = 401;
    public final static int FORBIDDEN = 403; /* PermissionDenied */
    public final static int NOT_FOUND = 404; /* ContainerNotFound, NodeNotFound */
    public final static int CONFLICT = 409; /* DuplicateNode */
    public final static int INTERNAL_SERVER_ERROR = 500; /* InternalFault */
    public final static int NOT_IMPLEMENTED = 501;
    public final static int SERVICE_UNAVAILABLE = 503;
    public final static int USER_ACCESS_DENIED = 504;

    // VOSpace fault names, thrown in the response header
    public final static String InternalFault = "InternalFault";
        /* Status 500, with a description of the cause of the fault. */
    public final static String PermissionDenied = "PermissionDenied";
        /* Status 403, with a description of why the credentials (if any were provided) were rejected. */
    public final static String InvalidURI = "InvalidURI";
        /* Status 400, with details of the invalid URI. */
    public final static String NodeNotFound = "NodeNotFound";
        /* Status 404, with the URI of the missing Node. */
    public final static String DuplicateNode = "DuplicateNode";
        /* Status 409, with the URI of the duplicate Node. */
    public final static String InvalidToken = "InvalidToken";
        /* No HTTP status, with the invalid token. */
    public final static String InvalidArgument = "InvalidArgument";
        /* Status 400, with a description of the invalid argument, including the View or
           Protocol URI and the name and value of the parameter that caused the fault. */
    public final static String TypeNotSupported = "TypeNotSupported";
        /* Status 400, with the QName of the unsupported type. */
    public final static String ViewNotSupported = "ViewNotSupported";
        /* No HTTP status, with the URI of the View. */
    public final static String InvalidData = "InvalidData";
        /* No HTTP status, with any error message that the data parser produced. */
    public final static String LinkFoundFault = "LinkFoundFault";
        /* Status 400, must contain the full details of the LinkNode. */
    public final static String NodeBusy = "NodeBusy";
        /* No HTTP status, thrown when a node is not in a state to perform the requested operation. */

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