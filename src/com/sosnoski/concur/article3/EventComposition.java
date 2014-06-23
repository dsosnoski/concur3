package com.sosnoski.concur.article3;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class EventComposition {
    
    // task definitions
    private static CompletableFuture<Integer> task1(int input) {
        return TimedEventSupport.delayedSuccess(1, input + 1);
    }
    private static CompletableFuture<Integer> task2(int input) {
        return TimedEventSupport.delayedSuccess(2, input + 2);
    }
    private static CompletableFuture<Integer> task3(int input) {
        return TimedEventSupport.delayedSuccess(3, input + 3);
    }
    private static CompletableFuture<Integer> task4(int input) {
        return TimedEventSupport.delayedSuccess(1, input + 4);
    }
    
    /**
     * Run events with blocking waits.
     * 
     * @return future for result (already complete)
     */
    private static CompletableFuture<Integer> runBlocking() {
        Integer i1 = task1(1).join();
        CompletableFuture<Integer> future2 = task2(i1);
        CompletableFuture<Integer> future3 = task3(i1);
        Integer result = task4(future2.join() + future3.join()).join();
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * Run events with composition.
     * 
     * @return future for result
     */
    private static CompletableFuture<Integer> runNonblocking() {
        return task1(1).thenCompose(i1 -> ((CompletableFuture<Integer>)task2(i1)
            .thenCombine(task3(i1), (i2,i3) -> i2+i3)))
            .thenCompose(i4 -> task4(i4));
    }
    
    /**
     * Run task2 and task3 and combine the results. This is just a refactoring of {@link #runNonblocking()} to make it
     * easier to understand the code.
     * 
     * @param i1
     * @return future representing the sum of the task2 and task3 result values
     */
    private static CompletableFuture<Integer> runTask2and3(Integer i1) {
        CompletableFuture<Integer> task2 = task2(i1);
        CompletableFuture<Integer> task3 = task3(i1);
        BiFunction<Integer, Integer, Integer> sum = (a, b) -> a + b;
        return task2.thenCombine(task3, sum);
    }
    
    /**
     * Run events with composition. This is just a refactoring of {@link #runNonblocking()} to make it easier to
     * understand the code.
     * 
     * @return future for result
     */
    private static CompletableFuture<Integer> runNonblockingAlt() {
        CompletableFuture<Integer> task1 = task1(1);
        CompletableFuture<Integer> comp123 = task1.thenCompose(EventComposition::runTask2and3);
        return comp123.thenCompose(EventComposition::task4);
    }
    
    /**
     * Time and print future result.
     * 
     * @param test supplier for future
     * @param name test case name
     */
    private static void timeComplete(Supplier<CompletableFuture<Integer>> test, String name) {
        System.out.println("Starting " + name);
        long start = System.currentTimeMillis();
        Integer result = test.get().join();
        long time = System.currentTimeMillis() - start;
        System.out.println(name + " returned " + result + " in " + time + " ms.");
    }
    
    public static void main(String[] args) throws Exception {
        timeComplete(EventComposition::runBlocking, "runBlocking");
        timeComplete(EventComposition::runNonblocking, "runNonblocking");
        timeComplete(EventComposition::runNonblockingAlt, "runNonblockingAlt");
    }
}