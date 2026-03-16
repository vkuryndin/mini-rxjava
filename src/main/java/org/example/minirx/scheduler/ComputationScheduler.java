package org.example.minirx.scheduler;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler for CPU-bound computation tasks.
 *
 * <p>This scheduler uses a fixed thread pool sized according to the number
 * of available processors. It is suitable for tasks that perform calculations
 * and do not spend much time waiting for external resources.
 *
 * <p>Threads are created as daemon threads so they do not prevent the JVM
 * from shutting down in demo and test scenarios.
 */
public class ComputationScheduler implements Scheduler {

    /**
     * Counter used to generate readable thread names.
     */
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);

    /**
     * Number of worker threads used for computation tasks.
     */
    private static final int THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors());

    /**
     * Executor used to run scheduled tasks.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT, task -> {
        Thread thread = new Thread(task);
        thread.setName("mini-rx-computation-" + THREAD_COUNTER.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Schedules the given task on the computation thread pool.
     *
     * @param task the task to execute
     */
    @Override
    public void execute(Runnable task) {
        executor.execute(Objects.requireNonNull(task, "task must not be null"));
    }
}