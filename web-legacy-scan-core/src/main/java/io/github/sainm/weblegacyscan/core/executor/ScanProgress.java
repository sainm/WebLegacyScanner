package io.github.sainm.weblegacyscan.core.executor;

import java.time.Duration;

/**
 * 扫描进度
 */
public record ScanProgress(
    int totalTasks,
    int completedTasks,
    int issuesFound,
    Duration elapsed
) {
    public double getPercentage() {
        if (totalTasks == 0) return 100.0;
        return (completedTasks * 100.0) / totalTasks;
    }

    public boolean isComplete() {
        return completedTasks >= totalTasks;
    }

    public static ScanProgress initial(int totalTasks) {
        return new ScanProgress(totalTasks, 0, 0, Duration.ZERO);
    }
}
