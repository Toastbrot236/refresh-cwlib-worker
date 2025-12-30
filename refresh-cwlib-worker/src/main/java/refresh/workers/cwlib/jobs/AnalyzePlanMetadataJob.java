package refresh.workers.cwlib.jobs;

import cwlib.types.SerializedResource;
import refresh.workers.WorkContext;
import refresh.workers.WorkerJob;
import refresh.workers.cwlib.state.AssetListState;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import cwlib.resources.RPlan;
import cwlib.structs.inventory.CreationHistory;
import cwlib.structs.inventory.InventoryItemDetails;
import cwlib.structs.inventory.UserCreatedDetails;
import cwlib.types.data.ResourceDescriptor;
import refresh.database.models.GameItem;

public class AnalyzePlanMetadataJob extends WorkerJob {
    private static final Logger logger = LogManager.getLogger();

    @Override
    protected Class<?> getJobStateType() {
        return AssetListState.class;
    }

    @Override
    public void executeJob(WorkContext context) throws IOException {
        AssetListState state = (AssetListState)this.jobState;
        for (String assetHash : state.Assets) {
            String path = "/home/ich/Development/Refresh/Refresh-DB/dataStore/" + assetHash;
            logger.error("Analyzing resource under " + path);
            File file = new File(path);

            if(!file.exists())
            {
                logger.error("Asset at '" + assetHash + "' couldn't be found");
                continue;
            }

            byte[] data = Files.readAllBytes(file.toPath());

            // Verify header magic
            byte[] magic = "PLNb".getBytes();
            boolean isMagicValid = true;

            for(int i = 0; i < magic.length; i++) {
                if (data[i] != magic[i]) {
                    logger.error("Asset at " + assetHash + " has non-plan magic!");
                    isMagicValid = false;
                    i = magic.length;
                }
            }
            if (!isMagicValid) {
                continue;
            }

            // Deserialize. All used pointers which are not validated to be not null seem
            // to always be initialized with a value.
            RPlan plan = new SerializedResource(data).loadResource(RPlan.class);
            InventoryItemDetails itemDetails = plan.inventoryData;

            String title;
            String description;
            boolean isUserCreation;
            UserCreatedDetails userDetails = itemDetails.userCreatedDetails;

            if (userDetails == null) {
                logger.debug("UserCreatedDetails is null");
                title = itemDetails.translatedTitle;
                description = itemDetails.translatedDescription;
                isUserCreation = false;
            }
            else {
                logger.debug("UserCreatedDetails not null");
                title = userDetails.name;
                description = userDetails.description;
                isUserCreation = true;
            }
            logger.debug("title: " + title + ", description: " + description);

            // Can be null in some cases. Likely happens if this defaults to translatedDescription,
            // which seems to not always be initialized.
            // Unconditionally ensure our pointer isn't null anyway.
            if (description == null) {
                description = "";
            }

            ResourceDescriptor icon = itemDetails.icon;
            String iconHash;
            if (icon.isHash()) {
                logger.debug("icon is hash");
                iconHash = icon.getSHA1().toString();
            }
            else if (icon.isGUID()) {
                logger.debug("icon is guid");
                iconHash = icon.getGUID().toString();
            }
            else {
                iconHash = "0";
            }
            logger.debug("icon reference: " + iconHash);

            // Get the usernames
            String[] contributorNames;
            CreationHistory history = itemDetails.creationHistory;
            if (history == null) {
                logger.debug("CreationHistory is null");
                contributorNames = new String[0];
            }
            else {
                logger.debug("CreationHistory not null");
                contributorNames = history.creators;
            }
            for (String contributorName : contributorNames) {
                logger.debug("Contributor: " + contributorName);
            }

            String creatorName = itemDetails.creator.toString();
            boolean isGamePhoto = itemDetails.photoData != null;
            boolean isCameraPhoto = itemDetails.eyetoyData != null;
            logger.debug("Creator: " + creatorName + ", isGamePhoto: " + isGamePhoto + ", isCameraPhoto: " + isCameraPhoto);

            try {
                context.Database.insertGameItem(new GameItem(assetHash, iconHash, title, description, 
                    creatorName, contributorNames, isGamePhoto, isCameraPhoto, isUserCreation));
            } 
            catch (SQLException e) {
                logger.error("Failed to insert deserialized info of plan '" + assetHash + "': " + e.getMessage());
            }
            
        }
    }
}
