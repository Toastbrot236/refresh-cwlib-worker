package refresh.database.models.assets;

import java.sql.Timestamp;

import refresh.enums.PinGroupOrigin;

public class GamePinGroup {
    public long ProgressType; // PK
    public String CustomGroupId;

    public boolean isProgressDescending;
    public boolean isProgressUniversal;

    public byte Category;
    public int InitialProgress;

    public String GroupName;
    public String GroupDescription;
    public PinGroupOrigin Origin;

    public boolean ShowInLBP2;
    public boolean ShowInLBP3;
    public boolean ShowInLBPVita;
    public boolean ShowInBetaBuild;

    public String CreatedByUserId;
    public String FirstApprovedByUserId;
    public String LastUpdateApprovedByUserId;

    public Timestamp FirstSubmittedAt;
    public Timestamp FirstApprovedAt;

    public Timestamp LastUpdateSubmittedAt;
    public Timestamp LastUpdateApprovedAt;

    public int Version;
    public String Changelog;
}
