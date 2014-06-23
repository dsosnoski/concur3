package com.sosnoski.concur.article3;

import java.text.DecimalFormat;

public class ThreadSwitch implements Runnable {
    
    /** Total increments spread across all threads. */
    private static final int TOTAL_INCREMENTS = 4096 * 400;

    /** Lock object for increments completed. */
    private static final Object doneLock = new Object();

    /** Counter incremented by all threads. */
    private static int sharedCounter;

    /** Increment count for thread. */
    private final int incrementCount;
    
    /** Ready to execute next increment flag. */
    private volatile boolean readyToRun;
    
    /** Data for thread. */
    private final int[] dataValues;
    
    /** Sum of data. */
    private int dataSum;

    /** Switcher instance to be notified after this one has incremented counter. */
    private ThreadSwitch runNext;
    
    public ThreadSwitch(int count, int[] data) {
        dataValues = data;
        incrementCount = count;
        dataSum = sumData();
    }
    
    public void setNext(ThreadSwitch next) {
        runNext = next;
    }
    
    private int sumData() {
        int acc = 0;
        for (int i = 0; i < dataValues.length; i++) {
            acc += dataValues[i];
        }
        return acc;
    }
    
    public synchronized void runNow() {
        readyToRun = true;
        notify();
    }

    public void run() {
        int remain = incrementCount;
        while (remain-- > 0) {

            // wait for signal before proceeding
            synchronized (this) {
                while (!readyToRun) {
                    try {
                        wait();
                    } catch (InterruptedException e) { /* ignored */
                    }
                }
                readyToRun = false;
            }
            
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
            synchronized (doneLock) {
                doneLock.notify();
            }
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
        ThreadSwitch[] switchers = new ThreadSwitch[count];
        for (int i = 0; i < count; i++) {
            int[] data = new int[size];
            for (int j = 0; j < size; j++) {
                data[j] = i + j;
            }
            switchers[i] = new ThreadSwitch(TOTAL_INCREMENTS / count, data);
        }
        Thread[] threads = new Thread[count];
        for (int i = 0; i < count; i++) {
            ThreadSwitch switcher = switchers[i];
            switcher.setNext(switchers[(i + 1) % count]);
            threads[i] = new Thread(switcher);
            threads[i].start();
        }
        
        // start executing the switchers in sequence
        long start = System.currentTimeMillis();
        sharedCounter = 0;
        switchers[0].runNow();
        synchronized (doneLock) {
            while (true) {
                if (sharedCounter >= TOTAL_INCREMENTS) {
                    break;
                }
                try {
                    doneLock.wait();
                } catch (InterruptedException e) { /* ignored */ }
            }
        }
        if (print) {
            long time = System.currentTimeMillis() - start;
            double micros = (double)time * 1000.0 / TOTAL_INCREMENTS;
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
            System.out.println("Beginning run with " + size + "-byte data blocks per thread");
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