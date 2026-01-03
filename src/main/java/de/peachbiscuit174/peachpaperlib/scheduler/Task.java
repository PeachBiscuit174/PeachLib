package de.peachbiscuit174.peachpaperlib.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a thread-safe handle for a task scheduled via the LibraryScheduler.
 * Provides control over the task's lifecycle, including cancellation and
 * status monitoring.
 * * <p>Example usage:</p>
 * <pre>{@code
 * Task myTask = scheduler.runSyncRepeating(() -> {
 * // Repetitive logic
 * }, 0, 1, TimeUnit.SECONDS);
 * * myTask.onCancel(() -> System.out.println("Cleaned up!"));
 * }</pre>
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class Task {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final boolean repeating;
    private Runnable cancelHandler;

    /**
     * Internal constructor to initialize a new task handle.
     *
     * @param repeating Whether the task is configured to repeat at an interval.
     */
    public Task(boolean repeating) {
        this.repeating = repeating;
    }

    /**
     * Cancels the task effectively immediately.
     * If the task is currently running, it will complete its current execution,
     * but no further iterations will be scheduled. If an onCancel handler
     * was provided, it will be executed now.
     * * <p>Example:</p>
     * <pre>{@code
     * task.cancel();
     * }</pre>
     */
    public void cancel() {
        if (cancelled.getAndSet(true)) {
            return; // Already cancelled
        }

        if (cancelHandler != null) {
            cancelHandler.run();
        }
    }

    /**
     * Checks if the task has been marked for cancellation.
     *
     * @return true if the task was cancelled, false otherwise.
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Indicates whether the task is a recurring task.
     *
     * @return true if the task repeats, false if it is a single-shot execution.
     */
    public boolean isRepeating() {
        return repeating;
    }

    /**
     * Executes the given {@link Runnable} only if the task is still active.
     * This is useful for long-running tasks that check their status between steps.
     * * <p>Example:</p>
     * <pre>{@code
     * scheduler.runAsync(() -> {
     * firstStep();
     * task.ifActive(() -> secondStep());
     * });
     * }</pre>
     *
     * @param runnable The code block to execute if the task is still active.
     */
    public void ifActive(Runnable runnable) {
        if (!isCancelled()) {
            runnable.run();
        }
    }

    /**
     * Registers a callback that will be executed when the task is cancelled.
     * Used for resource cleanup like closing database connections or
     * stopping particle effects.
     * * <p>Example:</p>
     * <pre>{@code
     * task.onCancel(() -> {
     * player.sendMessage("Process stopped.");
     * });
     * }</pre>
     *
     * @param handler The code to run upon cancellation.
     * @return The current Task instance for fluent API usage.
     */
    public Task onCancel(Runnable handler) {
        this.cancelHandler = handler;
        return this;
    }
}