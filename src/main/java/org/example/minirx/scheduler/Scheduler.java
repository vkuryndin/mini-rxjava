package org.example.minirx.scheduler;

/**
 * Schedules tasks for execution.
 *
 * <p>A scheduler is responsible for deciding where and how a task should run.
 * Different scheduler implementations can use different threading strategies.
 *
 * <p>Examples:
 * <ul>
 *     <li>an IO scheduler can use a cached thread pool,</li>
 *     <li>a computation scheduler can use a fixed thread pool,</li>
 *     <li>a single-thread scheduler can use one dedicated thread.</li>
 * </ul>
 */
public interface Scheduler {

    /**
     * Schedules the given task for execution.
     *
     * @param task the task to execute
     */
    void execute(Runnable task);
}