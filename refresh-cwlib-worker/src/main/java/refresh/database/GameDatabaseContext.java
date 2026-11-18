package refresh.database;

import refresh.database.models.PersistentJobState;
import refresh.database.models.WorkerInfo;
import refresh.database.models.assets.FarcHashesData;

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

    public int saveFarcHashes(FarcHashesData hashes) throws SQLException {
        hashes.GeneratedAt = Timestamp.valueOf(LocalDateTime.now());

        String sql =
                """
                INSERT INTO "FarcHashesData" ("GeneratedAt", "GeneratedUsingJobVersion", "DlcHeaderHash", "DlcResourceHash",
                    "PinFarcHash", "PinResourceHash", "PackFarcHash", "PackResourceHash", "MapHash", "EnglishTransTableHash")
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING "Version"
                """;

        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, hashes.GeneratedAt);
            stmt.setInt(2, hashes.GeneratedUsingJobVersion);
            stmt.setString(3, hashes.DlcHeaderHash);
            stmt.setString(4, hashes.DlcResourceHash);
            stmt.setString(5, hashes.PinFarcHash);
            stmt.setString(6, hashes.PinResourceHash);
            stmt.setString(7, hashes.PackFarcHash);
            stmt.setString(8, hashes.PackResourceHash);
            stmt.setString(9, hashes.MapHash);
            stmt.setString(10, hashes.EnglishTransTableHash);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    hashes.Version = rs.getInt(1);
                    return hashes.Version;
                } else {
                    throw new SQLException("Failed to insert farc hashes");
                }
            }
        }
    }

    @Override
    public void close() throws SQLException {
        if(this.conn != null)
            this.conn.close();
    }
}
