package refresh.database.models.assets;

import java.sql.Timestamp;

public class GamePin {
    public long PinID; // PK
    public long TargetValue; // PK

    public long ProgressType; // FK

    public long NameLamsKey;
    public String EnglishName;

    public long DescriptionLamsKey;
    public String EnglishDescription;

    public String IconHash;
    public String IconGuid;

    public boolean UnlocksTrophy;

    public Timestamp CreatedAt;
    public Timestamp LastUpdatedAt;
}
