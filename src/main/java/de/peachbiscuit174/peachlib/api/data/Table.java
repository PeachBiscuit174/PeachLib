package de.peachbiscuit174.peachlib.api.data;

import de.peachbiscuit174.peachlib.PeachLib;
import de.peachbiscuit174.peachlib.api.managers.DataManager;
import de.peachbiscuit174.peachlib.data.Task;
import de.peachbiscuit174.peachlib.data.cache.TableCache;
import org.jetbrains.annotations.ApiStatus;

import com.google.gson.Gson;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Wrapper class representing a specific table or collection in the database.
 * Provides fully asynchronous methods for JSON document management.
 */
public class Table {

    private final DataManager dataManager;
    private final String connectionId;
    private final String tableName;
    private final Gson gson;

    private TableCache cache;

    /**
     * Internal constructor utilized by DataManager.
     */
    @ApiStatus.Internal
    public Table(DataManager dataManager, String connectionId, String tableName) {
        this.dataManager = dataManager;
        this.connectionId = connectionId;
        this.tableName = tableName;
        this.gson = new Gson();
    }

    /**
     * Enables the local SQLite cache for this table (ultra-fast reads, auto-sync, TTL).
     * Synchronized to prevent Race Conditions if multiple threads try to enable the cache simultaneously.
     */
    public synchronized void enableCache() {
        if (this.cache != null) return;
        try {
            this.cache = new TableCache(dataManager, dataManager.getDataFolder(), connectionId, tableName);
            this.dataManager.registerCache(this.cache); // Ensure DataManager tracks it for shutdown!
        } catch (Exception e) {
            PeachLib.getPlugin().getLogger().log(Level.SEVERE, "Failed to enable cache for table: " + tableName, e);
        }
    }

    /**
     * Asynchronously writes an object to the database as a JSON document.
     *
     * @param id    The primary key.
     * @param value The object to be serialized.
     * @return A CompletableFuture completing upon operation finish.
     */
    public CompletableFuture<Void> set(String id, Object value) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        String jsonValue = gson.toJson(value);

        long currentTimestamp = dataManager.getTimeProvider().getCurrentTime();

        if (cache != null) {
            cache.saveToCache(id, jsonValue);
        }

        Task task = new Task(Task.TaskType.WRITE, connectionId, tableName, id, jsonValue, currentTimestamp, future);
        dataManager.enqueueTask(task);

        return future.thenApply(res -> null);
    }

    /**
     * Asynchronously reads a document from the database and deserializes it.
     * Checks the ultra-fast local SQLite cache first if enabled.
     *
     * @param id    The primary key.
     * @param clazz The target class type for deserialization.
     * @return A CompletableFuture containing the object, or null if not found.
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> get(String id, Class<T> clazz) {
        if (cache != null) {
            String cachedJson = cache.getCachedValue(id);
            if (cachedJson != null) {
                return CompletableFuture.completedFuture(gson.fromJson(cachedJson, clazz));
            }
        }

        CompletableFuture<Object> future = new CompletableFuture<>();
        Task task = new Task(Task.TaskType.READ, connectionId, tableName, id, null, 0L, future);
        dataManager.enqueueTask(task);

        return future.thenApply(jsonStr -> {
            if (jsonStr == null) return null;
            String resultStr = (String) jsonStr;

            if (cache != null) {
                cache.saveToCache(id, resultStr);
            }

            return gson.fromJson(resultStr, clazz);
        });
    }

    /**
     * Asynchronously deletes a document from the database and local cache.
     *
     * @param id The primary key.
     * @return A CompletableFuture completing upon deletion.
     */
    public CompletableFuture<Void> delete(String id) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        long currentTimestamp = dataManager.getTimeProvider().getCurrentTime();

        if (cache != null) {
            cache.removeFromCache(id);
        }

        Task task = new Task(Task.TaskType.DELETE, connectionId, tableName, id, null, currentTimestamp, future);
        dataManager.enqueueTask(task);

        return future.thenApply(res -> null);
    }

    /**
     * Asynchronously retrieves all primary keys within this table.
     *
     * @return A CompletableFuture containing a Set of all keys.
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Set<String>> getKeys() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Task task = new Task(Task.TaskType.GET_ALL_KEYS, connectionId, tableName, null, null, 0L, future);
        dataManager.enqueueTask(task);

        return future.thenApply(res -> (Set<String>) res);
    }
}