package de.peachbiscuit174.peachlib.data;

import org.jetbrains.annotations.ApiStatus;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an asynchronous queue task for the DataWorker.
 * Ensures read-after-write consistency by processing operations synchronously.
 *
 * @param type         The operation type.
 * @param connectionId The unique ID of the storage connection.
 * @param tableName    The target table.
 * @param id           The primary key. Can be null for GET_ALL_KEYS.
 * @param jsonValue    The serialized JSON value. Null for READ/DELETE/GET_ALL_KEYS.
 * @param timestamp    The current time provided by the TimeProvider.
 * @param future       The future to complete once processed.
 */
@ApiStatus.Internal
public record Task(
        TaskType type,
        String connectionId,
        String tableName,
        String id,
        String jsonValue,
        long timestamp,
        CompletableFuture<Object> future
) {
    public enum TaskType {
        WRITE,
        DELETE,
        READ,
        GET_ALL_KEYS
    }
}