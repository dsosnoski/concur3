package com.sosnoski.concur.article3;

import java.text.DecimalFormat;
import java.util.concurrent.CompletableFuture;

public class ThreadSwitchCompletable implements Runnable {
    
    /** Total increments spread across all threads. */
    private static final int TOTAL_INCREMENTS = 4096 * 400;

    /** Increments completed when done. */
    private static CompletableFuture<Boolean> doneLock;

    /** Counter incremented by all threads. */
    private static int sharedCounter;

    /** Increment count for thread. */
    private final int incrementCount;
    
    /** Data for thread. */
    private final int[] dataValues;
    
    /** Ready to execute next increment when complete. */
    private volatile CompletableFuture<Boolean> readyToRun;
    
    /** Sum of data. */
    private int dataSum;

    /** Switcher instance to be notified after this one has incremented counter. */
    private ThreadSwitchCompletable runNext;
    
    public ThreadSwitchCompletable(int count, int[] data) {
        dataValues = data;
        incrementCount = count;
        dataSum = sumData();
        readyToRun = new CompletableFuture<Boolean>();
    }
    
    public void setNext(ThreadSwitchCompletable next) {
        runNext = next;
    }
    
    private int sumData() {
        int acc = 0;
        for (int i = 0; i < dataValues.length; i++) {
            acc += dataValues[i];
        }
        return acc;
    }
    
    public void runNow() {
        readyToRun.complete(Boolean.TRUE);
    }

    public void run() {
        int remain = incrementCount;
        while (remain-- > 0) {

            // wait for signal before proceeding
            readyToRun.join();
            readyToRun = new CompletableFuture<Boolean>();
            
            // load data into cache
            int sum = sumData();
            if (sum != dataSum) {
                throw new IllegalStateException("Time to check the CPU");
            }

            // increment counter and signal next to run
            sharedCounter++;
            runNext.runNow();
        }
        
        // signal main thread when last thread completes
        if (sharedCounter == TOTAL_INCREMENTS) {
            doneLock.complete(Boolean.TRUE);
        }
    }

    /**
     * Time counter increments switching between specified number of threads.
     * 
     * @param count thread count
     * @param size data block size
     * @param print print results flag
     */
    private static void timeRun(int count, int size, boolean print) {
        
        // build and configure the switchers and threads
        ThreadSwitchCompletable[] switchers = new ThreadSwitchCompletable[count];
        for (int i = 0; i < count; i++) {
            int[] data = new int[size];
            for (int j = 0; j < size; j++) {
                data[j] = i + j;
            }
            switchers[i] = new ThreadSwitchCompletable(TOTAL_INCREMENTS / count, data);
        }
        Thread[] threads = new Thread[count];
        for (int i = 0; i < count; i++) {
            ThreadSwitchCompletable switcher = switchers[i];
            switcher.setNext(switchers[(i + 1) % count]);
            threads[i] = new Thread(switcher);
            threads[i].start();
        }
        
        // start executing the switchers in sequence
        doneLock = new CompletableFuture<Boolean>();
        long start = System.nanoTime();
        sharedCounter = 0;
        switchers[0].runNow();
        doneLock.join();
        if (print) {
            long time = System.nanoTime() - start;
            double micros = (double)time * 0.001 / TOTAL_INCREMENTS;
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(3);
            df.setMinimumFractionDigits(3);
            System.out.println(df.format(micros) + " microseconds per switch (" + time + " ms. total) with " + count + " threads");
        }
        for (int i = 0; i < count; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) { /* ignored */ }
        }
    }

    public static void main(String[] args) {
        
        // initialize with a couple of non-printing passes
        timeRun(1, 1024, false);
        timeRun(16, 1024, false);
        
        // run once for each command line value
        for (int i = 0; i < args.length; i++) {
            int size = Integer.parseInt(args[i]);
            System.out.println("Beginning run with " + (size * 4) + "-byte data blocks per thread");
            int count = 1;
            for (int j = 0; j < 13; j++) {
                timeRun(count, size, true);
                count *= 2;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { /* ignored */ }
        }
    }
}