package refresh.server;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;

public class EndpointContext {
    private static final Logger logger = LogManager.getLogger(CwlibServer.class.getName());

    public Request request;
    public Response response;
    public Callback callback;

    public String reqId;
    public String httpVersion;
    public String method;
    public String uri;
    public String pathLower;
    public String[] pathLowerParts;
    public String queryStr;
    public Fields queryParams;
    public String reqBodyStr;

    public EndpointContext(Request request, Response response, Callback callback) {
        this.reqId = request.getId();
        this.httpVersion = request.getConnectionMetaData().getHttpVersion().asString();
        this.method = request.getMethod().toLowerCase();
        this.uri = request.getHttpURI().asString().toLowerCase();
        this.pathLower = request.getHttpURI().getPath().toLowerCase();
        this.queryStr = request.getHttpURI().getQuery();
        this.queryParams = Request.extractQueryParameters(request);

        // If the route starts with a /, then remove it. This should usually be the case.
        if (this.pathLower.length() > 0 && this.pathLower.charAt(0) == '/') {
            this.pathLower = this.pathLower.substring(1);
        }
        this.pathLowerParts = this.pathLower.split("/");

        this.request = request;
        this.response = response;
        this.callback = callback;

        logger.debug(this.reqId, "Parsed EndpointContext for request ID '" + this.reqId + "'':\n\tHTTP Version: " + this.httpVersion + 
            "\n\tMethod: " + this.method + "\n\tURI: " + this.uri + "\n\tLowercase Path: " + this.pathLower + "\n\tQuery Params: " + this.queryStr);
    }
}
