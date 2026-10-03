package refresh.helpers;

import java.io.File;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.logging.log4j.Logger;

import cwlib.resources.RLevel;
import cwlib.resources.RPlan;
import cwlib.types.SerializedResource;
import cwlib.types.data.ResourceDescriptor;
import cwlib.types.data.WrappedResource;
import refresh.database.GameDatabaseContext;
import refresh.resources.MinimalLevelData;
import refresh.resources.MinimalPlanData;
import refresh.resources.MinimalResource;
import refresh.resources.MinimalResourceList;

public abstract class ResourceHelper {
    public static String GetAssetReference(ResourceDescriptor descriptor) {
        if (descriptor == null) return "0";
        else if (descriptor.isHash()) return descriptor.getSHA1().toString();
        else if (descriptor.isGUID()) return descriptor.getGUID().toString();
        else return "0";
    }

    // This should be used by an endpoint and a migration job
    public static MinimalResourceList DiscoverAndWriteAssetData(String rootHash, boolean includeDependencies, GameDatabaseContext database, Logger logger) {
        MinimalResourceList resourceList = new MinimalResourceList();
        int totalDeserializedAssetCount = 0;

        ArrayList<String> queuedHashes = new ArrayList<String>();
        queuedHashes.add(rootHash); // start with root

        // Use this to avoid scanning the same dependency multiple times.
        // Can happen if the same dependency appears multiple times in the tree,
        // or if we found a loop (more unlikely than the first case).
        ArrayList<String> alreadyScannedHashes = new ArrayList<String>();

        // Part 1: Gather and deserialize
        while (queuedHashes.size() > 0) {
            String currentHash = queuedHashes.getFirst();
            logger.debug("Scanning asset '" + currentHash + "'");

            boolean alreadyScanned = false;
            for (String alreadyScannedHash : alreadyScannedHashes) {
                if (currentHash.equals(alreadyScannedHash)) {
                    alreadyScanned = true;
                    logger.debug("Asset '" + currentHash + "' has already been scanned, skipping.");
                    break;
                }
            }
            if (alreadyScanned) {
                continue;
            }

            // TODO allow setting datastore by config, and maybe refactor resource loading
            File file = new File("/var/home/ich/Development/LBP/Refresh-DB/dataStore/" + currentHash);

            if (!file.exists()) {
                // TODO config or client should be able to specify whether this should result in an error response or just skipping the asset.
                logger.warn("Asset '" + currentHash + "' couldn't be found in datastore, skipping.");
                continue;
            }

            byte[] data;
            try {
                data = Files.readAllBytes(file.toPath());
            }
            catch (Exception ex) {
                // TODO config or client should be able to specify whether this should result in an error response or just skipping the asset.
                logger.warn("Failed to read  '" + currentHash + "' from datastore: " + ex.getMessage());
                continue;
            }

            SerializedResource resource;
            try {
                resource = new SerializedResource(data);
            }
            catch (Exception ex) {
                // TODO config or client should be able to specify whether this should result in an error response or just skipping the asset.
                logger.warn("Failed to parse '" + currentHash + "', probably not binary: " + ex.getMessage());
                continue;
            }

            logger.debug("Successfully deserialized asset metadata for '" + currentHash + "': type " + resource.getResourceType() + ", head revision: " + resource.getRevision().getHead());
            
            // Now that we got its dependencies, queue them if they're wanted
            if (includeDependencies) {
                logger.debug("Queueing dependencies of '" + currentHash + "'.");

                for (ResourceDescriptor dependency : resource.getDependencies()) {
                    String dependencyHash = ResourceHelper.GetAssetReference(dependency);

                    if (!dependency.isHash()) {
                        logger.debug("Skipping dependency '" + dependencyHash + "' because it is not a hash.");
                        continue;
                    }

                    // Don't skip dependencies if we don't care about their type, because they might be binary and have their own dependencies,
                    // eventually with the types we want.

                    logger.debug("Queueing dependency hash '" + dependencyHash + "'");
                    queuedHashes.add(dependencyHash);
                }
            }
            else {
                logger.debug("Skipping dependencies of '" + currentHash + "' because they are not wanted by the client.");
            }

            WrappedResource wrapped;
            try {
                wrapped = new WrappedResource(resource);
            }
            catch (Exception ex) {
                // TODO config or client should be able to specify whether this should result in an error response or just skipping the asset.
                logger.warn("Failed to parse inner data of '" + currentHash + "': " + ex.getMessage());
                continue;
            }
            
            switch (wrapped.type) {
                case LEVEL:
                    MinimalLevelData minLevel = new MinimalLevelData((RLevel)wrapped.resource);
                    resourceList.Levels.add(new MinimalResource<MinimalLevelData>(currentHash, minLevel));
                    totalDeserializedAssetCount++;

                    logger.debug("Parsed level '" + currentHash + "' added to response.");
                    break;
                case PLAN:
                    MinimalPlanData minPlan = new MinimalPlanData((RPlan)wrapped.resource);
                    resourceList.Plans.add(new MinimalResource<MinimalPlanData>(currentHash, minPlan));
                    totalDeserializedAssetCount++;
                    
                    logger.debug("Parsed plan '" + currentHash + "' added to response.");
                    break;
                case STREAMING_CHUNK: // TODO
                case ADVENTURE_CREATE_PROFILE: // TODO
                default:
                    logger.debug("Parsed asset '" + currentHash + "' has unknown type " + wrapped.type + ", skipping.");
                    break;

            }
        }

        // Part 2: Write to database
        if (database == null) {
            logger.warn("Received database context is null, deserialized plans/levels will not be written to database!");
            resourceList.FailedDatabaseInsertionCount = totalDeserializedAssetCount;
        }
        else {
            // write plans
            for (MinimalResource<MinimalPlanData> planResponse : resourceList.Plans) {
                try {
                    database.addOrUpdatePlanData(planResponse);
                    resourceList.SuccessfulDatabaseInsertionCount++;
                }
                catch (SQLException ex) {
                    logger.warn("Failed to write plan data '" + planResponse.Hash + "' to database: " + ex.getMessage());
                    resourceList.FailedDatabaseInsertionCount++;
                }
            }
            // write levels
            for (MinimalResource<MinimalLevelData> levelResponse : resourceList.Levels) {
                try {
                    database.addOrUpdateLevelData(levelResponse);
                    resourceList.SuccessfulDatabaseInsertionCount++;
                }
                catch (SQLException ex) {
                    logger.warn("Failed to write level data '" + levelResponse.Hash + "' to database: " + ex.getMessage());
                    resourceList.FailedDatabaseInsertionCount++;
                }
            }
        }

        return resourceList;
    }
}
