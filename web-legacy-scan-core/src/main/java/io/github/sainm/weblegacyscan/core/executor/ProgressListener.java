package io.github.sainm.weblegacyscan.core.executor;

import java.nio.file.Path;

/**
 * Progress listener interface
 */
public interface ProgressListener {
    
    /**
     * Progress update callback
     */
    void onProgress(ScanProgress progress);

    /**
     * Single file scan complete callback
     */
    void onFileComplete(Path file, boolean success);

    /**
     * Scan complete callback
     */
    void onScanComplete(ScanProgress finalProgress);
}
