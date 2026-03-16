package org.example.minirx.scheduler;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler for IO-related work.
 *
 * <p>This scheduler uses a cached thread pool, which is suitable for tasks that:
 * <ul>
 *     <li>may block,</li>
 *     <li>wait for external resources,</li>
 *     <li>perform file, network, or database operations.</li>
 * </ul>
 *
 * <p>Threads are created as daemon threads so they do not prevent the JVM
 * from shutting down in demo and test scenarios.
 */
public class IOThreadScheduler implements Scheduler {

    /**
     * Counter used to generate readable thread names.
     */
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);

    /**
     * Executor used to run scheduled tasks.
     */
    private final ExecutorService executor = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task);
        thread.setName("mini-rx-io-" + THREAD_COUNTER.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Schedules the given task on the cached thread pool.
     *
     * @param task the task to execute
     */
    @Override
    public void execute(Runnable task) {
        executor.execute(Objects.requireNonNull(task, "task must not be null"));
    }
}