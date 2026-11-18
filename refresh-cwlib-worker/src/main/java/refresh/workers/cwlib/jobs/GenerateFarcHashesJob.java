package refresh.workers.cwlib.jobs;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import org.joml.Vector4f;

import cwlib.enums.CommonMeshes;
import cwlib.enums.CompressionFlags;
import cwlib.enums.ContentsType;
import cwlib.enums.DLCFileFlags;
import cwlib.enums.LevelType;
import cwlib.enums.ResourceType;
import cwlib.enums.Revisions;
import cwlib.enums.SlotType;
import cwlib.resources.RDLC;
import cwlib.resources.RPacks;
import cwlib.resources.RPins;
import cwlib.resources.RTranslationTable;
import cwlib.structs.dlc.DLCFile;
import cwlib.structs.dlc.DLCGUID;
import cwlib.structs.profile.Pin;
import cwlib.structs.slot.Collectabubble;
import cwlib.structs.slot.Label;
import cwlib.structs.slot.Pack;
import cwlib.structs.slot.Slot;
import cwlib.structs.slot.SlotID;
import cwlib.types.SerializedResource;
import cwlib.types.archives.SaveArchive;
import cwlib.types.data.GUID;
import cwlib.types.data.ResourceDescriptor;
import cwlib.types.data.Revision;
import cwlib.types.data.SHA1;
import cwlib.types.databases.FileDB;
import cwlib.util.FileIO;
import cwlib.util.GsonUtils;
import refresh.database.models.assets.FarcHashesData;
import refresh.resources.DLCHeaderFile;
import refresh.workers.WorkContext;
import refresh.workers.WorkerJob;
import refresh.workers.cwlib.state.AssetListState;

public class GenerateFarcHashesJob extends WorkerJob {
    @Override
    protected Class<?> getJobStateType() {
        return AssetListState.class;
    }

    private int jobVersion = 1;
    private String debugDumpPath = "/var/home/ich/Development/LBP/Refresh-DB/RDump/";
    private String dataStorePath = "/var/home/ich/Development/LBP/Refresh-DB/dataStore/";
    private boolean complete = false; // Don't repeatedly execute this for each work cycle, only once per worker start

