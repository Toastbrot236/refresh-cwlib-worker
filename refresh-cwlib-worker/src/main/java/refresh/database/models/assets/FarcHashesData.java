package refresh.database.models.assets;

import java.sql.Timestamp;

public class FarcHashesData {
    public int Version;

    public Timestamp GeneratedAt;
    public int GeneratedUsingJobVersion;

    public String DlcHeaderHash;
    public String DlcResourceHash;
    public String PinFarcHash;
    public String PinResourceHash;
    public String PackFarcHash;
    public String PackResourceHash;
    public String MapHash;

    public String EnglishTransTableHash;
}
