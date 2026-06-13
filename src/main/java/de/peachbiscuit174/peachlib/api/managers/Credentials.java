package de.peachbiscuit174.peachlib.api.managers;

/**
 * Holds credentials and connection information for external databases.
 * For local storage (SQLite, YAML, FileTree), host, port, username, and password can be null or default values.
 *
 * @param host     The database host (e.g., localhost)
 * @param port     The database port (e.g., 3306)
 * @param database The database name or file name
 * @param username The database user
 * @param password The database password
 * @param useSsl   Whether to attempt an SSL connection (optional, falls back if unsupported)
 */
public record Credentials(
        String host,
        int port,
        String database,
        String username,
        String password,
        boolean useSsl
) {}