package de.peachbiscuit174.peachlib.scheduler;

import de.peachbiscuit174.peachlib.PeachLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A high-performance, resource-efficient scheduler for Minecraft libraries.
 * <p>
 * This scheduler manages independent thread pools to offload work from the main server thread
 * and provides a centralized synchronization mechanism.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 * <li><b>Lag Protection:</b> Synchronous tasks are processed with a strict time budget (25ms per tick)
 * to prevent the library from freezing the server, even under heavy load.</li>
 * <li><b>Real-Time Scheduling:</b> Delayed and repeating tasks use Java's {@link ScheduledExecutorService},
 * meaning they run based on wall-clock time (milliseconds), not Server-Ticks.</li>
 * </ul>
 *
 * <p><b>Note:</b> Do not instantiate this class manually. Use the provided
 * API access point to ensure resource sharing:
 * {@code API.getSchedulerManager().getScheduler()}</p>
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class LibraryScheduler {

    private static boolean instantiated = false;
    private final Plugin libraryOwner;
    private final ThreadPoolExecutor asyncExecutor;
    private final ScheduledExecutorService timerService;
    private final Queue<Runnable> syncQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final BukkitTask syncTask;

    /**
     * Maximum time (in nanoseconds) the sync task is allowed to run per tick.
     * <p>1 Tick = 50ms. We utilize 25ms (50%) to leave sufficient room for
     * server mechanics and other plugins.</p>
     */
    private static final long MAX_TICK_BUDGET_NANOS = 25_000_000L; // 25ms

    /**
     * Maximum time (in nanoseconds) the scheduler is allowed to block the main thread
     * during shutdown to process remaining tasks.
     */
    private static final long MAX_SHUTDOWN_SYNC_TIME_NANOS = 10_000_000_000L; // 10 seconds

    /**
     * Internal constructor for the scheduler.
     * <p><b>Warning:</b> Manual instantiation is discouraged to prevent
     * thread pool duplication. Access this via the API instead.</p>
     *
     * @param libraryOwner The plugin instance owning this scheduler.
     * @throws IllegalStateException If the scheduler is initialized more than once or by a foreign plugin.
     */
    @ApiStatus.Internal
    public LibraryScheduler(@NotNull Plugin libraryOwner) {
        if (instantiated) {
            throw new IllegalStateException("LibraryScheduler has already been instantiated! " +
                    "Use API.getSchedulerManager().getScheduler() instead of creating a new one.");
        }
        if (libraryOwner != PeachLib.getPlugin()) {
            throw new IllegalStateException("Only the PeachLib Plugin can initialize this!");
        }
        this.libraryOwner = libraryOwner;
        instantiated = true;

        // Named Thread Factory for better debugging and profiling
        ThreadFactory asyncFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r, "PPL-Async-" + counter.getAndIncrement());
            }
        };

        // Optimized Pool: core threads for stability, max threads for peaks, 60s idle timeout
        this.asyncExecutor = new ThreadPoolExecutor(
                2, 8, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                asyncFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        this.timerService = Executors.newSingleThreadScheduledExecutor(r ->
                new Thread(r, "PPL-Timer"));

        // Central task to process the sync queue every tick (approx. every 50ms).
        // We pass the standard tick budget (25ms).
        this.syncTask = Bukkit.getScheduler().runTaskTimer(libraryOwner, () ->
                processSyncQueue(MAX_TICK_BUDGET_NANOS), 1L, 1L);
    }

    // --- EXECUTION METHODS ---

    /**
     * Schedules a task to be executed on the Bukkit main thread.
     * <p>
     * The task is added to a queue and executed roughly in the next tick.
     * Execution respects a defined time budget to prevent lag spikes. If the budget
     * is exceeded, remaining tasks are deferred to the next tick.
     * </p>
     *
     * @param runnable The task to execute.
     */
    public void runSync(@NotNull Runnable runnable) {
        if (!isShutdown.get()) {
            syncQueue.add(runnable);
        }
    }

    /**
     * Executes a task asynchronously using the internal thread pool.
     * <p>
     * This is suitable for heavy calculations, database operations, or network I/O.
     * Do not access the Bukkit API from this task unless the API method is thread-safe.
     * </p>
     *
     * @param runnable The task to execute.
     */
    public void runAsync(@NotNull Runnable runnable) {
        if (!isShutdown.get()) {
            asyncExecutor.execute(runnable);
        }
    }

    // --- DELAYED & REPEATING (REAL TIME) ---

    /**
     * Schedules a task for synchronous execution after a specific <b>REAL-TIME</b> delay.
     * <p>
     * Note: The delay is based on system time, not server ticks. It will execute
     * even if the server TPS is low.
     * </p>
     *
     * @param runnable The task to execute on the main thread.
     * @param delay    The time to delay.
     * @param unit     The unit of the delay parameter.
     * @return A {@link ScheduledFuture} representing pending completion of the task, or null if shutdown.
     */
    public ScheduledFuture<?> runSyncDelayed(Runnable runnable, long delay, TimeUnit unit) {
        if (isShutdown.get()) return null;
        return timerService.schedule(() -> runSync(runnable), delay, unit);
    }

    /**
     * Schedules a task for asynchronous execution after a specific <b>REAL-TIME</b> delay.
     *
     * @param runnable The task to execute asynchronously.
     * @param delay    The time to delay.
     * @param unit     The unit of the delay parameter.
     * @return A {@link ScheduledFuture} representing pending completion of the task, or null if shutdown.
     */
    public ScheduledFuture<?> runAsyncDelayed(Runnable runnable, long delay, TimeUnit unit) {
        if (isShutdown.get()) return null;
        return timerService.schedule(() -> runAsync(runnable), delay, unit);
    }

    /**
     * Schedules a task for synchronous execution that repeats at a fixed <b>REAL-TIME</b> rate.
     *
     * @param runnable The task to execute on the main thread.
     * @param delay    The time to delay first execution.
     * @param period   The period between successive executions.
     * @param unit     The unit of the delay and period parameters.
     * @return A {@link ScheduledFuture} representing pending completion of the series of repeated tasks.
     */
    public ScheduledFuture<?> runSyncRepeating(Runnable runnable, long delay, long period, TimeUnit unit) {
        if (isShutdown.get()) return null;
        return timerService.scheduleAtFixedRate(() -> runSync(runnable), delay, period, unit);
    }

    /**
     * Schedules a task for asynchronous execution that repeats at a fixed <b>REAL-TIME</b> rate.
     *
     * @param runnable The task to execute asynchronously.
     * @param delay    The time to delay first execution.
     * @param period   The period between successive executions.
     * @param unit     The unit of the delay and period parameters.
     * @return A {@link ScheduledFuture} representing pending completion of the series of repeated tasks.
     */
    public ScheduledFuture<?> runAsyncRepeating(Runnable runnable, long delay, long period, TimeUnit unit) {
        if (isShutdown.get()) return null;
        return timerService.scheduleAtFixedRate(() -> runAsync(runnable), delay, period, unit);
    }

    // --- UTILITIES ---

    /**
     * Safely executes a task on the main thread only if the specified player is online.
     * <p>
     * This utility helps prevent memory leaks or errors when handling player
     * objects in delayed or asynchronous contexts.
     * </p>
     *
     * @param uuid The UUID of the player.
     * @param task The logic to run with the player instance.
     */
    public void runSafe(@NotNull UUID uuid, @NotNull Consumer<Player> task) {
        runSync(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                task.accept(player);
            }
        });
    }

    /**
     * Processes pending tasks in the sync queue.
     * <p>
     * This method runs until the queue is empty OR the specified time budget is exceeded.
     * </p>
     *
     * @param maxBudgetNanos The maximum time (in nanoseconds) this method is allowed to block.
     */
    private void processSyncQueue(long maxBudgetNanos) {
        if (syncQueue.isEmpty()) return;

        long startTime = System.nanoTime();
        Runnable task;

        // Process tasks until queue is empty or budget is exceeded
        while ((task = syncQueue.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                libraryOwner.getLogger().severe("Error in PPL Sync Task: " + e.getMessage());
                e.printStackTrace();
            }

            // Check if we have exceeded the time budget
            if (System.nanoTime() - startTime > maxBudgetNanos) {
                // Log warning only if we are significantly overloaded (> 1000 tasks pending)
                // or if we are in shutdown mode (budget > 1s implies shutdown or heavy op)
                if (syncQueue.size() > 1000 || maxBudgetNanos > 1_000_000_000L) {
                    libraryOwner.getLogger().warning("PPL Sync Queue budget exceeded! Stopped processing. Pending: " + syncQueue.size());
                }
                break;
            }
        }
    }

    /**
     * Gracefully shuts down the internal executors and processes the remaining sync queue.
     * <p>
     * <b>Process:</b>
     * <ol>
     * <li>Cancels the recurring Bukkit task.</li>
     * <li>Shuts down the timer and async thread pools.</li>
     * <li>Waits for the timer service to finish (max 5s).</li>
     * <li>Waits for running async tasks to finish (max 10s).</li>
     * <li>Executes remaining synchronous tasks with a <b>10-second timeout</b>.</li>
     * </ol>
     * This method must be called in the PeachLib plugin's {@code onDisable()}.
     * </p>
     */
    public void shutdown() {
        if (isShutdown.getAndSet(true)) return;

        // Cancel the Bukkit task processing the queue
        if (syncTask != null && !syncTask.isCancelled()) {
            syncTask.cancel();
        }

        // Initiate shutdown for both executors
        timerService.shutdown();
        asyncExecutor.shutdown();

        try {
            // Wait for TIMER service to finish (usually fast, giving it 5 seconds)
            if (!timerService.awaitTermination(5, TimeUnit.SECONDS)) {
                timerService.shutdownNow();
            }

            // Wait for ASYNC tasks to finish (up to 10 seconds)
            if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // Force shutdown on both if interrupted
            timerService.shutdownNow();
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Process remaining sync tasks immediately.
        // We allow a maximum of 10 seconds for this cleanup to avoid hanging the server shutdown.
        processSyncQueue(MAX_SHUTDOWN_SYNC_TIME_NANOS);

        instantiated = false;
    }
}