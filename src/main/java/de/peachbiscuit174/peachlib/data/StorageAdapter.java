package de.peachbiscuit174.peachlib.data;

import de.peachbiscuit174.peachlib.api.managers.Credentials;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

/**
 * Core backend interface for all storage types (JDBC or I/O).
 * Implementations process a unified document model: id (String), value (String/JSON), timestamp (Long).
 */
@ApiStatus.Internal
public interface StorageAdapter {

    void connect(Credentials credentials) throws Exception;

    void createTableIfNotExists(String tableName) throws Exception;

    void write(String tableName, String id, String jsonValue, long timestamp) throws Exception;

    void delete(String tableName, String id) throws Exception;

    String read(String tableName, String id) throws Exception;

    Set<String> getAllPrimaryKeys(String tableName) throws Exception;

    /**
     * Gracefully closes the connection to prevent memory leaks.
     */
    void disconnect() throws Exception;
}