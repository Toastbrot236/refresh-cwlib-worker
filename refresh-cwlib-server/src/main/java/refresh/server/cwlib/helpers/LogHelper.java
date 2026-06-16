package refresh.server.cwlib.helpers;

import cwlib.enums.CompressionFlags;
import cwlib.enums.ResourceType;
import cwlib.enums.SerializationType;
import cwlib.types.SerializedResource;
import cwlib.types.data.Revision;
import refresh.server.cwlib.EndpointContext;

public abstract class LogHelper {
    /**
     * Formats a message with the request ID and the message.
     */
    public static String formatRequestLog(String reqId, String message) {
        return "[Request " + reqId + "] " + message;
    }

    /**
     * Formats a message with the request ID and the message.
     */
    public static String formatSerializedResourceLog(String reqId, String hash, SerializedResource resource, String message) {
        ResourceType type = resource.getResourceType();
        SerializationType format = resource.getSerializationType();
        Revision revision = resource.getRevision();
        int headRevision = revision.getHead();
        short branchID = revision.getBranchID();
        short branchRevision = revision.getBranchRevision();
        String compression = CompressionFlags.toString(resource.getCompressionFlags());

        String ret = "'" + hash + "' (type: " + type + " | " + format
            + "; revision: " + headRevision + " | " + branchID + " | " + branchRevision + "; compression: " + compression + ")";
        
        if (message != null) ret = message + " " + ret; // message is optional, everything else is mandatory
        return formatRequestLog(reqId, ret);
    }

    /**
     * Formats a message with the request ID, the status code, the method, the URI and an optional message
     */
    public static String formatFullRequestLog(int statusCode, String reqId, String method, String uri, String message) {
        String ret = statusCode + " on method '" + method + "' @ '" + uri + "'";
        if (message != null) ret += "\n\twith message: '" + message + "'"; // message is optional, everything else is mandatory
        return formatRequestLog(reqId, ret);
    }

    /**
     * Formats a message with the request ID, the status code, the method, the URI and an optional message
     */
    public static String formatFullRequestLog(int statusCode, EndpointContext context, String message) {
        return formatFullRequestLog(statusCode, context.reqId, context.method, context.uri, message);
    }
}