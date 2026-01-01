package refresh.workers.cwlib.jobs;

import refresh.workers.WorkContext;
import refresh.workers.WorkerJob;
import refresh.workers.cwlib.state.CompleteAdventureDataJobState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cwlib.types.SerializedResource;
import cwlib.resources.RAdventureCreateProfile;
import cwlib.structs.slot.Slot;
import cwlib.structs.slot.SlotID;

public class CompleteAdventureDataJob extends WorkerJob {
    private static final Logger logger = LogManager.getLogger();

    @Override
    protected Class<?> getJobStateType() {
        return CompleteAdventureDataJobState.class;
    }

    // TODO: This should be refactored to somewhere where various different jobs can call this
    private boolean doesMagicMatch(byte[] given, String expected) {
        byte[] expectedMagic = expected.getBytes();
        if (given.length < expectedMagic.length) return false;

        for (int i = 0; i < expectedMagic.length; i++) {
            if (given[i] != expectedMagic[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void executeJob(WorkContext context) throws IOException {
        CompleteAdventureDataJobState state = (CompleteAdventureDataJobState)this.jobState;
        String dataStorePath = "/home/ich/Development/Refresh/Refresh-DB/dataStore/";

        for (String adventureRootHash : state.AdventureRootHashes) {
            logger.info("Processing adventure root asset '" + adventureRootHash + "'");
            File adventureRootFile = new File(dataStorePath + adventureRootHash);

            if(!adventureRootFile.exists()) {
                logger.error("Couldn't find the root asset!!");
                continue;
            }

            byte[] adventureRootData = Files.readAllBytes(adventureRootFile.toPath());
            if (this.doesMagicMatch(adventureRootData, "ADCb")) {
                logger.error("Root asset's magic is invalid!!");
                continue;
            }

            RAdventureCreateProfile adventureRootAsset = new SerializedResource(adventureRootData)
                .loadResource(RAdventureCreateProfile.class);

            // Get inner levels
            for (HashMap.Entry<SlotID, Slot> slot : adventureRootAsset.adventureSlots.entrySet()) {
                SlotID slotId = slot.getKey();
                Slot slotData = slot.getValue();
                logger.info("Processing inner adventure slot: ID: " + slotId.slotNumber + "', type: '" + slotId.slotType + "', name: '" + slotData.name + "'");
                
                // For some reason, there is always a slot of type ADVENTURE_AREA_LEVEL included which doesn't describe an inner level,
                // so ignore all slots which aren't ADVENTURE_LEVEL_LOCAL

                String levelRootHash;
                boolean isModded = false;
                if (slotData.root == null) {
                    logger.warn("Slot root resource is null!");
                    levelRootHash = "0";
                }
                else if (slotData.root.isHash()) {
                    logger.info("Slot root resource is a hash");
                    levelRootHash = slotData.root.getSHA1().toString();

                    // Now find the modded status in the map given by the game server
                    Boolean isModdedWrapped = state.LevelModdedRelations.get(levelRootHash);
                    if (isModdedWrapped == null) {
                        logger.warn("No pair matching slot root resource was found in LevelModdedRelations map!");
                    }
                    else if (isModdedWrapped == true) {
                        isModded = true;
                    }
                }
                else if (slotData.root.isGUID()) {
                    logger.info("Slot root resource is a guid");
                    levelRootHash = slotData.root.getGUID().toString();
                }
                else {
                    logger.warn("Slot root resource is of unknown type!");
                    levelRootHash = "0";
                }
            }
        }
    }
}
