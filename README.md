concur3
============

This gives sample Java code for the third article in my JVM Concurrency series on IBM
developerWorks, all in the `com.sosnoski.concur.article3` package.

The three main classes are EventComposition, ThreadSwitchCompletable and ThreadSwitchSynchronized.
EventComposition runs the three variations of event coordination code given in the article. The two
ThreadSwitch variations run thread switching timing tests, using different per-thread data block
sizes (given as an int[] size on the command line) and from 1 to 4096 threads, in powers of two.

ThreadSwitchCompletable uses CompletableFutures to switch between executing threads, with the prior
thread completing a future to trigger the execution of the thread waiting on that future.

ThreadSwitchSynchronized uses basic synchronized wait/notify to switch between executing threads.
