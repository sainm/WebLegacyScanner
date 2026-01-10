package io.github.sainm.weblegacyscan.core.executor;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Virtual thread executor interface
 */
public sealed interface VirtualThreadExecutor permits DefaultVirtualThreadExecutor {
    
    /**
     * Execute all tasks in parallel
     * @param tasks list of tasks
     * @return list of results
     */
    <T> List<T> executeAll(List<Callable<T>> tasks);

    /**
     * Shutdown the executor
     */
    void shutdown();

    /**
     * Add a progress listener
     */
    void addProgressListener(ProgressListener listener);

    /**
     * Remove a progress listener
     */
    void removeProgressListener(ProgressListener listener);

    /**
     * Cancel execution
     */
    void cancel();

    /**
     * Check if cancelled
     */
    boolean isCancelled();
}
