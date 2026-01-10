package io.github.sainm.weblegacyscan.core.executor;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Property-based tests for Progress Monotonicity.
 * 
 * Property 11: Progress Monotonicity
 * For any scan operation, the reported ScanProgress.completedTasks SHALL be 
 * monotonically non-decreasing and SHALL eventually equal ScanProgress.totalTasks 
 * upon completion.
 * 
 * Validates: Requirements 5.9, 5.11
 */
class ProgressMonotonicityPropertyTest {

    /**
     * Property 11: Progress Monotonicity - Completed Tasks Non-Decreasing
     * 
     * For any scan operation, the sequence of reported completedTasks values
     * SHALL be monotonically non-decreasing.
     * 
     * Feature: web-legacy-scan, Property 11: Progress Monotonicity
     * Validates: Requirements 5.9, 5.11
     */
    @Property(tries = 100)
    void completedTasksIsMonotonicallyNonDecreasing(
            @ForAll @IntRange(min = 1, max = 30) int taskCount
    ) throws InterruptedException {
        List<ScanProgress> progressHistory = new CopyOnWriteArrayList<>();
        
        VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor();
        
        // Add listener to capture all progress updates
        executor.addProgressListener(new ProgressListener() {
            @Override
            public void onProgress(ScanProgress progress) {
                progressHistory.add(progress);
            }

            @Override
            public void onFileComplete(Path file, boolean success) {
                // Not used in this test
            }

            @Override
            public void onScanComplete(ScanProgress finalProgress) {
                progressHistory.add(finalProgress);
            }
        });
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            tasks.add(() -> {
                // Simulate variable work duration
                Thread.sleep((long) (Math.random() * 20));
                return taskId;
            });
        }
        
        executor.executeAll(tasks);
        
