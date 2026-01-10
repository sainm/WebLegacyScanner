package io.github.sainm.weblegacyscan.core.executor;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 虚拟线程执行器接�?
 */
public sealed interface VirtualThreadExecutor permits DefaultVirtualThreadExecutor {
    
    /**
     * 并行执行所有任�?
     * @param tasks 任务列表
     * @return 结果列表
     */
    <T> List<T> executeAll(List<Callable<T>> tasks);

    /**
     * 关闭执行�?
     */
    void shutdown();

    /**
     * 添加进度监听�?
     */
    void addProgressListener(ProgressListener listener);

    /**
     * 移除进度监听�?
     */
    void removeProgressListener(ProgressListener listener);

    /**
     * 取消执行
     */
    void cancel();

    /**
     * 是否已取�?
     */
    boolean isCancelled();
}
