package io.github.sainm.weblegacyscan.core.scanner;

import io.github.sainm.weblegacyscan.core.executor.ProgressListener;

/**
 * Scanner interface
 */
public sealed interface Scanner permits DefaultScanner {
    
    /**
     * Execute scan
     * @param config scan configuration
     * @return scan result
     */
    ScanResult scan(ScanConfig config);

    /**
     * Add a progress listener
     */
    void addProgressListener(ProgressListener listener);

    /**
     * Remove a progress listener
     */
    void removeProgressListener(ProgressListener listener);

    /**
     * Cancel scan
     */
    void cancel();

    /**
     * Check if cancelled
     */
    boolean isCancelled();
}
