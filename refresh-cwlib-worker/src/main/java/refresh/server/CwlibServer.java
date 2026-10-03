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

import cwlib.util.GsonUtils;
import refresh.database.GameDatabaseContext;
import refresh.server.endpoints.AssetReadEndpoints;
import refresh.server.responses.ResponseRoot;

public class CwlibServer {
    private static final Logger logger = LogManager.getLogger(CwlibServer.class.getName());
    private Server httpServer;

    public CwlibServer() {
        this.httpServer = new Server(6789); // TODO ability to set port via config (or environment variable)

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
                        logger.debug("Successfully read request body: " + context.reqBodyStr);
                    } 
                    catch (IOException ex) {
                        return writeEmptyWithMessage(context, 500, "IOException while reading request body: '" + ex.getMessage() + "'.");
                    }
                }
                // Example path: /cwlib/deserialize/{sha1}
                // Route is always prefixed with "cwlib" and has at least 3 parts, so validate that.
                // TODO remove said string in context ctor
                if (context.pathLowerParts.length < 3 || !context.pathLowerParts[0].equals("cwlib")) {
                    return writeEmptyWithMessage(context, 404, "Unknown endpoint route '" + context.pathLower + "' (too short or not prefixed with '/cwlib').");
                }
                
                String endpointOperator = context.pathLowerParts[1];
                String resourceHash = context.pathLowerParts[2];

                GameDatabaseContext database = null;
                try {
                    database = new GameDatabaseContext(); // gameserver instanciates new one for every request, so let's try it here aswell
                }
                catch (Exception ex) {
                    return writeEmptyWithMessage(context, 500, "Failed to instanciate database context: " + ex.getMessage());
                }

                // method and path are case-insensitive here because we lower-case them in the context ctor
                switch (context.method) {
                    case "get":
                        switch (endpointOperator) {
                            // return content in response body
                            case "deserialize":
                                return writeResponse(context, AssetReadEndpoints.ReturnAndStoreAssetData(context, database, resourceHash));
                            default:
                                return writeEmptyWithMessage(context, 404, "Unknown GET endpoint route '" + context.pathLower + "' (unknown operator '" + endpointOperator + "').");
                        }
                    default:
                        return writeEmptyWithMessage(context, 404, "Unknown HTTP method '" + context.method + "'");
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

    protected static boolean writeEmptyWithMessage(EndpointContext context, int statusCode, String message) {
        return writeResponse(context, new ResponseRoot<String>(statusCode, null, message));
    }

    protected static <TResponse> boolean writeResponse(EndpointContext context, ResponseRoot<TResponse> response) {
        context.response.setStatus(response.StatusCode);
        String responseJson = GsonUtils.toJSON(response);
        logger.debug("Writing response JSON body '" + responseJson + "'.");

        Content.Sink.write(context.response, true, responseJson, context.callback);
        context.callback.succeeded();

        logger.info("Returning " + response.StatusCode + " on '" + context.method + "' '" + context.uri + "'.");
        return true;
    }
}
