package org.example.minirx.scheduler;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduler that executes all tasks on a single dedicated thread.
 *
 * <p>This scheduler is useful when tasks must be executed sequentially
 * and in a predictable order.
 *
 * <p>Threads are created as daemon threads so they do not prevent the JVM
 * from shutting down in demo and test scenarios.
 */
public class SingleThreadScheduler implements Scheduler {

    /**
     * Executor used to run scheduled tasks.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task);
        thread.setName("mini-rx-single-1");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Schedules the given task on the single worker thread.
     *
     * @param task the task to execute
     */
    @Override
    public void execute(Runnable task) {
        executor.execute(Objects.requireNonNull(task, "task must not be null"));
    }
}