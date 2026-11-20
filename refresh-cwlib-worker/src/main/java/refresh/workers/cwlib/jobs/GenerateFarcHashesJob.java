package refresh.workers.cwlib.jobs;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import org.joml.Vector4f;
import cwlib.enums.CompressionFlags;
import cwlib.enums.ContentsType;
import cwlib.enums.DLCFileFlags;
import cwlib.enums.LevelType;
import cwlib.enums.ResourceType;
import cwlib.enums.Revisions;
import cwlib.enums.SlotType;
import cwlib.io.Resource;
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
import cwlib.types.archives.Fart;
import cwlib.types.data.GUID;
import cwlib.types.data.ResourceDescriptor;
import cwlib.types.data.Revision;
import cwlib.types.data.SHA1;
import cwlib.types.databases.FileDB;
import cwlib.types.databases.FileDBRow;
import cwlib.util.FileIO;
import cwlib.util.GsonUtils;
import refresh.database.models.assets.FarcHashesData;
import refresh.resources.DLCHeaderFile;
import refresh.resources.MinimalFart;
import refresh.resources.MinimalFileDB;
import refresh.resources.RemoteFart;
import refresh.workers.WorkContext;
import refresh.workers.WorkerJob;
import refresh.workers.cwlib.state.AssetListState;

public class GenerateFarcHashesJob extends WorkerJob {
    @Override
    protected Class<?> getJobStateType() {
        return AssetListState.class;
    }

    private int jobVersion = 1;
    private int currentFarcDataVersion = 0;
    private String debugDumpPath = "/var/home/ich/Development/LBP/Refresh-DB/RDump";
    private String dataStorePath = "/var/home/ich/Development/LBP/Refresh-DB/dataStore";
    private boolean complete = false; // Don't repeatedly execute this for each work cycle, only once per worker start

    @Override
    public void executeJob(WorkContext context) throws IOException {
        if (complete) return;

        try {
            this.currentFarcDataVersion = context.Database.getHighestFarcDataVersion() + 1;
        }
        catch (SQLException ex) {
            System.out.println("SQL failure while getting highest farc hashes version: " + ex.getMessage());
            return;
        }

        RemoteFart pinFarc = new RemoteFart();
        RemoteFart packFarc = new RemoteFart();
        FileDB map = new FileDB(0x100); // taken from LBP2 brg_patch.map
        RTranslationTable englishTrans = this.createTestTransTable();
        RPins pins = this.createTestPins();
        RPacks packs = this.createTestPacks();

        SHA1 englishTransHash = this.writeTransTable("english.trans", englishTrans, map, new Fart[] {pinFarc, packFarc});
        SHA1 pinResourceHash = this.writeNormalResource("pins.pin", pins, map, new Fart[] {pinFarc, packFarc});
        SHA1 packResourceHash = this.writeNormalResource("packs.pck", packs, map, new Fart[] {pinFarc, packFarc});

        /*
        pinFarc.getKey().setRootHash(pinResourceHash);
        pinFarc.getKey().setRootType(ResourceType.PINS);
        packFarc.getKey().setRootHash(packResourceHash);
        packFarc.getKey().setRootType(ResourceType.PACKS);
        */

        RDLC dlcs = this.createTestDlcResource(new SHA1[] {englishTransHash, pinResourceHash, packResourceHash});
        SHA1 dlcResourceHash = this.writeDlcResource("dlcs.dlc", dlcs, map, new Fart[] {pinFarc, packFarc});

        SHA1 mapHash = this.writeMap("database.map", map, new Fart[] {pinFarc, packFarc});

        DLCHeaderFile header = this.createTestDlcHeader(dlcResourceHash, ResourceType.DOWNLOADABLE_CONTENT, englishTransHash, mapHash);
        DLCHeaderFile pinHeader = this.createTestDlcHeader(pinResourceHash, ResourceType.PINS, englishTransHash, mapHash);
        DLCHeaderFile packHeader = this.createTestDlcHeader(packResourceHash, ResourceType.PACKS, englishTransHash, mapHash);

        SHA1 pinHeaderHash = this.writeDlcHeader("pin_header.bin", pinHeader);
        SHA1 packHeaderHash = this.writeDlcHeader("pack_header.bin", packHeader);
        SHA1 dlcHeaderHash = this.writeDlcHeader("dlc_header.bin", header);

        pinFarc.headerHash = pinHeaderHash;
        packFarc.headerHash = packHeaderHash;
        SHA1 pinFarcHash = this.writeFarc("pins.farc", pinFarc);
        SHA1 packFarcHash = this.writeFarc("packs.farc", packFarc);

        FarcHashesData hashesData = new FarcHashesData();
        hashesData.DlcHeaderHash = dlcHeaderHash.toString();
        hashesData.DlcResourceHash = dlcResourceHash.toString();
        hashesData.PinFarcHash = pinFarcHash.toString();
        hashesData.PinResourceHash = pinResourceHash.toString();
        hashesData.PackFarcHash = packFarcHash.toString();
        hashesData.PackResourceHash = packResourceHash.toString();
        hashesData.MapHash = mapHash.toString();
        hashesData.EnglishTransTableHash = englishTransHash.toString();
        hashesData.GeneratedUsingJobVersion = this.jobVersion;

        try {
            int version = context.Database.saveFarcHashes(hashesData);
            System.out.println("Farc hashes have been generated and inserted into DB: auto-increment version " + version + "!");
        }
        catch (SQLException ex) {
            System.out.println("SQL failure while inserting farc hashes: " + ex.getMessage());
            return;
        }

        this.complete = true;
    }

