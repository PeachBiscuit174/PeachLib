package de.peachbiscuit174.peachpaperlib.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A high-performance, resource-efficient scheduler for Minecraft libraries.
 * It manages thread pools independently of the Bukkit Lifecycle and
 * provides a centralized synchronization system.
 * * <p>Key features:</p>
 * <ul>
 * <li>Dynamic thread pool (0-8 threads) with 60s idle timeout.</li>
 * <li>Centralized sync task to reduce main thread overhead.</li>
 * <li>Safe player interaction wrappers.</li>
 * </ul>
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class LibraryScheduler {

    private final Plugin libraryOwner;
    private final ThreadPoolExecutor asyncExecutor;
    private final ScheduledExecutorService timerService;
    private final Queue<Runnable> syncQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    /**
     * Initializes the scheduler for a library plugin.
     *
     * @param libraryOwner The plugin providing this scheduler instance.
     */
    public LibraryScheduler(Plugin libraryOwner) {
        this.libraryOwner = libraryOwner;

        // Efficient async pool: Only creates threads when needed (up to 8)
        this.asyncExecutor = new ThreadPoolExecutor(
                0, 8, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // Low-overhead scheduler for timing logic
        this.timerService = Executors.newSingleThreadScheduledExecutor();

        // Central Bukkit task that processes all sync requests at once
        Bukkit.getScheduler().runTaskTimer(libraryOwner, this::processSyncQueue, 1L, 1L);
    }

    // --- BASIC EXECUTION ---

    /**
     * Runs a task on the main server thread in the next tick.
     * @param runnable The code to execute.
     */
    public void runSync(Runnable runnable) {
        if (!isShutdown.get()) syncQueue.add(runnable);
    }

    /**
     * Runs a task asynchronously using the library pool.
     * @param runnable The code to execute.
     */
    public void runAsync(Runnable runnable) {
        if (!isShutdown.get()) asyncExecutor.execute(runnable);
    }

    // --- DELAYED EXECUTION ---

    /**
     * Runs a task on the main thread after a specific delay.
     * <pre>{@code
     * scheduler.runSyncDelayed(() -> player.kickPlayer("Time's up!"), 5, TimeUnit.MINUTES);
     * }</pre>
     */
    public Task runSyncDelayed(Runnable runnable, long delay, TimeUnit unit) {
        return scheduleInternal(new Task(false), runnable, delay, 0, unit, true);
    }

    /**
     * Runs a task asynchronously after a specific delay.
     */
    public Task runAsyncDelayed(Runnable runnable, long delay, TimeUnit unit) {
        return scheduleInternal(new Task(false), runnable, delay, 0, unit, false);
    }

    // --- REPEATING EXECUTION ---

    /**
     * Runs a repeating task on the main thread.
     * <pre>{@code
     * scheduler.runSyncRepeating(() -> player.sendMessage("Reminder!"), 0, 30, TimeUnit.SECONDS);
     * }</pre>
     */
    public Task runSyncRepeating(Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        return scheduleInternal(new Task(true), runnable, initialDelay, period, unit, true);
    }

    /**
     * Runs a repeating task asynchronously.
     */
    public Task runAsyncRepeating(Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        return scheduleInternal(new Task(true), runnable, initialDelay, period, unit, false);
    }

    // --- SPECIALIZED WRAPPERS ---

    /**
     * Executes a task only if the player is online. Always runs on the main thread.
     * <pre>{@code
     * scheduler.runSafe(uuid, player -> player.setLevel(10));
     * }</pre>
     * * @param uuid The player's UUID.
     * @param task Logic to execute with the online player.
     */
    public void runSafe(UUID uuid, Consumer<Player> task) {
        runSync(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                task.accept(player);
            }
        });
    }

    // --- INTERNAL MANAGEMENT ---

    private Task scheduleInternal(Task task, Runnable runnable, long delay, long period, TimeUnit unit, boolean sync) {
        if (isShutdown.get() || task.isCancelled()) return task;

        timerService.schedule(() -> {
            if (task.isCancelled()) return;

            if (sync) runSync(runnable);
            else runAsync(runnable);

            if (task.isRepeating()) {
                scheduleInternal(task, runnable, period, period, unit, sync);
            }
        }, delay, unit);

        return task;
    }

    private void processSyncQueue() {
        while (!syncQueue.isEmpty()) {
            Runnable task = syncQueue.poll();
            if (task != null) {
                try {
                    task.run();
                } catch (Exception e) {
                    libraryOwner.getLogger().severe("Exception in Library Task: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Shuts down all executors and flushes the sync queue.
     * Must be called in onDisable().
     */
    public void shutdown() {
        if (isShutdown.getAndSet(true)) return;
        asyncExecutor.shutdownNow();
        timerService.shutdownNow();
        processSyncQueue();
    }
}
