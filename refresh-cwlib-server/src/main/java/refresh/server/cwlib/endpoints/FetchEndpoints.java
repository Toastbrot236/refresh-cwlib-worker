package refresh.server.cwlib.endpoints;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cwlib.enums.CompressionFlags;
import cwlib.enums.ResourceType;
import cwlib.enums.SerializationType;
import cwlib.types.SerializedResource;
import cwlib.types.data.Revision;
import cwlib.types.data.WrappedResource;
import cwlib.util.GsonUtils;
import refresh.server.cwlib.EndpointContext;
import refresh.server.cwlib.helpers.LogHelper;
import refresh.server.cwlib.helpers.ResponseHelper;

public abstract class FetchEndpoints {
    private static final Logger logger = LogManager.getLogger(FetchEndpoints.class.getName());

    public static boolean returnResourceAsOriginal(EndpointContext context, String hash) {
        // TODO configurable data store path
        File file = new File(EndpointContext.DATA_STORE_BASE_PATH + hash);

        if (!file.exists())
            ResponseHelper.writeError(404, context, "The specified resource '" + hash + "' couldn't be found in data store.", logger);

        byte[] data;
        try {
            data = Files.readAllBytes(file.toPath());
        } 
        catch (IOException ex) {
            return ResponseHelper.writeError(400, context, "IOException while trying to read the resource: " + ex.getMessage(), logger);
        }

        ResponseHelper.writeSuccess(200, data, context, logger);
        return true;
    }

    public static boolean returnResourceAsJson(EndpointContext context, String hash) {
        SerializedResource resource;
        try {
            resource = new SerializedResource(EndpointContext.DATA_STORE_BASE_PATH + hash);
        }
        catch (Exception ex) {
            return ResponseHelper.writeError(501, context, "Failed to read or parse the resource: " + ex.getMessage(), logger);
        }

        logger.info(LogHelper.formatSerializedResourceLog(context.reqId, hash, resource, "Successfully read resource"));

        WrappedResource wrapped;
        try {
            wrapped = new WrappedResource(resource);
        }
        catch (Exception ex) {
            return ResponseHelper.writeError(501, context, "Failed to wrap the resource: " + ex.getMessage(), logger);
        }

        String json = GsonUtils.toJSON(wrapped, resource.getRevision());
        ResponseHelper.writeSuccess(200, json, context, logger);
        return true;
    }
}