    public RTranslationTable createTestTransTable() {
        RTranslationTable trans = new RTranslationTable();

        trans.add(RTranslationTable.makeLamsKeyID("TESTER_TEST_PIN_NAME"), "Epic pin name!!");
        trans.add(RTranslationTable.makeLamsKeyID("TESTER_TEST_PIN_DESC"), "Epic pin desk!!");
        trans.add(RTranslationTable.makeLamsKeyID("DAVINCI_THEME_NAME"), "Da vinki???");
        trans.add(RTranslationTable.makeLamsKeyID("DAVINCI_THEME_DESC"), "lololol");
        trans.add(RTranslationTable.makeLamsKeyID("TESTER_TEST_PACK_GROUP_NAME"), "Epic pack group name!!");
        trans.add(RTranslationTable.makeLamsKeyID("TESTER_TEST_PACK_GROUP_DESC"), "Epic pack group descedfressfsd lol!!");
        trans.add(RTranslationTable.makeLamsKeyID("TESTER_TEST_PACK_LEVEL_NAME"), "Epic pack leval name!!");
        trans.add(RTranslationTable.makeLamsKeyID("TESTER_TEST_PACK_LEVEL_DESC"), "Epic pack leval deskriptoin!!");

        return trans;
    }

    public RPins createTestPins() {
        RPins pins = new RPins();

        Pin testPin1 = new Pin();
        testPin1.id = 42069;
        testPin1.progressType = 42069;
        testPin1.category = 0;
        testPin1.titleLamsKey = RTranslationTable.makeLamsKeyID("TESTER_TEST_PIN_NAME");
        testPin1.descriptionLamsKey = RTranslationTable.makeLamsKeyID("TESTER_TEST_PIN_DESC");
        testPin1.icon = new ResourceDescriptor(18451, ResourceType.TEXTURE); // lbp1 star sticker
        testPin1.initialProgressValue = 1234;
        testPin1.targetValue = 9000;
        testPin1.trophyToUnlock = 65;
        pins.pins.add(testPin1);

        return pins;
    }

