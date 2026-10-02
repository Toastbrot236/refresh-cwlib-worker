package refresh.database.models.assets;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GameAsset {
    public String AssetHash;
    public boolean WasScannedByCWLib;

    // TODO once we support editing assets too
    //public String OriginalHash;
    //public String PatchedHash;
    //public long AppliedPatchFlags;

    public GameAsset(ResultSet rs, String hash) throws SQLException {
        this.AssetHash = hash;
        this.WasScannedByCWLib = rs.getBoolean(1);
    }
}
