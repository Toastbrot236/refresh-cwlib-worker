package refresh.database.models.assets;

import java.sql.Timestamp;

public class FarcHashesData {
    public int Version;
    //public int GameVersion;

    public Timestamp GeneratedAt;
    public int GeneratedUsingJobVersion;

    //public int PinGroupCount;
    //public int PinTotalCount;

    //public int SubmittedPinGroupCount;
    //public int SubmittedPinTotalCount;

    //public int AdminPinGroupCount;
    //public int AdminPinTotalCount;

    public String DlcHeaderHash;
    public String DlcResourceHash;
    public String PinFarcHash;
    public String PinResourceHash;
    public String PackFarcHash;
    public String PackResourceHash;
    public String MapHash;

    public String EnglishTransTableHash;
}