    public RPacks createTestPacks() {
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
        testSlot1.developerLevelType = LevelType.MAIN_PATH;
        testSlot1.name = "lol";
        testSlot1.description = "LEL";
        testSlot1.labels = new Label[1];
        testSlot1.labels[0] = new Label((int)RTranslationTable.makeLamsKeyID("LABEL_Easy"), 0); // why do i have to int cast a LAMS key?
        testSlot1.collectabubblesContained = new Collectabubble[0];
        testSlot1.collectabubblesRequired = new Collectabubble[0];
        testSlot1.moveRecommended = true;
        testSlot1.crossCompatible = false;
        testSlot1.showOnPlanet = true;
        testSlot1.livesOverride = 40;
        testSlot1.group = new SlotID(SlotType.DLC_PACK, 3);

        Pack testPack1 = new Pack();
        testPack1.contentsType = ContentsType.LEVEL;
        testPack1.mesh = testPack1.contentsType.getBadgeMesh();
        testPack1.contentID = "LBPDLCAMOGUS0001";
        testPack1.timestamp = 1795204244;
        testPack1.slot = testSlot1;

        // CREATE GROUP PACK + SLOT

        Slot testSlot2 = new Slot(
            new SlotID(SlotType.DLC_PACK, 3),
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
        testSlot2.collectabubblesRequired = new Collectabubble[0];
        testSlot2.moveRecommended = false;
        testSlot2.crossCompatible = false;
        testSlot2.showOnPlanet = true;
        testSlot2.livesOverride = 40;
        testSlot2.group = null;

        Pack testPack2 = new Pack();
        testPack2.contentsType = ContentsType.GROUP;
        testPack2.mesh = testPack2.contentsType.getBadgeMesh();
        testPack2.contentID = "LBPDLCAMOGUS0002";
        testPack2.timestamp = 1795204244;
        testPack2.slot = testSlot2;

        // CREATE PACKS RESOURCE

        ArrayList<Pack> packList = new ArrayList<>();
        packList.add(testPack1);
        packList.add(testPack2);
        return new RPacks(packList);
    }

    public RDLC createTestDlcResource(SHA1[] hashes) {
        RDLC dlcs = new RDLC();

        DLCFile testDlcFile1 = new DLCFile();
        testDlcFile1.directory = "";
        testDlcFile1.file = "";
        testDlcFile1.contentID = "LBPDLCAMOGUS0001";
        testDlcFile1.inGameCommerceID = "";
        testDlcFile1.categoryID = "";
        testDlcFile1.flags = 17; //DLCFileFlags.OWNED;
        testDlcFile1.typeMask = 16;

        DLCFile testDlcFile2 = new DLCFile();
        testDlcFile2.directory = "";
        testDlcFile2.file = "";
        testDlcFile2.contentID = "LBPDLCAMOGUS0002";
        testDlcFile2.inGameCommerceID = "";
        testDlcFile2.categoryID = "";
        testDlcFile2.flags = 17; //DLCFileFlags.OWNED;
        testDlcFile2.typeMask = 16;

        dlcs.files.add(testDlcFile1);
        dlcs.files.add(testDlcFile2);

        HashSet<GUID> guids = new HashSet<>();
        //guids.add(new GUID(18451));
        guids.add(new GUID(142846)); // root level guid of the one level

        /*
        for (SHA1 hash : hashes) {
            // Generate guids for our hashes
            guids.add(new GUID(RTranslationTable.makeLamsKeyID(hash.toString())));
        }
        */

        for (GUID guid : guids) {
            testDlcFile1.guids.add(guid);
            testDlcFile2.guids.add(guid);

            DLCGUID testDlcGuid1 = new DLCGUID();
            testDlcGuid1.guid = guid;
            testDlcGuid1.flags = DLCFileFlags.OWNED;
            dlcs.guids.add(testDlcGuid1);
        }

        return dlcs;
    }

    public DLCHeaderFile createTestDlcHeader(SHA1 mainResHash, ResourceType mainResType, SHA1 englishTransHash, SHA1 mapHash) {
        DLCHeaderFile header = new DLCHeaderFile();

        header.Revision = 0;
        header.BranchDescription = 0;
        for (int i = 0; i < header.TranslationTables.length; i++) {
            header.TranslationTables[i] = englishTransHash; // lol
        }
        header.DatabaseHash = mapHash;
        header.Resource = new ResourceDescriptor(mainResHash, mainResType);

        return header;
    }

    public SHA1 writeNormalResource(String filename, Resource resource, FileDB map, Fart[] farcs) {
        byte[] binary = SerializedResource.compress(resource, new Revision(Revisions.LBP2_MAX), CompressionFlags.USE_NO_COMPRESSION);
        String json = GsonUtils.toJSON(resource, new Revision(Revisions.LBP2_MAX));

        return writeFile(filename, binary, json, map, farcs);
    }

    public SHA1 writeTransTable(String filename, RTranslationTable table, FileDB map, Fart[] farcs) {
        byte[] binary = table.build();
        SHA1 hash = writeFile(filename, binary, null, map, farcs);

        // Have to export JSON using this instead...
        table.export(this.debugDumpPath + "/" + this.currentFarcDataVersion + "/" + filename + "_" + hash.toString() + ".json");

        return hash;
    }

    public SHA1 writeDlcResource(String filename, RDLC dlc, FileDB map, Fart[] farcs) {
        byte[] binary = SerializedResource.compress(dlc, new Revision(Revisions.LBP2_MAX), CompressionFlags.USE_NO_COMPRESSION);

        /*
        // Serialize as text instead
        String textDlcRootTemplate =
        """
        DLCt
        GUIDS   %s
        %s
        Files   %s
        %s
        """;

        String textDlcGuidTemplate =
        """
            [%s]    
                GUID.guid   %s
                Flags   %s
        """;

        String textDlcFileTemplate =
        """
            [%s]    
                Directory   "%s"
                File    "%s"
                ContentID   "%s"
                InGameCommerceID    "%s"
                CategoryID  "%s"
                Flags   %s
                GUIDs   %s
        %s
                TypeMask    %s
        """;

        String textGuidTemplate =
        """
                    [%s]    %s
        """;

        String textDlcGuids = "";
        String textDlcFiles = "";

        for (int i = 0; i < dlc.guids.size(); i++) {
            DLCGUID dlcGuid = dlc.guids.get(i);
            textDlcGuids += String.format(textDlcGuidTemplate, i, dlcGuid.guid.getValue(), dlcGuid.flags);
        }

        for (int i = 0; i < dlc.files.size(); i++) {
            DLCFile dlcFile = dlc.files.get(i);
            String textDlcFileGuids = "";

            for (int j = 0; j < dlcFile.guids.size(); j++) {
                GUID dlcFileGuid = dlcFile.guids.get(j);
                textDlcFileGuids += String.format(textGuidTemplate, j, dlcFileGuid.getValue());
            }

            textDlcFiles += String.format(textDlcFileTemplate, i, dlcFile.directory, dlcFile.file, dlcFile.contentID,
                                    dlcFile.inGameCommerceID, dlcFile.categoryID, dlcFile.flags, dlcFile.guids.size(),
                                    textDlcFileGuids, dlcFile.typeMask);
        }
        String dlcText = String.format(textDlcRootTemplate, dlc.guids.size(), textDlcGuids, dlc.files.size(), textDlcFiles);

        */

        String dlcJson = GsonUtils.toJSON(dlc, new Revision(Revisions.LBP2_MAX));

        return writeFile(filename, binary, dlcJson, map, farcs);
    }

    public SHA1 writeDlcHeader(String filename, DLCHeaderFile header) {
        byte[] binary = header.build();
        String json = GsonUtils.toJSON(header, new Revision(Revisions.LBP2_MAX));

        return this.writeFile(filename, binary, json, null, null);
    }

    public SHA1 writeFarc(String filename, RemoteFart farc) {
        byte[] binary = farc.build();
        String json = GsonUtils.toJSON(new MinimalFart(farc), new Revision(Revisions.LBP2_MAX));

        return this.writeFile(filename, binary, json, null, null);
    }

    public SHA1 writeMap(String filename, FileDB map, Fart[] farcs) {
        byte[] binary = map.build();
        String json = GsonUtils.toJSON(new MinimalFileDB(map), new Revision(Revisions.LBP2_MAX));

        return this.writeFile(filename, binary, json, null, farcs);
    }

    public SHA1 writeFile(String filename, byte[] binary, String json, FileDB map, Fart[] farcs) {
        SHA1 hash = SHA1.fromBuffer(binary);
        FileIO.write(binary, this.dataStorePath + "/" + hash.toString());
        FileIO.write(binary, this.debugDumpPath + "/" + this.currentFarcDataVersion + "/" + filename + "_" + hash.toString() + ".bin");

        if (map != null) {
            FileDBRow row = new FileDBRow(map, "/gamedata/data/" + filename, 1795204244, binary.length, hash,
                new GUID(RTranslationTable.makeLamsKeyID(hash.toString()) /* lol */));

            map.newFileDBRow(row);
        }

        if (farcs != null) {
            for (Fart farc : farcs) {
                farc.add(binary);
            }
        }

        if (json != null) {
            FileIO.write(json.getBytes(), this.debugDumpPath + "/" + this.currentFarcDataVersion + "/" + filename + "_" + hash.toString() + ".json");
        }

        return hash;
    }
}