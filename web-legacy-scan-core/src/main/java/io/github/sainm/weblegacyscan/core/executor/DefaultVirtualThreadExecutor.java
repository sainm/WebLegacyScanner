package io.github.sainm.weblegacyscan.core.executor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认虚拟线程执行器实现
 */
public final class DefaultVirtualThreadExecutor implements VirtualThreadExecutor {

    private final List<ProgressListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final int maxConcurrency;
    private final Semaphore semaphore;

    public DefaultVirtualThreadExecutor() {
        this(Integer.MAX_VALUE);
    }

    public DefaultVirtualThreadExecutor(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency > 0 ? maxConcurrency : Integer.MAX_VALUE;
        this.semaphore = new Semaphore(this.maxConcurrency);
    }

    @Override
    public <T> List<T> executeAll(List<Callable<T>> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }

        cancelled.set(false);
        Instant start = Instant.now();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger issuesFound = new AtomicInteger(0);
        AtomicInteger lastReportedProgress = new AtomicInteger(0);
        Object progressLock = new Object();
        int totalTasks = tasks.size();

        List<T> results = new ArrayList<>(totalTasks);
        List<Future<T>> futures = new ArrayList<>(totalTasks);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // 提交所有任务
            for (Callable<T> task : tasks) {
                if (cancelled.get()) {
                    break;
                }

                Future<T> future = executor.submit(() -> {
                    if (cancelled.get()) {
                        return null;
                    }

                    try {
                        semaphore.acquire();
                        try {
                            return task.call();
                        } finally {
                            semaphore.release();
                            int done = completed.incrementAndGet();
                            // Ensure monotonic progress reporting
                            notifyProgressIfIncreased(
                                lastReportedProgress,
                                new ScanProgress(
                                    totalTasks, done, issuesFound.get(),
                                    Duration.between(start, Instant.now())
                                ),
                                progressLock
                            );
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                });
                futures.add(future);
            }

            // 收集结果
            for (Future<T> future : futures) {
                try {
                    T result = future.get();
                    if (result != null) {
                        results.add(result);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException e) {
                    // 记录错误但继续处理其他任务
                    Throwable cause = e.getCause();
                    if (cause != null) {
                        System.err.println("Task failed: " + cause.getMessage());
                    }
                }
            }
        }

        ScanProgress finalProgress = new ScanProgress(
            totalTasks, completed.get(), issuesFound.get(),
            Duration.between(start, Instant.now())
        );
        notifyScanComplete(finalProgress);

        return results;
    }

    @Override
    public void shutdown() {
        cancelled.set(true);
    }

    @Override
    public void addProgressListener(ProgressListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeProgressListener(ProgressListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    private void notifyProgress(ScanProgress progress) {
        for (ProgressListener listener : listeners) {
            try {
                listener.onProgress(progress);
            } catch (Exception e) {
                // 忽略监听器异常
            }
        }
    }

    /**
     * Notify progress only if the completed tasks count has increased.
     * This ensures monotonic progress reporting even with concurrent task completion.
     * Uses synchronization to ensure notifications are delivered in order.
     */
    private void notifyProgressIfIncreased(AtomicInteger lastReported, ScanProgress progress, Object lock) {
        int currentCompleted = progress.completedTasks();
        
        synchronized (lock) {
            int lastValue = lastReported.get();
            if (currentCompleted <= lastValue) {
                // Another thread already reported equal or higher progress
                return;
            }
            lastReported.set(currentCompleted);
            notifyProgress(progress);
        }
    }

    private void notifyScanComplete(ScanProgress progress) {
        for (ProgressListener listener : listeners) {
            try {
                listener.onScanComplete(progress);
            } catch (Exception e) {
                // 忽略监听器异常
            }
        }
    }
}
