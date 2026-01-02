package refresh.database.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class GameInnerLevel {
    public int InnerId;
    public int AdventureId;
    public Date PublishedAt;
    public Date MetadataUpdatedAt;
    public Date RootResourceUpdatedAt;
    public boolean DiscoveredFromPublish;
    public String Title;
    public String IconHash;
    public String Description;
    public int LevelType;
    public int MinPlayers;
    public int MaxPlayers;
    public boolean EnforceMinMaxPlayers;
    public boolean RequiresMoveController;
    public String RootResource;
    public float LocationX;
    public float LocationY;
    public float LocationZ;
    public byte BadgeSize;
    public String[] Labels;
    public String[] ContributorNames;
    public boolean IsModded;

    public GameInnerLevel(ResultSet rs) throws SQLException {
        this.InnerId = rs.getInt(1);
        this.AdventureId = rs.getInt(2);
        this.PublishedAt = rs.getDate(3);
        this.MetadataUpdatedAt = rs.getDate(4);
        this.RootResourceUpdatedAt = rs.getDate(5);
        this.DiscoveredFromPublish = rs.getBoolean(6);
        this.Title = rs.getString(7);
        this.IconHash = rs.getString(8);
        this.Description = rs.getString(9);
        this.LevelType = rs.getInt(10);
        this.MinPlayers = rs.getInt(11);
        this.MaxPlayers = rs.getInt(12);
        this.EnforceMinMaxPlayers = rs.getBoolean(13);
        this.RequiresMoveController = rs.getBoolean(14);
        this.RootResource = rs.getString(15);
        this.LocationX = rs.getFloat(16);
        this.LocationY = rs.getFloat(17);
        this.LocationZ = rs.getFloat(18);
        this.BadgeSize = rs.getByte(19);
        this.Labels = (String[])rs.getArray(20).getArray();
        this.ContributorNames = (String[])rs.getArray(21).getArray();
        this.IsModded = rs.getBoolean(22);
    }

    public GameInnerLevel(int pInnerId, int pAdventureId, Date pPublishedAt, Date pMetadataUpdatedAt, Date pRootResourceUpdatedAt,
        boolean pDiscoveredFromPublish, String pTitle, String pIconHash, String pDescription, int pLevelType, 
        int pMinPlayers, int pMaxPlayers, boolean pEnforceMinMaxPlayers, boolean pRequiresMoveController, String pRootResource, 
        float pLocationX, float pLocationY, float pLocationZ, byte pBadgeSize, String[] pLabels, String[] pContributorNames, 
        boolean pIsModded) {

        this.InnerId = pInnerId;
        this.AdventureId = pAdventureId;
        this.PublishedAt = pPublishedAt;
        this.MetadataUpdatedAt = pMetadataUpdatedAt;
        this.RootResourceUpdatedAt = pRootResourceUpdatedAt;
        this.DiscoveredFromPublish = pDiscoveredFromPublish;
        this.Title = pTitle;
        this.IconHash = pIconHash;
        this.Description = pDescription;
        this.LevelType = pLevelType;
        this.MinPlayers = pMinPlayers;
        this.MaxPlayers = pMaxPlayers;
        this.EnforceMinMaxPlayers = pEnforceMinMaxPlayers;
        this.RequiresMoveController = pRequiresMoveController;
        this.RootResource = pRootResource;
        this.LocationX = pLocationX;
        this.LocationY = pLocationY;
        this.LocationZ = pLocationZ;
        this.BadgeSize = pBadgeSize;
        this.Labels = pLabels;
        this.ContributorNames = pContributorNames;
        this.IsModded = pIsModded;
    }
}
