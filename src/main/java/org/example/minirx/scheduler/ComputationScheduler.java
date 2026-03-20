package org.example.minirx.scheduler;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler for CPU-intensive work.
 *
 * <p>Uses a fixed thread pool sized by the number of available processors.
 */
public class ComputationScheduler implements Scheduler {

    /**
     * Counter for generated thread names.
     */
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);

    /**
     * Number of worker threads for computation tasks.
     */
    private static final int THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors());

    /**
     * Executor used for scheduled tasks.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT, task -> {
        Thread thread = new Thread(task);
        thread.setName("mini-rx-computation-" + THREAD_COUNTER.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Schedules a task on the computation pool.
     *
     * @param task task to execute
     */
    @Override
    public void execute(Runnable task) {
        executor.execute(Objects.requireNonNull(task, "task must not be null"));
    }
}
