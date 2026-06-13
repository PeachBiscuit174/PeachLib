package de.peachbiscuit174.peachlib.data.logs;

import de.peachbiscuit174.peachlib.PeachLib;
import de.peachbiscuit174.peachlib.api.managers.DataManager;
import de.peachbiscuit174.peachlib.data.Task;
import org.jetbrains.annotations.ApiStatus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;

/**
 * Handles crash-safety by writing critical operations (WRITE, DELETE) to a temporary WAL (Write-Ahead-Log)
 * before they are processed by the worker. Allows recovery in case of a server crash.
 */
@ApiStatus.Internal
public class QueueLogger {

    private final File logFile;
    private final File tempLogFile;
    private BufferedWriter writer;

    public QueueLogger(File dataFolder) {
        File logsDir = new File(dataFolder, "QueueLogs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        this.logFile = new File(logsDir, "queue_cache.log");
        this.tempLogFile = new File(logsDir, "queue_cache.tmp");

        initWriter();
    }

    private void initWriter() {
        try {
            if (this.writer != null) {
                this.writer.close();
            }
            // Ensure UTF-8 when writing to prevent OS-dependent character corruption
            this.writer = new BufferedWriter(new FileWriter(this.tempLogFile, StandardCharsets.UTF_8, true));
        } catch (IOException e) {
            PeachLib.getPlugin().getLogger().severe("Failed to initialize QueueLogger writer: " + e.getMessage());
        }
    }

    /**
     * Atomically appends a task to the temporary WAL log file.
     * Uses Base64 for values to safely avoid delimiter issues with JSON strings.
     *
     * @param task The queue task to log.
     */
    public synchronized void logTask(Task task) {
        if (task.type() == Task.TaskType.READ || task.type() == Task.TaskType.GET_ALL_KEYS) {
            return;
        }

        try {
            // Strict UTF-8 enforcement to prevent malformed data across different operating systems
            String valueSafe = task.jsonValue() != null ? Base64.getEncoder().encodeToString(task.jsonValue().getBytes(StandardCharsets.UTF_8)) : "NULL";
            String line = task.type() + "," + task.connectionId() + "," + task.tableName() + "," + task.id() + "," + valueSafe + "," + task.timestamp() + System.lineSeparator();

            if (writer != null) {
                writer.write(line);
                writer.flush();
            }
        } catch (IOException e) {
            PeachLib.getPlugin().getLogger().severe("Failed to write to queue log: " + e.getMessage());
        }
    }

    /**
     * Recovers data from a dirty crash log and feeds it back into the DataManager.
     * Should be called on Plugin Enable.
     */
    public synchronized void recover(DataManager dataManager) {
        try {
            if (writer != null) writer.close();
        } catch (IOException ignored) {}

        processRecoveryFile(tempLogFile, dataManager);
        processRecoveryFile(logFile, dataManager);

        initWriter();
    }

    private void processRecoveryFile(File file, DataManager dataManager) {
        if (!file.exists() || file.length() == 0) return;

        try {
            // Read with UTF-8
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            int recovered = 0;

            for (String line : lines) {
                try {
                    String[] parts = line.split(",");
                    if (parts.length == 6) {
                        Task.TaskType type = Task.TaskType.valueOf(parts[0]);
                        String connId = parts[1];
                        String table = parts[2];
                        String id = parts[3];

                        // Enforce UTF-8 during decode
                        String json = "NULL".equals(parts[4]) ? null : new String(Base64.getDecoder().decode(parts[4]), StandardCharsets.UTF_8);
                        long timestamp = Long.parseLong(parts[5]);

                        // Future is null here, which is intended, but Handled safely by updated DataWorker
                        Task task = new Task(type, connId, table, id, json, timestamp, null);
                        dataManager.enqueueTask(task);
                        recovered++;
                    }
                } catch (Exception ex) {
                    PeachLib.getPlugin().getLogger().warning("Überspringe fehlerhafte Zeile im Crash-Log (" + file.getName() + "): " + line);
                }
            }

            if (recovered > 0) {
                PeachLib.getPlugin().getLogger().info("Successfully recovered " + recovered + " DB operations from crash log: " + file.getName());
            }

            // Clear file after successful recovery
            file.delete();
            file.createNewFile();

        } catch (Exception e) {
            PeachLib.getPlugin().getLogger().severe("Failed to read crash log from " + file.getName() + "!");
        }
    }

    /**
     * Clears the log safely once the queue is fully processed and empty.
     */
    public synchronized void clearLogAtomically() {
        if (!tempLogFile.exists() || tempLogFile.length() == 0) return;
        try {
            if (writer != null) {
                writer.close();
            }

            Files.move(tempLogFile.toPath(), logFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(tempLogFile.toPath());

            initWriter();
        } catch (IOException e) {
            PeachLib.getPlugin().getLogger().severe("Failed to clear queue log atomically: " + e.getMessage());
        }
    }
}