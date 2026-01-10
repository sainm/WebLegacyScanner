package io.github.sainm.weblegacyscan.core.executor;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Property-based tests for Concurrency Limit Enforcement.
 * 
 * Property 20: Concurrency Limit Enforcement
 * For any scan with a configured max-threads limit, the number of concurrent 
 * virtual threads SHALL not exceed the configured limit.
 * 
 * Validates: Requirements 5.4
 */
class ConcurrencyLimitPropertyTest {

    /**
     * Property 20: Concurrency Limit Enforcement
     * 
     * For any configured concurrency limit and any number of tasks, the number of
     * concurrently executing tasks SHALL never exceed the configured limit.
     * 
     * Feature: web-legacy-scan, Property 20: Concurrency Limit Enforcement
     * Validates: Requirements 5.4
     */
    @Property(tries = 100)
    void concurrentTasksNeverExceedConfiguredLimit(
            @ForAll @IntRange(min = 1, max = 10) int maxConcurrency,
            @ForAll @IntRange(min = 1, max = 50) int taskCount
    ) throws InterruptedException {
        // Track the maximum concurrent executions observed
        AtomicInteger currentConcurrency = new AtomicInteger(0);
        AtomicInteger maxObservedConcurrency = new AtomicInteger(0);
        
        // Use a latch to ensure all tasks have a chance to run concurrently
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch allTasksStarted = new CountDownLatch(taskCount);
        
        VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor(maxConcurrency);
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            tasks.add(() -> {
                // Increment current concurrency
                int current = currentConcurrency.incrementAndGet();
                
                // Update max observed concurrency
                maxObservedConcurrency.updateAndGet(max -> Math.max(max, current));
                
                // Signal that this task has started
                allTasksStarted.countDown();
                
                // Wait for start signal to ensure tasks overlap
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Simulate some work
                Thread.sleep(10);
                
                // Decrement current concurrency
                currentConcurrency.decrementAndGet();
                
                return taskId;
            });
        }
        
        // Start execution in a separate thread
        Thread executorThread = new Thread(() -> {
            executor.executeAll(tasks);
        });
        executorThread.start();
        
        // Wait a bit for tasks to start acquiring semaphore permits
        Thread.sleep(100);
        
        // Release all waiting tasks
        startLatch.countDown();
        
        // Wait for executor to complete
        executorThread.join(10000); // 10 second timeout
        
        // Verify the property: max observed concurrency should not exceed the limit
        assert maxObservedConcurrency.get() <= maxConcurrency 
            : "Concurrency limit violated: observed " + maxObservedConcurrency.get() 
              + " concurrent tasks but limit was " + maxConcurrency;
    }

    /**
     * Property 20: Concurrency Limit Enforcement - All Tasks Complete
     * 
     * Even with concurrency limits, all submitted tasks SHALL eventually complete.
     * 
     * Feature: web-legacy-scan, Property 20: Concurrency Limit Enforcement
     * Validates: Requirements 5.4
     */
    @Property(tries = 100)
    void allTasksCompleteWithConcurrencyLimit(
            @ForAll @IntRange(min = 1, max = 5) int maxConcurrency,
            @ForAll @IntRange(min = 1, max = 20) int taskCount
    ) {
        VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor(maxConcurrency);
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            tasks.add(() -> {
                // Simulate some work
                Thread.sleep(5);
                return taskId;
            });
        }
        
        List<Integer> results = executor.executeAll(tasks);
        
        // All tasks should complete and return results
        assert results.size() == taskCount 
            : "Expected " + taskCount + " results but got " + results.size();
    }

    /**
     * Property 20: Concurrency Limit Enforcement - Single Thread Limit
     * 
     * With maxConcurrency=1, tasks SHALL execute sequentially (never more than 1 concurrent).
     * 
     * Feature: web-legacy-scan, Property 20: Concurrency Limit Enforcement
     * Validates: Requirements 5.4
     */
    @Property(tries = 50)
    void singleThreadLimitEnforcesSequentialExecution(
            @ForAll @IntRange(min = 2, max = 10) int taskCount
    ) throws InterruptedException {
        AtomicInteger currentConcurrency = new AtomicInteger(0);
        AtomicInteger maxObservedConcurrency = new AtomicInteger(0);
        
        VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor(1);
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            tasks.add(() -> {
                int current = currentConcurrency.incrementAndGet();
                maxObservedConcurrency.updateAndGet(max -> Math.max(max, current));
                
                // Simulate work
                Thread.sleep(10);
                
                currentConcurrency.decrementAndGet();
                return taskId;
            });
        }
        
        List<Integer> results = executor.executeAll(tasks);
        
        // With limit of 1, max concurrency should be exactly 1
        assert maxObservedConcurrency.get() == 1 
            : "With maxConcurrency=1, expected max concurrency of 1 but observed " 
              + maxObservedConcurrency.get();
        
        // All tasks should complete
        assert results.size() == taskCount 
            : "Expected " + taskCount + " results but got " + results.size();
    }

    /**
     * Property 20: Concurrency Limit Enforcement - Default Unlimited
     * 
     * With default constructor (no limit), tasks can run with high concurrency.
     * 
     * Feature: web-legacy-scan, Property 20: Concurrency Limit Enforcement
     * Validates: Requirements 5.4
     */
    @Example
    void defaultExecutorAllowsHighConcurrency() throws InterruptedException {
        int taskCount = 20;
        AtomicInteger maxObservedConcurrency = new AtomicInteger(0);
        AtomicInteger currentConcurrency = new AtomicInteger(0);
        CountDownLatch allStarted = new CountDownLatch(taskCount);
        CountDownLatch proceed = new CountDownLatch(1);
        
        VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor();
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            tasks.add(() -> {
                int current = currentConcurrency.incrementAndGet();
                maxObservedConcurrency.updateAndGet(max -> Math.max(max, current));
                allStarted.countDown();
                
                try {
                    proceed.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                currentConcurrency.decrementAndGet();
                return taskId;
            });
        }
        
        Thread executorThread = new Thread(() -> {
            executor.executeAll(tasks);
        });
        executorThread.start();
        
        // Wait for all tasks to start
        Thread.sleep(200);
        
        // Release all tasks
        proceed.countDown();
        
        executorThread.join(5000);
        
        // With no limit, we should see high concurrency (at least more than 1)
        assert maxObservedConcurrency.get() > 1 
            : "Default executor should allow high concurrency but observed only " 
              + maxObservedConcurrency.get();
    }

    /**
     * Property 20: Concurrency Limit Enforcement - Zero or Negative Limit
     * 
     * When maxConcurrency is zero or negative, it SHALL be treated as unlimited.
     * 
     * Feature: web-legacy-scan, Property 20: Concurrency Limit Enforcement
     * Validates: Requirements 5.4
     */
    @Property(tries = 20)
    void zeroOrNegativeLimitTreatedAsUnlimited(
            @ForAll @IntRange(min = -10, max = 0) int invalidLimit
    ) {
        int taskCount = 5;
        
        VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor(invalidLimit);
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            tasks.add(() -> {
                Thread.sleep(5);
                return taskId;
            });
        }
        
        // Should not throw and should complete all tasks
        List<Integer> results = executor.executeAll(tasks);
        
        assert results.size() == taskCount 
            : "Expected " + taskCount + " results but got " + results.size();
    }
}
