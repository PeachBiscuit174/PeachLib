package de.peachbiscuit174.peachlib.data.backends;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.peachbiscuit174.peachlib.api.managers.Credentials;
import de.peachbiscuit174.peachlib.data.StorageAdapter;
import org.jetbrains.annotations.ApiStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Raw JDBC implementation for MySQL and MariaDB databases.
 * Utilizes HikariCP for connection pooling and efficient async operations.
 */
@ApiStatus.Internal
public class MySQLAdapter implements StorageAdapter {

    private HikariDataSource dataSource;
    private static final Pattern VALID_TABLE_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    @Override
    public void connect(Credentials credentials) throws Exception {
        HikariConfig config = new HikariConfig();

        String jdbcUrl = "jdbc:mysql://" + credentials.host() + ":" + credentials.port() + "/" + credentials.database();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(credentials.username());
        config.setPassword(credentials.password());

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useSSL", String.valueOf(credentials.useSsl()));
        config.addDataSourceProperty("requireSSL", String.valueOf(credentials.useSsl()));

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);

        this.dataSource = new HikariDataSource(config);
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
    public void createTableIfNotExists(String tableName) throws Exception {
        validateTableName(tableName);

        // Primary approach: Utilize native JSON type for modern database servers
        String sqlJSON = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                "`id` VARCHAR(191) NOT NULL, " +
                "`value` JSON NOT NULL, " +
                "`timestamp` BIGINT NOT NULL, " +
                "PRIMARY KEY (`id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        // Fallback approach: Utilize LONGTEXT for older MySQL/MariaDB versions lacking JSON support
        String sqlFallback = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                "`id` VARCHAR(191) NOT NULL, " +
                "`value` LONGTEXT NOT NULL, " +
                "`timestamp` BIGINT NOT NULL, " +
                "PRIMARY KEY (`id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sqlJSON)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                try (PreparedStatement fallbackStmt = conn.prepareStatement(sqlFallback)) {
                    fallbackStmt.executeUpdate();
                }
            }
        }
    }

    @Override
    public void write(String tableName, String id, String jsonValue, long timestamp) throws Exception {
        validateTableName(tableName);
        String sql = "INSERT INTO `" + tableName + "` (`id`, `value`, `timestamp`) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `timestamp` = VALUES(`timestamp`);";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, jsonValue);
            stmt.setLong(3, timestamp);
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(String tableName, String id) throws Exception {
        validateTableName(tableName);
        String sql = "DELETE FROM `" + tableName + "` WHERE `id` = ?;";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public String read(String tableName, String id) throws Exception {
        validateTableName(tableName);
        String sql = "SELECT `value` FROM `" + tableName + "` WHERE `id` = ?;";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
    public Set<String> getAllPrimaryKeys(String tableName) throws Exception {
        validateTableName(tableName);
        Set<String> keys = new HashSet<>();
        String sql = "SELECT `id` FROM `" + tableName + "`;";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                keys.add(rs.getString("id"));
            }
        }
        return keys;
    }

    @Override
    public void disconnect() throws Exception {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}