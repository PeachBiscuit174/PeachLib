package de.peachbiscuit174.peachlib.data.queue;

import de.peachbiscuit174.peachlib.api.managers.DataManager;
import de.peachbiscuit174.peachlib.data.StorageAdapter;
import de.peachbiscuit174.peachlib.data.Task;
import de.peachbiscuit174.peachlib.data.logs.AuditLogger;
import de.peachbiscuit174.peachlib.data.logs.QueueLogger;
import org.jetbrains.annotations.ApiStatus;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker responsible for processing database tasks asynchronously.
 * Guarantees execution order and utilizes batch processing to prevent CPU spikes.
 */
@ApiStatus.Internal
public class DataWorker implements Runnable {

    private final DataManager dataManager;
    private final Queue<Task> taskQueue;
    private final QueueLogger queueLogger;
    private final AuditLogger auditLogger;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private static final int BATCH_SIZE = 100;

    public DataWorker(DataManager dataManager, Queue<Task> taskQueue, QueueLogger queueLogger, AuditLogger auditLogger) {
        this.dataManager = dataManager;
        this.taskQueue = taskQueue;
        this.queueLogger = queueLogger;
        this.auditLogger = auditLogger;
    }

    @Override
    public void run() {
        if (!isProcessing.compareAndSet(false, true)) {
            return;
        }

        try {
            int processed = 0;
            while (processed < BATCH_SIZE && !taskQueue.isEmpty()) {
                Task task = taskQueue.poll();
                if (task == null) break;

                processSingleTask(task);
                processed++;
            }

            if (taskQueue.isEmpty()) {
                queueLogger.clearLogAtomically();
            }

        } finally {
            isProcessing.set(false);
        }
    }

    /**
     * Drains the entire queue instantly. Called synchronously during server shutdown
     * to prevent data loss.
     */
    public void flushAll() {
        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();
            if (task != null) {
                processSingleTask(task);
            }
        }
        queueLogger.clearLogAtomically();
    }

    private void processSingleTask(Task task) {
        try {
            StorageAdapter adapter = dataManager.getAdapter(task.connectionId());
            if (adapter == null) {
                throw new IllegalStateException("Adapter not found for connection: " + task.connectionId());
            }

            switch (task.type()) {
                case WRITE -> {
                    adapter.write(task.tableName(), task.id(), task.jsonValue(), task.timestamp());
                    auditLogger.logOperation(task.connectionId(), task.tableName(), "WRITE", task.id(), task.jsonValue());
                    if (task.future() != null) task.future().complete(null);
                }
                case DELETE -> {
                    adapter.delete(task.tableName(), task.id());
                    auditLogger.logOperation(task.connectionId(), task.tableName(), "DELETE", task.id(), null);
                    if (task.future() != null) task.future().complete(null);
                }
                case READ -> {
                    String json = adapter.read(task.tableName(), task.id());
                    if (task.future() != null) task.future().complete(json);
                }
                case GET_ALL_KEYS -> {
                    Set<String> keys = adapter.getAllPrimaryKeys(task.tableName());
                    if (task.future() != null) task.future().complete(keys);
                }
            }
        } catch (Exception e) {
            if (task.future() != null) {
                task.future().completeExceptionally(e);
            } else {
                e.printStackTrace();
            }
        }
    }
}