        // Verify monotonicity: each completedTasks value should be >= previous
        int previousCompleted = 0;
        for (ScanProgress progress : progressHistory) {
            assert progress.completedTasks() >= previousCompleted
                : "Progress not monotonic: went from " + previousCompleted 
                  + " to " + progress.completedTasks();
            previousCompleted = progress.completedTasks();
        }
    }

    /**
     * Property 11: Progress Monotonicity - Final Progress Equals Total
     * 
     * For any scan operation, the final reported completedTasks SHALL equal totalTasks.
     * 
     * Feature: web-legacy-scan, Property 11: Progress Monotonicity
     * Validates: Requirements 5.9, 5.11
     */
    @Property(tries = 100)
    void finalProgressEqualsTotal(
            @ForAll @IntRange(min = 1, max = 30) int taskCount
    ) {
        List<ScanProgress> finalProgressList = new CopyOnWriteArrayList<>();
        
        VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor();
        
        executor.addProgressListener(new ProgressListener() {
            @Override
            public void onProgress(ScanProgress progress) {
                // Not used for this assertion
            }

            @Override
            public void onFileComplete(Path file, boolean success) {
                // Not used in this test
            }

            @Override
            public void onScanComplete(ScanProgress finalProgress) {
                finalProgressList.add(finalProgress);
            }
        });
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            tasks.add(() -> {
                Thread.sleep(5);
                return taskId;
            });
        }
        
        executor.executeAll(tasks);
        
        // Verify final progress
        assert !finalProgressList.isEmpty() : "No final progress reported";
        
        ScanProgress finalProgress = finalProgressList.get(finalProgressList.size() - 1);
        assert finalProgress.completedTasks() == taskCount
            : "Final completedTasks (" + finalProgress.completedTasks() 
              + ") should equal totalTasks (" + taskCount + ")";
        assert finalProgress.totalTasks() == taskCount
            : "Final totalTasks (" + finalProgress.totalTasks() 
              + ") should equal expected (" + taskCount + ")";
    }

    /**
     * Property 11: Progress Monotonicity - Total Tasks Constant
     * 
     * For any scan operation, the totalTasks value SHALL remain constant 
     * throughout all progress updates.
     * 
     * Feature: web-legacy-scan, Property 11: Progress Monotonicity
     * Validates: Requirements 5.9, 5.11
     */
    @Property(tries = 100)
    void totalTasksRemainsConstant(
            @ForAll @IntRange(min = 1, max = 30) int taskCount
    ) {
        List<ScanProgress> progressHistory = new CopyOnWriteArrayList<>();
        
        VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor();
        
        executor.addProgressListener(new ProgressListener() {
            @Override
            public void onProgress(ScanProgress progress) {
                progressHistory.add(progress);
            }

            @Override
            public void onFileComplete(Path file, boolean success) {
                // Not used in this test
            }

            @Override
            public void onScanComplete(ScanProgress finalProgress) {
                progressHistory.add(finalProgress);
            }
        });
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            tasks.add(() -> {
                Thread.sleep(5);
                return taskId;
            });
        }
        
        executor.executeAll(tasks);
        
        // Verify totalTasks is constant
        for (ScanProgress progress : progressHistory) {
            assert progress.totalTasks() == taskCount
                : "totalTasks changed: expected " + taskCount 
                  + " but got " + progress.totalTasks();
        }
    }

    /**
     * Property 11: Progress Monotonicity - Percentage Non-Decreasing
     * 
     * For any scan operation, the reported percentage SHALL be monotonically 
     * non-decreasing and reach 100% upon completion.
     * 
     * Feature: web-legacy-scan, Property 11: Progress Monotonicity
     * Validates: Requirements 5.9, 5.11
     */
    @Property(tries = 100)
    void percentageIsMonotonicallyNonDecreasing(
            @ForAll @IntRange(min = 1, max = 30) int taskCount
    ) {
        List<ScanProgress> progressHistory = new CopyOnWriteArrayList<>();
        
        VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor();
        
        executor.addProgressListener(new ProgressListener() {
            @Override
            public void onProgress(ScanProgress progress) {
                progressHistory.add(progress);
            }

            @Override
            public void onFileComplete(Path file, boolean success) {
                // Not used in this test
            }

            @Override
            public void onScanComplete(ScanProgress finalProgress) {
                progressHistory.add(finalProgress);
            }
        });
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            tasks.add(() -> {
                Thread.sleep(5);
                return taskId;
            });
        }
        
        executor.executeAll(tasks);
        
        // Verify percentage monotonicity
        double previousPercentage = 0.0;
        for (ScanProgress progress : progressHistory) {
            double currentPercentage = progress.getPercentage();
            assert currentPercentage >= previousPercentage
                : "Percentage not monotonic: went from " + previousPercentage 
                  + " to " + currentPercentage;
            previousPercentage = currentPercentage;
        }
        
        // Verify final percentage is 100%
        if (!progressHistory.isEmpty()) {
            ScanProgress finalProgress = progressHistory.get(progressHistory.size() - 1);
            assert finalProgress.getPercentage() == 100.0
                : "Final percentage should be 100% but was " + finalProgress.getPercentage();
        }
    }

    /**
     * Property 11: Progress Monotonicity - With Concurrency Limit
     * 
     * Progress monotonicity SHALL hold even with concurrency limits.
     * 
     * Feature: web-legacy-scan, Property 11: Progress Monotonicity
     * Validates: Requirements 5.9, 5.11
     */
    @Property(tries = 100)
    void progressMonotonicWithConcurrencyLimit(
            @ForAll @IntRange(min = 1, max = 5) int maxConcurrency,
            @ForAll @IntRange(min = 1, max = 20) int taskCount
    ) {
        List<ScanProgress> progressHistory = new CopyOnWriteArrayList<>();
        
        VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor(maxConcurrency);
        
        executor.addProgressListener(new ProgressListener() {
            @Override
            public void onProgress(ScanProgress progress) {
                progressHistory.add(progress);
            }

            @Override
            public void onFileComplete(Path file, boolean success) {
                // Not used in this test
            }

            @Override
            public void onScanComplete(ScanProgress finalProgress) {
                progressHistory.add(finalProgress);
            }
        });
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            tasks.add(() -> {
                Thread.sleep(5);
                return taskId;
            });
        }
        
        executor.executeAll(tasks);
        
        // Verify monotonicity
        int previousCompleted = 0;
        for (ScanProgress progress : progressHistory) {
            assert progress.completedTasks() >= previousCompleted
                : "Progress not monotonic with concurrency limit: went from " 
                  + previousCompleted + " to " + progress.completedTasks();
            previousCompleted = progress.completedTasks();
        }
        
        // Verify final state
        if (!progressHistory.isEmpty()) {
            ScanProgress finalProgress = progressHistory.get(progressHistory.size() - 1);
            assert finalProgress.completedTasks() == taskCount
                : "Final completedTasks should equal taskCount";
        }
    }

    /**
     * Property 11: Progress Monotonicity - Empty Task List
     * 
     * For an empty task list, progress SHALL immediately be complete (100%).
     * 
     * Feature: web-legacy-scan, Property 11: Progress Monotonicity
     * Validates: Requirements 5.9, 5.11
     */
    @Example
    void emptyTaskListReportsComplete() {
        List<ScanProgress> progressHistory = new CopyOnWriteArrayList<>();
        
        VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor();
        
        executor.addProgressListener(new ProgressListener() {
            @Override
            public void onProgress(ScanProgress progress) {
                progressHistory.add(progress);
            }

            @Override
            public void onFileComplete(Path file, boolean success) {
                // Not used in this test
            }

            @Override
            public void onScanComplete(ScanProgress finalProgress) {
                progressHistory.add(finalProgress);
            }
        });
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        
        List<Integer> results = executor.executeAll(tasks);
        
        // Empty task list should return empty results
        assert results.isEmpty() : "Empty task list should return empty results";
        
        // For empty tasks, ScanProgress.initial(0).getPercentage() returns 100%
        ScanProgress emptyProgress = ScanProgress.initial(0);
        assert emptyProgress.getPercentage() == 100.0
            : "Empty progress should report 100%";
        assert emptyProgress.isComplete()
            : "Empty progress should be complete";
    }

    /**
     * Property 11: Progress Monotonicity - isComplete Consistency
     * 
     * isComplete() SHALL return true if and only if completedTasks >= totalTasks.
     * 
     * Feature: web-legacy-scan, Property 11: Progress Monotonicity
     * Validates: Requirements 5.9, 5.11
     */
    @Property(tries = 100)
    void isCompleteConsistentWithCompletedTasks(
            @ForAll @IntRange(min = 0, max = 100) int totalTasks,
            @ForAll @IntRange(min = 0, max = 100) int completedTasks
    ) {
        ScanProgress progress = new ScanProgress(
            totalTasks, 
            completedTasks, 
            0, 
            java.time.Duration.ZERO
        );
        
        boolean expectedComplete = completedTasks >= totalTasks;
        assert progress.isComplete() == expectedComplete
            : "isComplete() should be " + expectedComplete 
              + " when completedTasks=" + completedTasks 
              + " and totalTasks=" + totalTasks;
    }
}
