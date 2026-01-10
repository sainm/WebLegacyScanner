package io.github.sainm.weblegacyscan.core.executor;

import java.nio.file.Path;

/**
 * 进度监听器接�?
 */
public interface ProgressListener {
    
    /**
     * 进度更新
     */
    void onProgress(ScanProgress progress);

    /**
     * 单个文件扫描完成
     */
    void onFileComplete(Path file, boolean success);

    /**
     * 扫描完成
     */
    void onScanComplete(ScanProgress finalProgress);
}
