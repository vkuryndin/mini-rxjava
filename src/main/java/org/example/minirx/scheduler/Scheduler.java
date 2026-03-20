package org.example.minirx.scheduler;

/**
 * Executes tasks according to a chosen scheduling strategy.
 *
 * <p>Different implementations may use one thread, a fixed pool,
 * or a cached pool depending on their purpose.
 */
public interface Scheduler {

    /**
     * Submits a task for execution.
     *
     * @param task task to run
     */
    void execute(Runnable task);
}
