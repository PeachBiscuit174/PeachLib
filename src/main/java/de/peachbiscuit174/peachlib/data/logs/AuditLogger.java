package de.peachbiscuit174.peachlib.data.logs;

import de.peachbiscuit174.peachlib.PeachLib;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Logs successful database modifications (WRITE, DELETE) to YAML files for server administrators.
 * Utilizes an in-memory map to prevent heavy disk I/O on every log operation.
 * Implements a FIFO (First-In-First-Out) limit to restrict file sizes.
 */
@ApiStatus.Internal
public class AuditLogger {

    private final File auditDir;
    private final int limit;

    // Dedicated single thread to process all logging sequentially without blocking the main worker
    private final ExecutorService loggingExecutor = Executors.newSingleThreadExecutor();

    private final Map<String, YamlConfiguration> cachedConfigs = new ConcurrentHashMap<>();

    // Pattern to prevent Path Traversal exploits
    private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9_\\-]+$");

    public AuditLogger(File dataFolder, int limit) {
        this.auditDir = new File(dataFolder, "AuditLogs");
        if (!this.auditDir.exists()) {
            this.auditDir.mkdirs();
        }
        this.limit = limit;
    }

    private void validateName(String name) {
        if (name == null || !VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid identifier to prevent path traversal: " + name);
        }
    }

    /**
     * Queues an operation to be logged to the YAML file.
     *
     * @param connectionId The database connection ID.
     * @param tableName    The modified table.
     * @param action       The action performed (e.g., "WRITE", "DELETE").
     * @param id           The document ID.
     * @param value        The JSON value (null for DELETE operations).
     */
    public void logOperation(String connectionId, String tableName, String action, String id, String value) {
        validateName(connectionId);

        loggingExecutor.submit(() -> {
            File logFile = new File(auditDir, connectionId + ".yml");

            YamlConfiguration config = cachedConfigs.computeIfAbsent(connectionId, k -> {
                if (logFile.exists()) {
                    return YamlConfiguration.loadConfiguration(logFile);
                }
                return new YamlConfiguration();
            });

            List<String> logs = config.getStringList("logs");
            LinkedList<String> fastLogs = new LinkedList<>(logs);

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String logEntry = String.format("[%s] Table: %s | Action: %s | ID: %s | Value: %s",
                    timestamp, tableName, action, id, (value != null ? value : "N/A"));

            fastLogs.add(logEntry);

            // FIFO Principle: Remove oldest entries if the limit is exceeded
            while (fastLogs.size() > limit) {
                fastLogs.removeFirst(); // Much faster than remove(0) on ArrayList
            }

            config.set("logs", fastLogs);
            try {
                config.save(logFile);
            } catch (IOException e) {
                PeachLib.getPlugin().getLogger().severe("Failed to save audit log: " + e.getMessage());
            }
        });
    }

    /**
     * Safely shuts down the background logging thread.
     */
    public void shutdown() {
        loggingExecutor.shutdown();
        try {
            if (!loggingExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                loggingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            loggingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}