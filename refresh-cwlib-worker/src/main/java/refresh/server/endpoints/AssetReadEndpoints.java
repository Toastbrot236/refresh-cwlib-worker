package refresh.server.endpoints;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import refresh.database.GameDatabaseContext;
import refresh.helpers.ResourceHelper;
import refresh.resources.MinimalResourceList;
import refresh.server.EndpointContext;
import refresh.server.responses.ResponseRoot;

public abstract class AssetReadEndpoints {
    private static final Logger logger = LogManager.getLogger(AssetReadEndpoints.class.getName());

    public static ResponseRoot<MinimalResourceList> ReturnAndStoreAssetData(EndpointContext context, GameDatabaseContext database, String hash) {
        String includeDepsStr = context.queryParams.getValue("includeDependencies");
        boolean includeDeps = includeDepsStr != null && includeDepsStr.equalsIgnoreCase("true");

        MinimalResourceList response = ResourceHelper.DiscoverAndWriteAssetData(hash, includeDeps, database, logger);
        return new ResponseRoot<MinimalResourceList>(200, response, null);
    }
}