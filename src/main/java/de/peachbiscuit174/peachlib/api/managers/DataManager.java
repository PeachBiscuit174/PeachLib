package de.peachbiscuit174.peachlib.api.managers;

import de.peachbiscuit174.peachlib.PeachLib;
import de.peachbiscuit174.peachlib.api.PeachLibAPI;
import de.peachbiscuit174.peachlib.api.data.Table;
import de.peachbiscuit174.peachlib.data.StorageAdapter;
import de.peachbiscuit174.peachlib.data.Task;
import de.peachbiscuit174.peachlib.data.cache.TableCache;
import de.peachbiscuit174.peachlib.data.logs.AuditLogger;
import de.peachbiscuit174.peachlib.data.logs.QueueLogger;
import de.peachbiscuit174.peachlib.data.queue.DataWorker;
import de.peachbiscuit174.peachlib.data.time.TimeProvider;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Central entry point for data management in PeachLib.
 * Handles storage registration, queue processing, and provides Table wrappers.
 */
public class DataManager {

    private final Map<String, StorageAdapter> adapters = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Task> taskQueue = new ConcurrentLinkedQueue<>();
    private final List<TableCache> activeCaches = new CopyOnWriteArrayList<>();

    private final TimeProvider timeProvider;
    private final QueueLogger queueLogger;
    private final AuditLogger auditLogger;
    private final DataWorker dataWorker;
    private final File dataFolder;

    private int auditLogLimit = 500;

    /**
     * Do not USE!
     * @param pluginDataFolder
     */
    @ApiStatus.Internal
    public DataManager(File pluginDataFolder) {
        this.dataFolder = pluginDataFolder;
        this.timeProvider = new TimeProvider();
        this.timeProvider.syncAsync();

        this.queueLogger = new QueueLogger(pluginDataFolder);
        this.auditLogger = new AuditLogger(pluginDataFolder, auditLogLimit);
        this.dataWorker = new DataWorker(this, taskQueue, queueLogger, auditLogger);

        PeachLibAPI.getSchedulerManager().getScheduler()
                .runAsyncRepeating(dataWorker, 0, 50, TimeUnit.MILLISECONDS);
    }

    public synchronized void registerStorage(String connectionId, StorageType type, Credentials credentials) throws Exception {
        if (adapters.containsKey(connectionId)) {
            return;
        }
        StorageAdapter adapter = createAdapterForType(type);
        adapter.connect(credentials);
        adapters.put(connectionId, adapter);
    }

    public Table getTable(String connectionId, String tableName) {
        if (!adapters.containsKey(connectionId)) {
            throw new IllegalArgumentException("Connection ID '" + connectionId + "' is not registered.");
        }
        try {
            adapters.get(connectionId).createTableIfNotExists(tableName);
        } catch (Exception e) {
            PeachLib.getPlugin().getLogger().log(Level.SEVERE, "Failed to create table: " + tableName, e);
        }
        return new Table(this, connectionId, tableName);
    }

    @ApiStatus.Internal
    public void registerCache(TableCache cache) {
        this.activeCaches.add(cache);
    }

    public void setAuditLogLimit(int limit) {
        this.auditLogLimit = limit;
    }

    public void recoverCrashLogs() {
        queueLogger.recover(this);
    }

    /**
     * Safely flushes queues and closes all database connections.
     * Prevents data loss during server restarts.
     */
    public void shutdown() {
        // 1. Force worker to finish all queued tasks immediately (prevent dataloss)
        dataWorker.flushAll();

        // 2. Safely close logging threads to prevent thread leaks
        auditLogger.shutdown();

        // 3. Stop all caching background schedules
        for (TableCache cache : activeCaches) {
            cache.shutdown();
        }
        activeCaches.clear();

        // 4. Finally disconnect adapters safely
        for (Map.Entry<String, StorageAdapter> entry : adapters.entrySet()) {
            try {
                entry.getValue().disconnect();
            } catch (Exception e) {
                PeachLib.getPlugin().getLogger().log(Level.SEVERE, "Failed to close StorageAdapter: " + entry.getKey(), e);
            }
        }
        adapters.clear();
    }

    @ApiStatus.Internal
    public File getDataFolder() {
        return dataFolder;
    }

    @ApiStatus.Internal
    public TimeProvider getTimeProvider() {
        return timeProvider;
    }

    @ApiStatus.Internal
    public StorageAdapter getAdapter(String connectionId) {
        return adapters.get(connectionId);
    }

    @ApiStatus.Internal
    public void enqueueTask(Task task) {
        queueLogger.logTask(task);
        taskQueue.add(task);
    }

    private StorageAdapter createAdapterForType(StorageType type) {
        return switch (type) {
            case MYSQL -> new de.peachbiscuit174.peachlib.data.backends.MySQLAdapter();
            case SQLITE -> new de.peachbiscuit174.peachlib.data.backends.SQLiteAdapter(this.dataFolder);
            case FILETREE -> new de.peachbiscuit174.peachlib.data.backends.FileTreeAdapter(this.dataFolder);
            case YAML -> new de.peachbiscuit174.peachlib.data.backends.YAMLAdapter(this.dataFolder);
        };
    }
}