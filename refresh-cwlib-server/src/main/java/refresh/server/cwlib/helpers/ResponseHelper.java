package refresh.server.cwlib.helpers;

import java.util.Arrays;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.Content;

import cwlib.util.GsonUtils;
import refresh.server.cwlib.EndpointContext;
import refresh.server.cwlib.responses.api.ErrorResponse;

public abstract class ResponseHelper {
    public static boolean writeError(int statusCode, EndpointContext context, String message, Logger logger) {
        logger.error(LogHelper.formatFullRequestLog(statusCode, context, message));
        return writeResponse(statusCode, context, GsonUtils.toJSON(new ErrorResponse(message)));
    }

    public static boolean writeUnknownPathOrMethodError(EndpointContext context, Logger logger) {
        return ResponseHelper.writeError(404, context, "Unknown path or method", logger);
    }

    public static boolean writeTodoError(EndpointContext context, Logger logger) {
        return ResponseHelper.writeError(501, context, "TODO", logger);
    }

    public static boolean writeSuccess(int statusCode, byte[] responseBody, EndpointContext context, Logger logger) {
        logger.info(LogHelper.formatFullRequestLog(statusCode, context, null));
        return writeResponse(statusCode, context, Arrays.toString(responseBody));
    }

    public static boolean writeSuccess(int statusCode, String responseBody, EndpointContext context, Logger logger) {
        logger.info(LogHelper.formatFullRequestLog(statusCode, context, null));
        return writeResponse(statusCode, context, responseBody);
    }

    public static boolean writeResponse(int statusCode, EndpointContext context, byte[] responseBody) {
        return writeResponse(statusCode, context, Arrays.toString(responseBody));
    }

    public static boolean writeResponse(int statusCode, EndpointContext context, String responseBodyUtf8) {
        context.response.setStatus(statusCode);
        Content.Sink.write(context.response, true, responseBodyUtf8, context.callback);
        context.callback.succeeded();
        return true;
    }
}
