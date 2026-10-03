package refresh.database.models.assets;

import java.sql.ResultSet;
import java.sql.SQLException;

import refresh.helpers.CommonConstants;

public class GameAsset {
    public String AssetHash;
    public int ScannedByCWLibVersion;

    // TODO once we support editing assets too
    //public String OriginalHash;
    //public String PatchedHash;
    //public long AppliedPatchFlags;

    public GameAsset(ResultSet rs) throws SQLException {
        this.AssetHash = rs.getString(1);
        this.ScannedByCWLibVersion = rs.getInt(2);
    }

    public GameAsset(String hash) {
        this.AssetHash = hash;
        this.ScannedByCWLibVersion = CommonConstants.CurrentCWLibWorkerVersion;
    }
}
