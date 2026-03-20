package org.example.minirx.scheduler;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler intended for I/O-bound work.
 *
 * <p>Uses a cached thread pool, which fits tasks that may block or spend
 * time waiting on external resources.
 */
public class IOThreadScheduler implements Scheduler {

    /**
     * Counter for generated thread names.
     */
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);

    /**
     * Executor used for scheduled tasks.
     */
    private final ExecutorService executor = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task);
        thread.setName("mini-rx-io-" + THREAD_COUNTER.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Schedules a task on the cached pool.
     *
     * @param task task to execute
     */
    @Override
    public void execute(Runnable task) {
        executor.execute(Objects.requireNonNull(task, "task must not be null"));
    }
}
