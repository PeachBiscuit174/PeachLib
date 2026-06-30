package de.peachbiscuit174.peachlib.data.cache;

import de.peachbiscuit174.peachlib.PeachLib;
import de.peachbiscuit174.peachlib.api.managers.Credentials;
import de.peachbiscuit174.peachlib.api.managers.DataManager;
import de.peachbiscuit174.peachlib.data.StorageAdapter;
import de.peachbiscuit174.peachlib.data.backends.SQLiteAdapter;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages an opt-in local SQLite cache for ultra-fast read operations.
 * Implements auto-sync and Time-To-Live (TTL) background cleanup.
 */
@ApiStatus.Internal
public class TableCache {

    private final String cacheConnectionId;
    private final String tableName;
    private final StorageAdapter sqliteCacheAdapter;
    private final DataManager dataManager;

    private final Map<String, Long> lastAccessMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final long TTL_MILLIS = 10 * 60 * 1000L;

    public TableCache(DataManager dataManager, File dataFolder, String connectionId, String tableName) throws Exception {
        this.dataManager = dataManager;
        this.tableName = tableName;
        this.cacheConnectionId = connectionId + "_cache";

        this.sqliteCacheAdapter = new SQLiteAdapter(dataFolder);

        Credentials creds = new Credentials(null, 0, cacheConnectionId, null, null, false);
        this.sqliteCacheAdapter.connect(creds);
        this.sqliteCacheAdapter.createTableIfNotExists(tableName);

        long startupTime = System.currentTimeMillis();
        for (String key : this.sqliteCacheAdapter.getAllPrimaryKeys(tableName)) {
            lastAccessMap.put(key, startupTime);
        }

        startAutoCleanupTask();
    }

    public String getCachedValue(String id) {
        try {
            String value = sqliteCacheAdapter.read(tableName, id);
            if (value != null) {
                lastAccessMap.put(id, System.currentTimeMillis());
                return value;
            }
        } catch (Exception e) {
            PeachLib.getPlugin().getLogger().log(Level.WARNING, "Error reading from cache for table " + tableName, e);
        }
        return null;
    }

    public void saveToCache(String id, String jsonValue) {
        try {
            sqliteCacheAdapter.write(tableName, id, jsonValue, System.currentTimeMillis());
            lastAccessMap.put(id, System.currentTimeMillis());
        } catch (Exception e) {
            PeachLib.getPlugin().getLogger().log(Level.WARNING, "Error writing to cache for table " + tableName, e);
        }
    }

    public void removeFromCache(String id) {
        try {
            sqliteCacheAdapter.delete(tableName, id);
            lastAccessMap.remove(id);
        } catch (Exception e) {
            PeachLib.getPlugin().getLogger().log(Level.WARNING, "Error deleting from cache for table " + tableName, e);
        }
    }

    private void startAutoCleanupTask() {
        scheduler.scheduleAtFixedRate(this::performTTLCleanup, 5, 5, TimeUnit.MINUTES);
    }

    public void performTTLCleanup() {
        long currentTime = System.currentTimeMillis();
        try {
            for (String key : lastAccessMap.keySet()) {
                Long lastAccess = lastAccessMap.get(key);
                if (lastAccess == null || (currentTime - lastAccess) > TTL_MILLIS) {
                    removeFromCache(key);
                }
            }
        } catch (Exception e) {
            PeachLib.getPlugin().getLogger().log(Level.WARNING, "Error performing TTL cleanup for cache " + tableName, e);
        }
    }

    /**
     * Safely terminates the cache scheduler and closes the database connection.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            sqliteCacheAdapter.disconnect();
        } catch (Exception e) {
            PeachLib.getPlugin().getLogger().log(Level.WARNING, "Error disconnecting cache adapter", e);
        }
    }
}