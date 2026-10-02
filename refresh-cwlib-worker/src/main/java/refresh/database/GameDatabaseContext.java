package refresh.database;

import refresh.database.models.PersistentJobState;
import refresh.database.models.WorkerInfo;
import refresh.exceptions.MissingDatabaseMigrationException;
import refresh.resources.MinimalLevelData;
import refresh.resources.MinimalPlanData;
import refresh.resources.MinimalResource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

public class GameDatabaseContext implements AutoCloseable {
    private final Connection conn;

    public String[] requiredMigrations = new String[] {
        "20250611223701_InitialFromRealm",
        "WhateverAddsCWLibStuffToDBlol",
    };

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public GameDatabaseContext() throws SQLException, MissingDatabaseMigrationException {
        String url = "jdbc:postgresql://localhost/refresh";
        this.conn = DriverManager.getConnection(url, "refresh", "refresh");
    }

    // Call once on program init
    public void EnsureMigrationsAreApplied() throws SQLException, MissingDatabaseMigrationException {
        String sql = "SELECT MigrationId FROM \"__EFMigrationsHistory\"";
        ArrayList<String> migrationIds = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            try(ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    migrationIds.add(rs.getString(1));
                }
            }
        }

        // Instead of immediately throwing on the first ID we find, we should instead gather all missing IDs
        // and then list them all in the exception message.
        ArrayList<String> missingIds = new ArrayList<>();
        for (String expectedId : this.requiredMigrations) {

            boolean idExists = false;
            for (String migrationId : migrationIds) {
                if (migrationId.equals(expectedId)) {
                    idExists = true;
                    break;
                }
            }

            if (!idExists) {
                missingIds.add(expectedId);
            }
        }
        if (missingIds.size() > 0) {
            String message = 
                "The database is missing the following migrations: '" +
                Arrays.toString(missingIds.toArray()) +
                ". Please apply them by using 'dotnet ef database update' on the game server.";
            throw new MissingDatabaseMigrationException(message);
        }
    }

    public void addOrUpdatePlanData(MinimalResource<MinimalPlanData> plan) throws SQLException {
        // Remove old data (potentially from a previous scan)
        String removePlanSql = "DELETE FROM \"GamePlanAssets\" WHERE \"PlanHash\" = ?";
        try(PreparedStatement stmt = conn.prepareStatement(removePlanSql)) {
            stmt.setString(1, plan.Hash);
            stmt.executeUpdate(removePlanSql);
        }

        // Now insert
        String insertPlanSql = 
                """
                INSERT INTO "GamePlanAssets" ("PlanHash", "Name", "Description", "IconHash")
                VALUES (?, ?, ?, ?)
                """;

        try(PreparedStatement stmt = conn.prepareStatement(insertPlanSql)) {
            stmt.setString(1, plan.Hash);
            stmt.setString(2, plan.Content.Name);
            stmt.setString(3, plan.Content.Description);
            stmt.setString(4, plan.Content.Icon);
        }

        // Update contributor names separately since the're stored in their own table.
        this.addOrUpdateContributorNames(plan.Hash, plan.Content.ContributorUsernames);
    }

    public void addOrUpdateLevelData(MinimalResource<MinimalLevelData> level) throws SQLException {
        // Remove old data (potentially from a previous scan)
        String removePlanSql = "DELETE FROM \"GameLevelAssets\" WHERE \"LevelHash\" = ?";
        try(PreparedStatement stmt = conn.prepareStatement(removePlanSql)) {
            stmt.setString(1, level.Hash);
            stmt.executeUpdate(removePlanSql);
        }

        // Now insert
        String insertPlanSql = 
                """
                INSERT INTO "GameLevelAssets" ("LevelHash", "HasValidWorldThing")
                VALUES (?, ?)
                """;

        try(PreparedStatement stmt = conn.prepareStatement(insertPlanSql)) {
            stmt.setString(1, level.Hash);
            stmt.setBoolean(2, level.Content.HasValidWorldThing);
        }

        // Update contributor names separately since the're stored in their own table.
        this.addOrUpdateContributorNames(level.Hash, level.Content.ContributorUsernames);
    }

    private void addOrUpdateContributorNames(String assetHash, ArrayList<String> usernames) throws SQLException {
        // Clear previously saved usernames if there are any
        String removeContributorsSql = "DELETE FROM \"AssetContributorRelations\" WHERE \"AssetHash\" = ?";
        try(PreparedStatement stmt = conn.prepareStatement(removeContributorsSql)) {
            stmt.setString(1, assetHash);
            stmt.executeUpdate(removeContributorsSql);
        }

        // TODO lookup user ID for each one and then reference them in these relations
        // (renames would be way smaller issues then, also less DB calls when fetching)

        // TODO try to insert all names in just one DB call
        for (String username : usernames) {
            String insertContributorsSql = 
                """
                INSERT INTO "AssetContributorRelations" ("AssetHash", "Username")
                VALUES (?, ?)
                """;

            try(PreparedStatement stmt = conn.prepareStatement(insertContributorsSql)) {
                stmt.setString(1, assetHash);
                stmt.setString(2, username);
            }
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

        String sql = "UPDATE \"Workers\" SET \"LastContact\" = ? WHERE \"WorkerId\" = ?";

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
