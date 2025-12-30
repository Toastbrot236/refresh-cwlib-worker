package refresh.database;

import refresh.database.models.PersistentJobState;
import refresh.database.models.WorkerInfo;
import refresh.database.models.GameItem;
import java.sql.*;
import java.time.LocalDateTime;

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

    private GameItem getGameItem(String planHash) throws SQLException {
        String sql = 
                """
                SELECT "PlanHash", "IconHash", "Title", "Description", "CreatorName", 
                    "ContributorNames", "IsGamePhoto", "IsCameraPhoto", "IsUserCreation"
                FROM \"GameItems\" 
                WHERE \"PlanHash\" = ?
                """;
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, planHash);

            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next()) return new GameItem(rs);
                return null;
            }
        }
    }

    public boolean insertGameItem(GameItem item) throws SQLException {
        // check if row already exists
        GameItem existing = this.getGameItem(item.PlanHash);
        if (existing != null) return false; // row already exists; don't think there is any need to update it

        String sql = 
                """
                INSERT INTO "GameItems" ("PlanHash", "IconHash", "Title", "Description", "CreatorName", 
                    "ContributorNames", "IsGamePhoto", "IsCameraPhoto", "IsUserCreation")
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING "PlanHash"
                """;

        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.PlanHash);
            stmt.setString(2, item.IconHash);
            stmt.setString(3, item.Title);
            stmt.setString(4, item.Description);
            stmt.setString(5, item.CreatorName);
            stmt.setArray(6, conn.createArrayOf("TEXT", item.ContributorNames));
            stmt.setBoolean(7, item.IsGamePhoto);
            stmt.setBoolean(8, item.IsCameraPhoto);
            stmt.setBoolean(9, item.IsUserCreation);

            try(ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void close() throws SQLException {
        if(this.conn != null)
            this.conn.close();
    }
}
