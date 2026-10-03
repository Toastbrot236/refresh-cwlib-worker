package refresh.database;

import refresh.database.models.PersistentJobState;
import refresh.database.models.WorkerInfo;
import refresh.helpers.CommonConstants;
import refresh.resources.MinimalLevelData;
import refresh.resources.MinimalPlanData;
import refresh.resources.MinimalResource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;

public class GameDatabaseContext implements AutoCloseable {
    private final Connection conn;

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public GameDatabaseContext() throws SQLException {
        String url = "jdbc:postgresql://localhost/refresh";
        this.conn = DriverManager.getConnection(url, "refresh", "refresh");
    }

    public void markAssetAsScanned(String hash) throws SQLException {
        String sql = "UPDATE \"GameAssets\" SET \"ScannedByCWLibVersion\" = ? WHERE \"AssetHash\" = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hash);
            stmt.setInt(2, CommonConstants.CurrentCWLibWorkerVersion);
        }
    }

    /**
     * Returns the hashes of all level root or photo plan assets we want to scan.
     * If a GameAsset's ScannedByCWLibVersion is 0, it hasn't been scanned yet, and if it's above that but below our current version,
     * its scan is outdated; the asset should be scanned again in both cases.
     */
    // TODO consider whether we should also clear data for assets no longer used by any levels or photos
    public HashSet<String> getAssetHashesNeedingScans() throws SQLException {
        String sql = 
                """
                SELECT concatenated."AssetHash" FROM (
                    SELECT a."AssetHash", a."ScannedByCWLibVersion", l."UpdateDate" AS LastUsedAt
                    FROM "GameAssets" AS a
                    INNER JOIN "GameLevels" l
                    ON a."AssetHash" = l."RootResource"

                    UNION

                    SELECT a."AssetHash", a."ScannedByCWLibVersion", p."PublishedAt" AS LastUsedAt
                    FROM "GameAssets" AS a
                    INNER JOIN "GamePhotos" p
                    ON a."AssetHash" = p."PlanHash"
                ) AS concatenated
                WHERE concatenated."ScannedByCWLibVersion" < ?
                ORDER BY concatenated.LastUsedAt DESC
                LIMIT 100
                """;
                // No need to do any skipping, asset hashes handled here will automatically be set as handled later.
                // Also, 100 is more than enough per minute, since we will also traverse and scan dependencies for all of them.

        HashSet<String> hashes = new HashSet<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, CommonConstants.CurrentCWLibWorkerVersion);

            try(ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    hashes.add(rs.getString(1));
                }
            }
        }
        return hashes;
    }
    
    public void addOrUpdatePlanData(MinimalResource<MinimalPlanData> plan) throws SQLException {
        // Remove old data (potentially from a previous scan)
        String removePlanSql = "DELETE FROM \"GamePlanAssets\" WHERE \"PlanHash\" = ?";
        try(PreparedStatement stmt = conn.prepareStatement(removePlanSql)) {
            stmt.setString(1, plan.Hash);
            stmt.executeUpdate(removePlanSql);
        }

        // Delete old plan data so we can replace it with new one, and mark this asset as scanned by current version (if it has a GameAsset).
        String sql = 
                """
                DELETE FROM "GamePlanAssets" WHERE "PlanHash" = ?";

                INSERT INTO "GamePlanAssets" ("PlanHash", "Name", "Description", "IconHash")
                VALUES (?, ?, ?, ?);

                UPDATE "GameAssets" SET "ScannedByCWLibVersion" = ? WHERE "AssetHash" = ?;
                """;

        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plan.Hash);

            stmt.setString(2, plan.Hash);
            stmt.setString(3, plan.Content.Name);
            stmt.setString(4, plan.Content.Description);
            stmt.setString(5, plan.Content.Icon);

            stmt.setInt(6, CommonConstants.CurrentCWLibWorkerVersion);
            stmt.setString(7, plan.Hash);

            stmt.executeQuery();
        }

        // Update contributor names separately since the're stored in their own table.
        this.addOrUpdateContributorNames(plan.Hash, plan.Content.ContributorUsernames);
    }

    public void addOrUpdateLevelData(MinimalResource<MinimalLevelData> level) throws SQLException {
        // Delete old level data so we can replace it with new one, and mark this asset as scanned by current version (if it has a GameAsset).
        String insertPlanSql = 
                """
                DELETE FROM \"GameLevelAssets\" WHERE \"LevelHash\" = ?;

                INSERT INTO "GameLevelAssets" ("LevelHash", "HasValidWorldThing")
                VALUES (?, ?);

                UPDATE "GameAssets" SET "ScannedByCWLibVersion" = ? WHERE "AssetHash" = ?;
                """;

        try(PreparedStatement stmt = conn.prepareStatement(insertPlanSql)) {
            stmt.setString(1, level.Hash);

            stmt.setString(2, level.Hash);
            stmt.setBoolean(3, level.Content.HasValidWorldThing);

            stmt.setInt(4, CommonConstants.CurrentCWLibWorkerVersion);
            stmt.setString(5, level.Hash);

            stmt.executeQuery();
        }

        // Update contributor names separately since the're stored in their own table.
        this.addOrUpdateContributorNames(level.Hash, level.Content.ContributorUsernames);
    }

    private void addOrUpdateContributorNames(String assetHash, ArrayList<String> usernames) throws SQLException {
        // Don't do anything if there aren't actually any usernames
        if (usernames.size() <= 0) return;

        int index = 0;
        ArrayList<String> valueSqlParts = new ArrayList<>();
        for (String username : usernames) {
            valueSqlParts.add(String.format("\n(%s, %s, %d)", assetHash, username, index));
            index++;
        }

        // Clear previously saved usernames if there are any, and then append insertions for every username,
        // which we've built in the loop above.
        String sql = String.format(
            """
                DELETE FROM \"AssetContributorRelations\" WHERE \"AssetHash\" = %s;
                INSERT INTO \"AssetContributorRelations\" (\"AssetHash\", \"Username\", \"Index\")
                VALUES %s;
             """, assetHash, String.join(",", valueSqlParts));
        
        try(Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private void deleteWorkers() throws SQLException {
        String sql = "DELETE FROM \"Workers\" WHERE \"Class\" = 1";
        try(Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public int createWorker() throws SQLException {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        this.deleteWorkers();

        String sql =
                """
                INSERT INTO "Workers" ("Class", "CreatedAt", "LastContact")
                VALUES (?, ?, ?)
                RETURNING "WorkerId"
                """;

        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, 1);
            stmt.setTimestamp(2, now);
            stmt.setTimestamp(3, now);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Failed to insert worker");
                }
            }
        }
    }

    public WorkerInfo getWorker(int id) throws SQLException {
        String sql = "SELECT \"WorkerId\", \"Class\", \"CreatedAt\", \"LastContact\" FROM \"Workers\" WHERE \"WorkerId\" = ?";
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next()) return new WorkerInfo(rs);
                return null;
            }
        }
    }

    public boolean markWorkerContacted(int id) throws SQLException {
        WorkerInfo worker = getWorker(id);
        if(worker == null)
            return false;

        String sql = "UPDATE \"Workers\" SET \"LastContact\" = ? WHERE \"WorkerId\" = ? ";

        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, id);

            int updated = stmt.executeUpdate();
            if(updated != 1)
                throw new SQLException("Expected to update 1 row, but only updated " + updated);
        }

        return true;
    }

    public PersistentJobState getJobState(String jobId) throws SQLException {
        String sql = "SELECT \"JobId\", \"Class\", \"State\" FROM \"JobStates\" WHERE \"JobId\" = ? AND \"Class\" = 1";
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, jobId);

            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next()) return new PersistentJobState(rs);
                return null;
            }
        }
    }

    public PersistentJobState getGameAssetPatchInfo(String hash) throws SQLException {
        String sql = "SELECT \"WasScannedByCWLib\" FROM \"GameAssets\" WHERE \"AssetHash\" = ?";
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hash);

            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next()) return new PersistentJobState(rs);
                return null;
            }
        }
    }

    @Override
    public void close() throws SQLException {
        if(this.conn != null)
            this.conn.close();
    }
}
