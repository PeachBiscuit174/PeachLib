package de.peachbiscuit174.peachlib.data.backends;

import de.peachbiscuit174.peachlib.api.managers.Credentials;
import de.peachbiscuit174.peachlib.data.StorageAdapter;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Raw JDBC implementation for local SQLite databases.
 * Operates on a single connection optimized with WAL (Write-Ahead Logging) mode.
 */
@ApiStatus.Internal
public class SQLiteAdapter implements StorageAdapter {

    private Connection connection;
    private final File dataFolder;
    private static final Pattern VALID_TABLE_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    public SQLiteAdapter(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public synchronized void connect(Credentials credentials) throws Exception {
        File dbDir = new File(dataFolder, "Databases");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        File dbFile = new File(dbDir, credentials.database() + ".db");
        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection(jdbcUrl);

        try (PreparedStatement pragmaStmt = connection.prepareStatement("PRAGMA journal_mode=WAL;")) {
            pragmaStmt.execute();
        }
        try (PreparedStatement pragmaStmt = connection.prepareStatement("PRAGMA synchronous=NORMAL;")) {
            pragmaStmt.execute();
        }
    }

    /**
     * Validates the table name to prevent SQL Injection via string concatenation.
     */
    private void validateTableName(String tableName) {
        if (tableName == null || !VALID_TABLE_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Invalid table name: " + tableName + ". Only alphanumeric characters and underscores are allowed.");
        }
    }

    @Override
    public synchronized void createTableIfNotExists(String tableName) throws Exception {
        validateTableName(tableName);

        // SQLite lacks a native JSON type; TEXT is the standard architectural equivalent.
        String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                "`id` VARCHAR(191) PRIMARY KEY NOT NULL, " +
                "`value` TEXT NOT NULL, " +
                "`timestamp` BIGINT NOT NULL" +
                ");";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    @Override
    public synchronized void write(String tableName, String id, String jsonValue, long timestamp) throws Exception {
        validateTableName(tableName);
        String sql = "INSERT INTO `" + tableName + "` (`id`, `value`, `timestamp`) VALUES (?, ?, ?) " +
                "ON CONFLICT(`id`) DO UPDATE SET `value` = excluded.`value`, `timestamp` = excluded.`timestamp`;";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, jsonValue);
            stmt.setLong(3, timestamp);
            stmt.executeUpdate();
        }
    }

    @Override
    public synchronized void delete(String tableName, String id) throws Exception {
        validateTableName(tableName);
        String sql = "DELETE FROM `" + tableName + "` WHERE `id` = ?;";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public synchronized String read(String tableName, String id) throws Exception {
        validateTableName(tableName);
        String sql = "SELECT `value` FROM `" + tableName + "` WHERE `id` = ?;";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        }
        return null;
    }

    @Override
    public synchronized Set<String> getAllPrimaryKeys(String tableName) throws Exception {
        validateTableName(tableName);
        Set<String> keys = new HashSet<>();
        String sql = "SELECT `id` FROM `" + tableName + "`;";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                keys.add(rs.getString("id"));
            }
        }
        return keys;
    }

    @Override
    public synchronized void disconnect() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}