    @Override
    public void executeJob(WorkContext context) throws IOException {
        if (complete) return;

        RTranslationTable englishTrans = new RTranslationTable();
        englishTrans.add(420, "Epic pin name!!");
        englishTrans.add(421, "Epic pin desk!!");
        englishTrans.add(RTranslationTable.makeLamsKeyID("TESTER_TEST_PACK_GROUP_NAME"), "Epic pack group name!!");
        englishTrans.add(RTranslationTable.makeLamsKeyID("TESTER_TEST_PACK_GROUP_DESC"), "Epic pack group descedfressfsd lol!!");
        englishTrans.add(RTranslationTable.makeLamsKeyID("TESTER_TEST_PACK_LEVEL_NAME"), "Epic pack leval name!!");
        englishTrans.add(RTranslationTable.makeLamsKeyID("TESTER_TEST_PACK_LEVEL_DESC"), "Epic pack leval deskriptoin!!");

        DLCGUID testDlcGuid1 = new DLCGUID();
        testDlcGuid1.guid = new GUID(18451); // lbp1 star sticker
        testDlcGuid1.flags = DLCFileFlags.OWNED;

        DLCFile testDlcFile1 = new DLCFile();
        testDlcFile1.directory = "";
        testDlcFile1.file = "dccomics_3.edat";
        testDlcFile1.contentID = "LBPDLCWBDCCK0003";
        testDlcFile1.inGameCommerceID = "";
        testDlcFile1.categoryID = "";
        testDlcFile1.flags = DLCFileFlags.OWNED;
        testDlcFile1.typeMask = 6329151;
        testDlcFile1.guids.add(new GUID(18451)); // lbp1 star sticker

        RDLC dlcs = new RDLC();
        dlcs.guids.add(testDlcGuid1);
        dlcs.files.add(testDlcFile1);

        Slot testSlot1 = new Slot(
            new SlotID(SlotType.DLC_LEVEL, 2),
            new ResourceDescriptor(142846, ResourceType.LEVEL), // match fit side b
            new Vector4f(0.125f, 0.444f, 0.67f, 0.0f)
        );
        testSlot1.icon = new ResourceDescriptor(85790, ResourceType.TEXTURE); // match fit side b
        testSlot1.authorID = null;
        testSlot1.authorName = "Marcell Davis";
        testSlot1.translationTag = "TESTER_TEST_PACK_LEVEL";
        testSlot1.planetDecorations = new ResourceDescriptor(102363, ResourceType.PLAN); // factory
        testSlot1.primaryLinkLevel = null;
        testSlot1.initiallyLocked = false;
        testSlot1.shareable = true;
        testSlot1.developerLevelType = LevelType.BOSS;
        testSlot1.name = "lol";
        testSlot1.description = "LEL";
        testSlot1.labels = new Label[1];
        testSlot1.labels[0] = new Label((int)RTranslationTable.makeLamsKeyID("LABEL_Easy"), 0); // why do i have to int cast a LAMS key?
        testSlot1.collectabubblesContained = new Collectabubble[0];
        testSlot1.collectabubblesRequired = new Collectabubble[3];
        testSlot1.collectabubblesRequired[0] = new Collectabubble();
        testSlot1.collectabubblesRequired[1] = new Collectabubble();
        testSlot1.collectabubblesRequired[2] = new Collectabubble();
        testSlot1.moveRecommended = true;
        testSlot1.crossCompatible = false;
        testSlot1.showOnPlanet = true;
        testSlot1.livesOverride = 40;
        testSlot1.group = new SlotID(SlotType.DLC_PACK, 2);

        Pack testPack1 = new Pack();
        testPack1.contentsType = ContentsType.LEVEL;
        testPack1.mesh = new ResourceDescriptor(CommonMeshes.LEVEL_BADGE_GUID, ResourceType.MESH);
        testPack1.contentID = "";
        testPack1.timestamp = 0;
        testPack1.slot = testSlot1;

        Slot testSlot2 = new Slot(
            new SlotID(SlotType.DLC_PACK, 2),
            null,
            new Vector4f(0.234f, 0.456f, 0.555f, 0.0f)
        );
        testSlot2.icon = new ResourceDescriptor(130927, ResourceType.TEXTURE); // space pool icon
        testSlot2.authorID = null;
        testSlot2.authorName = "Marcell Davis";
        testSlot2.translationTag = "TESTER_TEST_PACK_GROUP";
        testSlot2.planetDecorations = new ResourceDescriptor(102363, ResourceType.PLAN); // factory
        testSlot2.primaryLinkLevel = null;
        testSlot2.initiallyLocked = false;
        testSlot2.shareable = true;
        testSlot2.developerLevelType = LevelType.MAIN_PATH;
        testSlot2.name = "GROUP!!!";
        testSlot2.description = "Legit";
        testSlot2.labels = new Label[1];
        testSlot2.labels[0] = new Label((int)RTranslationTable.makeLamsKeyID("LABEL_Quick"), 0); // why do i have to int cast a LAMS key?
        testSlot2.collectabubblesContained = new Collectabubble[0];
        testSlot2.collectabubblesRequired = new Collectabubble[3];
        testSlot2.collectabubblesRequired[0] = new Collectabubble();
        testSlot2.collectabubblesRequired[1] = new Collectabubble();
        testSlot2.collectabubblesRequired[2] = new Collectabubble();
        testSlot2.moveRecommended = false;
        testSlot2.crossCompatible = false;
        testSlot2.showOnPlanet = true;
        testSlot2.livesOverride = 40;
        testSlot2.group = null;

        Pack testPack2 = new Pack();
        testPack2.contentsType = ContentsType.GROUP;
        testPack2.mesh = new ResourceDescriptor(CommonMeshes.POLAROID_GUID, ResourceType.MESH);
        testPack2.contentID = "LBPDLCWBDCCK0003";
        testPack2.timestamp = 1387407600;
        testPack2.slot = testSlot2;

        ArrayList<Pack> packList = new ArrayList<>();
        packList.add(testPack1);
        packList.add(testPack2);
        RPacks packs = new RPacks(packList);

        RPins pins = new RPins();
        Pin testPin1 = new Pin();
        testPin1.id = 42069;
        testPin1.progressType = 42069;
        testPin1.category = 2;
        testPin1.titleLamsKey = 420;
        testPin1.descriptionLamsKey = 421;
        testPin1.icon = new ResourceDescriptor(18451, ResourceType.TEXTURE); // lbp1 star sticker
        testPin1.initialProgressValue = 1234;
        testPin1.targetValue = 9000;
        testPin1.trophyToUnlock = 65;
        pins.pins.add(testPin1);

        SaveArchive pinFarc = new SaveArchive(new Revision(Revisions.LBP2_MAX), 0x4);
        SaveArchive pckFarc = new SaveArchive(new Revision(Revisions.LBP2_MAX), 0x4);
        FileDB map = new FileDB(0x100);

        byte[] pinsBin = SerializedResource.compress(pins, new Revision(Revisions.LBP2_MAX), CompressionFlags.USE_NO_COMPRESSION);
        byte[] pinsJson = GsonUtils.toJSON(pins, new Revision(Revisions.LBP2_MAX)).getBytes();
        SHA1 pinsHash = SHA1.fromBuffer(pinsBin);
        FileIO.write(pinsBin, this.dataStorePath + pinsHash.toString());
        FileIO.write(pinsBin, this.debugDumpPath + "pins_" + pinsHash.toString() + ".bin");
        FileIO.write(pinsJson, debugDumpPath + "pins_" + pinsHash.toString() + ".json");
        pinFarc.add(pinsBin);
        map.newFileDBRow("custom/pins.pin", 12345676);

        byte[] packBin = SerializedResource.compress(packs, new Revision(Revisions.LBP2_MAX), CompressionFlags.USE_NO_COMPRESSION);
        byte[] packJson = GsonUtils.toJSON(packs, new Revision(Revisions.LBP2_MAX)).getBytes();
        SHA1 packHash = SHA1.fromBuffer(packBin);
        FileIO.write(packBin, this.dataStorePath + packHash.toString());
        FileIO.write(packBin, this.debugDumpPath + "pack_" + packHash.toString() + ".bin");
        FileIO.write(packJson, this.debugDumpPath + "pack_" + packHash.toString() + ".json");
        pckFarc.add(packBin);
        map.newFileDBRow("custom/pack.pck", 12345677);

        byte[] dlcBin = SerializedResource.compress(dlcs, new Revision(Revisions.LBP2_MAX), CompressionFlags.USE_NO_COMPRESSION);
        byte[] dlcJson = GsonUtils.toJSON(dlcs, new Revision(Revisions.LBP2_MAX)).getBytes();
        SHA1 dlcHash = SHA1.fromBuffer(dlcBin);
        FileIO.write(dlcBin, this.dataStorePath + dlcHash.toString());
        FileIO.write(dlcBin, this.debugDumpPath + "dlc_" + dlcHash.toString() + ".bin");
        FileIO.write(dlcJson, this.debugDumpPath + "dlc_" + dlcHash.toString() + ".json");
        pinFarc.add(dlcBin);
        pckFarc.add(dlcBin);
        map.newFileDBRow("custom/deeelcee.dlc", 12345678);

        byte[] transBin = englishTrans.build();
        SHA1 transHash = SHA1.fromBuffer(transBin);
        FileIO.write(dlcBin, this.dataStorePath + transHash.toString());
        FileIO.write(dlcBin, this.debugDumpPath + "trans_" + transHash.toString() + ".bin");
        englishTrans.export(this.debugDumpPath + "trans_" + transHash.toString() + ".json");
        pinFarc.add(transBin);
        pckFarc.add(transBin);
        map.newFileDBRow("custom/english.trans", 12345679);

        byte[] mapBin = map.build();
        SHA1 mapHash = SHA1.fromBuffer(mapBin);
        FileIO.write(mapBin, this.dataStorePath + mapHash.toString());
        FileIO.write(mapBin, this.debugDumpPath + "db_" + mapHash.toString() + ".map");

        byte[] pinFarcBin = pinFarc.build(false);
        SHA1 pinFarcHash = SHA1.fromBuffer(pinFarcBin);
        FileIO.write(pinFarcBin, this.dataStorePath + pinFarcHash.toString());
        FileIO.write(pinFarcBin, this.debugDumpPath + "pin_" + pinFarcHash.toString() + ".farc");

        byte[] pckFarcBin = pckFarc.build(false);
        SHA1 pckFarcHash = SHA1.fromBuffer(pckFarcBin);
        FileIO.write(pckFarcBin, this.dataStorePath + pckFarcHash.toString());
        FileIO.write(pckFarcBin, this.debugDumpPath + "pck_" + pckFarcHash.toString() + ".farc");

        DLCHeaderFile header = new DLCHeaderFile();
        header.Revision = 0;
        header.BranchDescription = 0;
        for (int i = 0; i < header.TranslationTables.length; i++) {
            header.TranslationTables[i] = transHash; // lol
        }
        header.DatabaseHash = mapHash;
        header.Resource = new ResourceDescriptor(dlcHash, ResourceType.DOWNLOADABLE_CONTENT);

        byte[] dlcHeaderBin = header.build();
        byte[] dlcHeaderJson = GsonUtils.toJSON(header, new Revision(Revisions.LBP2_MAX)).getBytes();
        SHA1 dlcHeaderHash = SHA1.fromBuffer(dlcHeaderBin);
        FileIO.write(dlcHeaderBin, this.dataStorePath + dlcHeaderHash.toString());
        FileIO.write(dlcHeaderBin, this.debugDumpPath + "header_" + dlcHeaderHash.toString() + ".bin");
        FileIO.write(dlcHeaderJson, this.debugDumpPath + "header_" + dlcHeaderHash.toString() + ".json");

        FarcHashesData hashesData = new FarcHashesData();
        hashesData.DlcHeaderHash = dlcHeaderHash.toString();
        hashesData.DlcResourceHash = dlcHash.toString();
        hashesData.PinFarcHash = pinFarcHash.toString();
        hashesData.PinResourceHash = pinsHash.toString();
        hashesData.PackFarcHash = pckFarcHash.toString();
        hashesData.PackResourceHash = packHash.toString();
        hashesData.MapHash = mapHash.toString();
        hashesData.EnglishTransTableHash = transHash.toString();
        hashesData.GeneratedUsingJobVersion = this.jobVersion;

        try {
            int version = context.Database.saveFarcHashes(hashesData);
            System.out.println("Farc hashes have been generated and inserted into DB: auto-increment version " + version + "!");
            this.complete = true;
        }
        catch (SQLException ex) {
            System.out.println("SQL failure while inserting farc hashes: " + ex.getMessage());
        }
    }
}