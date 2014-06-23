concur3
============

This gives sample Java code for the third article in my JVM Concurrency series on IBM
developerWorks, all in the `com.sosnoski.concur.article3` package.

The two main classes are EventComposition and ThreadSwitch. EventComposition runs the three variations
of event coordination code given in the article. ThreadSwitch runs a thread switching timing test,
using different per-thread data block sizes and from 1 to 4096 threads, in powers of two.

