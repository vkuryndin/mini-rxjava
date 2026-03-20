package org.example.minirx.scheduler;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduler that runs all tasks on one dedicated thread.
 *
 * <p>Useful when task order matters and work should be processed
 * sequentially.
 */
public class SingleThreadScheduler implements Scheduler {

    /**
     * Executor used for scheduled tasks.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task);
        thread.setName("mini-rx-single-1");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Schedules a task on the single worker thread.
     *
     * @param task task to execute
     */
    @Override
    public void execute(Runnable task) {
        executor.execute(Objects.requireNonNull(task, "task must not be null"));
    }
}
