package refresh.helpers;

import cwlib.types.data.ResourceDescriptor;

import java.io.File;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.logging.log4j.Logger;

import cwlib.resources.RLevel;
import cwlib.resources.RPlan;
import cwlib.types.SerializedResource;
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

    /**
     * Deserializes plan/level data from the assets specified by hash array, returns said data, and tries to write it into database if the given DB context is not null.
     * If wanted, will also do the same for all its dependencies.
     * Scan data which already exists in DB will be overwritten.
     */ 
    public static MinimalResourceList DiscoverAndWriteAssetData(List<String> rootHashes, boolean includeDependencies, GameDatabaseContext database, Logger logger) {
        MinimalResourceList resourceList = new MinimalResourceList();
        int totalDeserializedAssetCount = 0;

        LinkedHashSet<String> queuedHashes = new LinkedHashSet<String>();
        queuedHashes.addAll(rootHashes); // start with root

        // Use this to avoid scanning the same dependency multiple times.
        // Can happen if the same dependency appears multiple times in the tree,
        // or if we found a loop (more unlikely than the first case).
        HashSet<String> alreadyScannedHashes = new HashSet<String>();

        // Part 1: Gather and deserialize
        while (queuedHashes.size() > 0) {
            String currentHash = queuedHashes.removeFirst(); // dequeue our next hash
            alreadyScannedHashes.add(currentHash);
            logger.debug("Scanning asset '" + currentHash + "'");

            if (alreadyScannedHashes.contains(currentHash)) {
                logger.debug("Asset '" + currentHash + "' has already been handled, skipping.");
                continue;
            }

            // TODO allow setting datastore by config, and maybe refactor resource loading
            File file = new File("/var/home/ich/Development/LBP/Refresh-DB/dataStore/" + currentHash);

            if (!file.exists()) {
                logger.warn("Asset '" + currentHash + "' couldn't be found in datastore, skipping.");
                continue;
            }

            byte[] data;
            try {
                data = Files.readAllBytes(file.toPath());
            }
            catch (Exception ex) {
                logger.warn("Failed to read  '" + currentHash + "' from datastore: " + ex.getMessage());
                continue;
            }

            SerializedResource resource;
            try {
                resource = new SerializedResource(data);
            }
            catch (Exception ex) {
                // debug log because it's common for assets to reference assets which are not binary or just not handled by CWLib (e.g. textures)
                logger.debug("Failed to parse '" + currentHash + "', probably not binary: " + ex.getMessage());
                continue;
            }

            logger.debug("Successfully deserialized asset metadata for '" + currentHash + "': type " + resource.getResourceType() + ", head revision: " + resource.getRevision().getHead());
            
            // Now that we got its dependencies, queue them if they're wanted
            // TODO Some dependencies, like icons in speech bubbles, are probably not in this list. We should somehow gather those too.
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
                // debug log for a similar reason as with SerializedResource
                logger.debug("Failed to parse inner data of '" + currentHash + "': " + ex.getMessage());
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
