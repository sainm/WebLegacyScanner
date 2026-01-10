package io.github.sainm.weblegacyscan.core.scanner;

import io.github.sainm.weblegacyscan.core.executor.ProgressListener;

/**
 * 扫描器接�?
 */
public sealed interface Scanner permits DefaultScanner {
    
    /**
     * 执行扫描
     * @param config 扫描配置
     * @return 扫描结果
     */
    ScanResult scan(ScanConfig config);

    /**
     * 添加进度监听�?
     */
    void addProgressListener(ProgressListener listener);

    /**
     * 移除进度监听�?
     */
    void removeProgressListener(ProgressListener listener);

    /**
     * 取消扫描
     */
    void cancel();

    /**
     * 是否已取�?
     */
    boolean isCancelled();
}
