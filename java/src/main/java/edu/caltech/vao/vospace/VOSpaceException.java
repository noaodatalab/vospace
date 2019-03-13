
package edu.caltech.vao.vospace;

import java.util.EnumMap;

/**
 * Any exception thrown by a VOSpace service must be associated with an HTTP status code and a
 * VOSpace fault type.
 */
public class VOSpaceException extends Exception {
    public enum VOFault { InternalFault, PermissionDenied, InvalidURI, ContainerNotFound,
            NodeNotFound, DuplicateNode, InvalidToken, InvalidArgument, TypeNotSupported,
            ViewNotSupported, InvalidData, LinkFoundFault, NodeBusy
    }

    // Map fault names to strings
    private static EnumMap<VOFault, String> faultNames;
    static {
        faultNames = new EnumMap<VOFault, String>(VOFault.class);
        faultNames.put(VOFault.InternalFault, "Internal Fault");
        faultNames.put(VOFault.PermissionDenied, "Permission Denied");
        faultNames.put(VOFault.InvalidURI, "Invalid URI");
        faultNames.put(VOFault.ContainerNotFound, "Container Not Found");
        faultNames.put(VOFault.NodeNotFound, "Node Not Found");
        faultNames.put(VOFault.DuplicateNode, "Duplicate Node");
        faultNames.put(VOFault.InvalidToken, "Invalid Token");
        faultNames.put(VOFault.InvalidArgument, "Invalid Argument");
        faultNames.put(VOFault.TypeNotSupported, "Type Not Supported");
        faultNames.put(VOFault.ViewNotSupported, "View Not Supported");
        faultNames.put(VOFault.InvalidData, "Invalid Data");
        faultNames.put(VOFault.LinkFoundFault, "Link Found Fault");
        faultNames.put(VOFault.NodeBusy, "Node Busy");
    }
    // Map fault names to status codes
    private static EnumMap<VOFault, Integer> statusCodes;
    static {
        statusCodes = new EnumMap<VOFault, Integer>(VOFault.class);
        statusCodes.put(VOFault.InternalFault, 500);
        statusCodes.put(VOFault.PermissionDenied, 403);
        statusCodes.put(VOFault.InvalidURI, 400);
        statusCodes.put(VOFault.ContainerNotFound, 404);
        statusCodes.put(VOFault.NodeNotFound, 404);
        statusCodes.put(VOFault.DuplicateNode, 409);
        statusCodes.put(VOFault.InvalidToken, 400);
        statusCodes.put(VOFault.InvalidArgument, 400);
        statusCodes.put(VOFault.TypeNotSupported, 400);
        statusCodes.put(VOFault.ViewNotSupported, 400);
        statusCodes.put(VOFault.InvalidData, 400);
        statusCodes.put(VOFault.LinkFoundFault, 400);
        statusCodes.put(VOFault.NodeBusy, 409);
    }
    // Map fault names to messages
    private static EnumMap<VOFault, String> faultMessages;
    static {
        faultMessages = new EnumMap<VOFault, String>(VOFault.class);
        faultMessages.put(VOFault.InternalFault, "An error occurred in the VOSpace application.");
        faultMessages.put(VOFault.PermissionDenied, "The user does not have sufficient access privileges for this operation.");
        faultMessages.put(VOFault.InvalidURI, "The requested URI is invalid.");
        faultMessages.put(VOFault.ContainerNotFound, "A Container does not exist with the requested URI.");
        faultMessages.put(VOFault.NodeNotFound, "A Node does not exist with the requested URI.");
        faultMessages.put(VOFault.DuplicateNode, "A Node already exists with the requested URI.");
        faultMessages.put(VOFault.InvalidToken, "The requested Token is invalid.");
        faultMessages.put(VOFault.InvalidArgument, "A required argument is invalid.");
        faultMessages.put(VOFault.TypeNotSupported, "The service does not support the requested Node type.");
        faultMessages.put(VOFault.ViewNotSupported, "The service does not support the requested View.");
        faultMessages.put(VOFault.InvalidData, "The specified data is invalid.");
        faultMessages.put(VOFault.LinkFoundFault, "The requested URI contains a LinkNode.");
        faultMessages.put(VOFault.NodeBusy, "The requested Node is busy.");
    }

    protected VOFault faultCode = VOFault.InternalFault;

    public VOSpaceException(VOFault fault, String message) {
        super(message);
        faultCode = fault;
    }

    public VOSpaceException(VOFault fault) {
        this(fault, faultMessages.get(fault));
    }

    /* Should be used only in the case of an InternalFault */
    public VOSpaceException(Throwable t) {
        super(t);
    }

    public VOSpaceException(Throwable t, VOFault fault) {
        super(t);
        faultCode = fault;
    }

    public VOSpaceException(String message, Throwable t, VOFault fault) {
        super(message, t);
        faultCode = fault;
    }

    public int getStatusCode() {
        return statusCodes.get(faultCode);
    }

    public String toString() {
        return faultNames.get(faultCode) + ": " + getMessage();
    }
}