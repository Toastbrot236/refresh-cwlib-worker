package refresh.server.cwlib;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;

import refresh.server.cwlib.helpers.LogHelper;

public class EndpointContext {
    private static final Logger logger = LogManager.getLogger(CwlibServer.class.getName());
    public static final String DATA_STORE_BASE_PATH = "/home/ich/Development/Refresh/Refresh-DB/dataStore/"; // TODO make this configurable

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
        this.pathLowerParts = this.pathLower.split("/");
        this.queryStr = request.getHttpURI().getQuery();
        this.queryParams = Request.extractQueryParameters(request);

        this.request = request;
        this.response = response;
        this.callback = callback;

        logger.debug(LogHelper.formatRequestLog(this.reqId, "Parsed EndpointContext:\n\tHTTP Version: "
            + this.httpVersion + "\n\tMethod: " + this.method + "\n\tURI: " + this.uri + "\n\tLowercase Path: " + this.pathLower + "\n\tQuery Params: " + this.queryStr));
    }
}
