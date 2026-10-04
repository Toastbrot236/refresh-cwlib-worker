package refresh.workers.cwlib.jobs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cwlib.resources.RLevel;
import cwlib.resources.RPlan;
import cwlib.types.SerializedResource;
import cwlib.types.data.ResourceDescriptor;
import refresh.helpers.ResourceHelper;
import refresh.resources.MinimalLevelData;
import refresh.resources.MinimalPlanData;
import refresh.resources.MinimalResource;
import refresh.resources.MinimalResourceList;
import refresh.workers.WorkContext;
import refresh.workers.WorkerJob;
import refresh.workers.cwlib.state.AssetListState;

public class ScanNewAssetsJob extends WorkerJob {
    private static final Logger logger = LogManager.getLogger();

    // TODO does this job even need its own state?
    @Override
    protected Class<?> getJobStateType() {
        return AssetListState.class;
    }

    @Override
    public void executeJob(WorkContext context) throws IOException {
        LinkedHashSet<String> queuedHashes = new LinkedHashSet<String>();
        
        try {
            // Fetch and queue root hashes
            queuedHashes.addAll(context.Database.getAssetHashesNeedingScans());
        }
        catch (SQLException ex) {
            logger.error("Failed to fetch asset hashes to scan from DB! Skipping this job.");
            return;
        }

        MinimalResourceList resourceList = new MinimalResourceList();
        int totalDeserializedAssetCount = 0;

        // Use this to avoid looking up and scanning the same asset multiple times.
        // Can happen if the same asset appears multiple times in a dependency tree,
        // or if we found a loop (more unlikely than the first case).
        // Now that we start with multiple root hashes, we can now have dependencies appear in multiple trees.
        // We can't rely on HashSet's own deduplication here, since we keep removing and adding hashes to our queue.
        HashSet<String> alreadyScannedHashes = new HashSet<String>();

        // Part 1: Deserialize and gather dependencies
        while (queuedHashes.size() > 0) {
            String currentHash = queuedHashes.removeFirst(); // dequeue our next hash
            alreadyScannedHashes.add(currentHash);
            logger.debug("Scanning asset '" + currentHash + "'");

            if (alreadyScannedHashes.contains(currentHash)) {
                logger.debug("Asset '" + currentHash + "' has already been handled, skipping.");
                continue;
            }

            // TODO allow setting datastore by config
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
            
            // We can already queue the dependencies from the resource metadata
            // TODO Some dependencies, like icons in speech bubbles, are probably not in this list. We should somehow gather those too.
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
            
            switch (resource.getResourceType()) {
                case LEVEL:
                    MinimalLevelData minLevel = new MinimalLevelData(resource.loadResource(RLevel.class));
                    resourceList.Levels.add(new MinimalResource<MinimalLevelData>(currentHash, minLevel));
                    totalDeserializedAssetCount++;

                    logger.debug("Parsed level '" + currentHash + "' added to response.");
                    break;
                case PLAN:
                    MinimalPlanData minPlan = new MinimalPlanData(resource.loadResource(RPlan.class));
                    resourceList.Plans.add(new MinimalResource<MinimalPlanData>(currentHash, minPlan));
                    totalDeserializedAssetCount++;
                    
                    logger.debug("Parsed plan '" + currentHash + "' added to response.");
                    break;
                case STREAMING_CHUNK: // TODO
                case ADVENTURE_CREATE_PROFILE: // TODO
                default:
                    logger.debug("Parsed asset '" + currentHash + "' has unhandled type " + resource.getResourceType() + ", skipping.");
                    break;

            }
        }

        // Part 2: Write to database
        // TODO mark all of these as scanned on their GameAssets
        // write plans
        for (MinimalResource<MinimalPlanData> planResponse : resourceList.Plans) {
            try {
                context.Database.addOrUpdatePlanData(planResponse);
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
                context.Database.addOrUpdateLevelData(levelResponse);
                resourceList.SuccessfulDatabaseInsertionCount++;
            }
            catch (SQLException ex) {
                logger.warn("Failed to write level data '" + levelResponse.Hash + "' to database: " + ex.getMessage());
                resourceList.FailedDatabaseInsertionCount++;
            }
        }
    }
}
