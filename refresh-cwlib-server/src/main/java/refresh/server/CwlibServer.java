package refresh.server;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;

import refresh.server.endpoints.FetchEndpoints;
import refresh.server.helpers.LogHelper;
import refresh.server.helpers.ResponseHelper;

public class CwlibServer {
    private static final Logger logger = LogManager.getLogger(CwlibServer.class.getName());
    private Server httpServer;

    public CwlibServer() {
        this.httpServer = new Server(6789); // TODO ability to set port via config

        this.httpServer.setHandler(new Handler.Abstract()
        {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws IOException
            {
                EndpointContext context = new EndpointContext(request, response, callback);

                // Don't try to get body if it's a GET request
                if (!context.method.equals("get"))
                {
                    try {
                        context.reqBodyStr = Content.Source.asString(request);
                        logger.debug(LogHelper.formatRequestLog(context.reqId, "Successfully read body: " + context.reqBodyStr));
                    } 
                    catch (IOException ex) {
                        return ResponseHelper.writeError(400, context, "IOException while reading request body: " + ex.getMessage(), logger);
                    }
                }
                
                // all routes below have atleast 3 parts: the "cwlib" prefix, the operator, and the asset hash;
                // for now we will only search in the main data store, so no dry archive or anything remote
                if (context.pathLowerParts.length < 3 || !context.pathLowerParts[0].equals("cwlib")) {
                    return ResponseHelper.writeUnknownPathOrMethodError(context, logger);
                }

                String endpointOperator = context.pathLowerParts[1];
                String resourceHash = context.pathLowerParts[2];

                // method and path are case-insensitive here because we lower-case them in the context ctor
                switch (context.method) {
                    case "get":
                        return switch (endpointOperator) {
                            case "asoriginal" -> FetchEndpoints.returnResourceAsJson(context, resourceHash);
                            case "asjson" -> FetchEndpoints.returnResourceAsJson(context, resourceHash);
                            case "asminimaljson" -> ResponseHelper.writeTodoError(context, logger);
                            default -> ResponseHelper.writeUnknownPathOrMethodError(context, logger);
                        };
                    case "post":
                        return switch (endpointOperator) {
                            case "editlevel" -> ResponseHelper.writeTodoError(context, logger);
                            case "editplan" -> ResponseHelper.writeTodoError(context, logger);
                            case "editadventure" -> ResponseHelper.writeTodoError(context, logger);
                            case "editslotlist" -> ResponseHelper.writeTodoError(context, logger);
                            case "editpack" -> ResponseHelper.writeTodoError(context, logger);
                            case "editquest" -> ResponseHelper.writeTodoError(context, logger);
                            case "editchunk" -> ResponseHelper.writeTodoError(context, logger);
                            case "editpins" -> ResponseHelper.writeTodoError(context, logger);
                            default -> ResponseHelper.writeUnknownPathOrMethodError(context, logger);
                        };
                    default: return ResponseHelper.writeUnknownPathOrMethodError(context, logger);
                }
            }
        });
    }

    public void start() throws Exception {
        this.httpServer.start();
    }

    public void stop() throws Exception {
        this.httpServer.stop();
    }
}
