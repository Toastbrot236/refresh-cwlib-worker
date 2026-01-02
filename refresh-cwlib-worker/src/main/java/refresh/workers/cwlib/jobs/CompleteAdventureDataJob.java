package refresh.workers.cwlib.jobs;

import refresh.workers.WorkContext;
import refresh.workers.WorkerJob;
import refresh.workers.cwlib.state.CompleteAdventureDataJobState;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import cwlib.enums.SlotType;
import cwlib.types.SerializedResource;
import cwlib.resources.RAdventureCreateProfile;
import cwlib.resources.RLevel;
import cwlib.structs.slot.Slot;
import cwlib.structs.slot.SlotID;
import cwlib.types.data.NetworkPlayerID;
import refresh.database.models.GameInnerLevel;
import refresh.workers.cwlib.state.AdventureToComplete;

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

        for (AdventureToComplete adventure : state.Adventures) {
            logger.info("\n\n ---- Next adventure root hash: " + adventure.RootHash + " ---- ");

            // Get ID. May be a little slower this way, but if the adventure somehow can't be found, we can just skip it
            // instead of unnecessarily adding inner levels
            int adventureId = 0;
            try {
                adventureId = context.Database.getLevelIdFromRootHash(adventure.RootHash);
            }
            catch (SQLException ex) {
                logger.error("SQLException happened when looking up adventure ID");
            }
            if (adventureId <= 0) {
                logger.error("Skipping adventure because of an invalid ID (not found or exception thrown)");
                continue;
            }
            
            File adventureRootFile = new File(dataStorePath + adventure.RootHash);
            if (!adventureRootFile.exists()) {
                logger.error("Couldn't find the adventure root asset");
                continue;
            }

            byte[] adventureRootData = Files.readAllBytes(adventureRootFile.toPath());
            if (!this.doesMagicMatch(adventureRootData, "ADCb")) {
                logger.error("Adventure root asset's magic " 
                    + new String(Arrays.copyOfRange(adventureRootData, 0, 4), StandardCharsets.UTF_8) + " is invalid");
                continue;
            }

            RAdventureCreateProfile adventureRootAsset = new SerializedResource(adventureRootData)
                .loadResource(RAdventureCreateProfile.class);

            // Get inner levels
            for (HashMap.Entry<SlotID, Slot> slot : adventureRootAsset.adventureSlots.entrySet()) {
                SlotID slotId = slot.getKey();
                Slot slotData = slot.getValue();

                logger.info("Processing inner level ID " + slotId.slotNumber + " (" + slotId.slotType + ")");
                
                // For some reason, there is always a slot of type ADVENTURE_AREA_LEVEL included which doesn't describe an inner level,
                // so ignore all slots which aren't ADVENTURE_LEVEL_LOCAL
                if (slotId.slotType != SlotType.ADVENTURE_LEVEL_LOCAL) {
                    continue;
                }

                String levelRootHash;
                boolean isModded = false;
                if (slotData.root == null) {
                    levelRootHash = "0";
                }
                else if (slotData.root.isHash()) {
                    levelRootHash = slotData.root.getSHA1().toString();

                    // Now find the modded status in the map given by the game server
                    Boolean isModdedWrapped = adventure.LevelModdedRelations.get(levelRootHash);
                    if (isModdedWrapped == true) {
                        isModded = true;
                    }
                }
                else if (slotData.root.isGUID()) {
                    levelRootHash = slotData.root.getGUID().toString();
                }
                else {
                    levelRootHash = "0";
                }

                float xLocation = slotData.location.x;
                float yLocation = slotData.location.y;
                float zLocation = slotData.location.z;
                byte badgeSize = slotData.customBadgeSize;

                // Get level asset to obtain contributor list
                File levelRootFile = new File(dataStorePath + levelRootHash);
                ArrayList<String> contributorNames = new ArrayList<String>();

                if (levelRootFile.exists()) {
                    byte[] levelRootData = Files.readAllBytes(levelRootFile.toPath());

                    if (this.doesMagicMatch(levelRootData, "LVLb")) {
                        RLevel level = new SerializedResource(levelRootData)
                            .loadResource(RLevel.class);
                        
                        NetworkPlayerID[] contributorPlayerIds = level.playerRecord.getPlayerIDs();
                        
                        for (NetworkPlayerID playerId : contributorPlayerIds) {
                            String username = playerId.toString();

                            // These playerIDs lists seem to always have empty handles, whose corresponding numbers in
                            // playerNumbers are always -1. Since handles already must be atleast 3 characters long,
                            // skip all shorter handles. Non-empty names under 3 characters, incase a level ever has such names,
                            // will be modded, and uninformative in the DB and on the API anyway.
                            // Also, skip names which appear in every single asset regardless of actual involvement with the level.
                            if (username.length() < 3 || username.length() > 16 
                             || username.equals("OfflinePlayer") || username.equals("gumbahmoo")) {
                                continue;
                            }

                            contributorNames.add(username);
                        }
                    }
                    else {
                        logger.warn("Level root asset's magic " 
                            + new String(Arrays.copyOfRange(levelRootData, 0, 4), StandardCharsets.UTF_8) + " is invalid");
                    }
                }
                else {
                    logger.warn("Couldn't find the level root asset");
                }

                GameInnerLevel innerLevel = new GameInnerLevel(slotId.slotNumber, slotData.name, )
            }


        }
    }
}
