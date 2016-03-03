package com.hubcap.utils;

/*
 * #%L
 * HubCap-Core
 * %%
 * Copyright (C) 2016 decoded4620
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.File;
import java.lang.Thread.State;
import java.util.Date;

import com.hubcap.Constants;
import com.hubcap.lowlevel.ExpressionEval;

public class ThreadUtils {

    // reference to our main thread
    private static Thread mainThread;

    /**
     * Only allow setting and clearing of the main thread i.e. cannot
     * 'overwrite', must explicitely setNull first.
     * 
     * @param t
     */
    public static void setMainThread(Thread t) {
        if (mainThread != t) {
            if (mainThread == null && t != null) {
                mainThread = t;
            } else if (t == null && mainThread != null) {
                mainThread = null;
            }
        }
    }

    /**
     * Returns the main Thread.
     * 
     * @return
     */
    public static Thread getMainThread() {
        return mainThread;
    }

    /**
     * Returns true if the thread in question is the 'main thread'
     * 
     * @param t
     *            a Thread to check against the main thread.
     * @return true if the Thread passed in is the main thread.
     */
    public static boolean isMainThread(Thread t) {
        return t == mainThread;
    }

    /**
     * Fold (interrupt and /or join) the thread.
     * 
     * @param t
     *            The Thread to fold.
     * @param join
     *            True to join and interrupt the thread, false, to only
     *            interrupt.
     * @param verbose
     *            true to be verbose.
     * @return true if the thread folded successfully.
     */
    public static boolean fold(Thread t, boolean join, boolean verbose) {
        if (t != mainThread) {

            // first attempt to interrupt the thread
            // get it out of any loops or sleeps
            t.interrupt();

            // then join its execution with the main thread
            // safely
            if (join) {
                return ThreadUtils.safeJoin(t, verbose);
            }

            // only return true if we successfully terminated the thread
            return t.getState() == State.TERMINATED;
        } else {
            System.err.println("HubCap::fold() - CANNOT FOLD MAIN THREAD!");
        }
        return false;
    }

    /**
     * Does a basic calculation on the number of threads you can concuurently
     * run given the desired Cpu Usage, the Cpu wait time, and the Cpu calc
     * time. For slower threaded tasks, such as downloading large files, etc,
     * you may pass in a large number for cpuWaitTime vs. cpuCalcTime (if
     * processing the of data takes .1 X the time it takes to download for
     * example)
     * 
     * @param cpuUsagePct
     *            a float between 0 and 1
     * @param cpuWaitTime
     *            a float
     * @param cpuCalcTime
     *            a float
     * @return the number of concurrent threads that can run with likely
     *         stability given the parameters.
     */
    public static int getStableThreadCount(float cpuUsagePct, float cpuWaitTime, float cpuCalcTime, int maxThreads) {

        // number of processor cores
        int cores = Runtime.getRuntime().availableProcessors();

        // number of disks on the file system (known)
        File[] paths;

        // returns pathnames for files and directory
        paths = File.listRoots();
        int disks = paths.length;

        // c can be 0 -> N
        double c = (cpuWaitTime > 0 && cpuCalcTime > 0) ? cpuWaitTime / cpuCalcTime : 0;
        // final thread calculation
        int numThreads = (int) (2 * cores * disks * cpuUsagePct * (1 + c));
        // clamp the max threads between 1 and MAX_THREADS

        numThreads = Math.max(1, numThreads);
        if (maxThreads > 0) {
            numThreads = Math.min(maxThreads, numThreads);
        }

        System.out.println("Getting Stable thread count:" + numThreads);

        return numThreads;
    }

    /**
     * Wait safely, returns true if wait finished normally. Returns false
     * otherwise. Blocking.
     * 
     * @param factory
     * @return
     */
    public static boolean safeWait(Object factory, boolean verbose) {
        try {
            factory.wait();
            return true;
        } catch (InterruptedException ex) {

            if (verbose) {
                System.err.println("SAFE WAIT!");
                ErrorUtils.printStackTrace(ex);
            }
            return false;
        }
    }

    /**
     * Join a thread safely, returns true if wait finished normally. Returns
     * false otherwise. If thread is stuck in an infinite loop and not
     * responding to interrupts, this could hang your main thread.
     * 
     * @param factory
     * @return
     */
    public static boolean safeJoin(Thread t, boolean verbose) {
        try {
            t.join();
            return true;
        } catch (InterruptedException ex) {
            if (verbose) {
                System.err.println("SAFE JOIN!");
                ErrorUtils.printStackTrace(ex);
            }
            return false;
        }
    }

    /**
     * Wrapper for <code>Thread.sleep</code> that makes it safe.
     * 
     * @param millis
     *            a <code>long</code>
     */
    public static boolean safeSleep(long millis, boolean verbose) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException ex) {
            if (verbose) {
                ErrorUtils.printStackTrace(ex);
            }
            return false;
        }
    }

    /**
     * Performs a 'safe sleep' at tiny intervals until eval.evaluate() returns
     * true. or until the maxTime has been reached.
     * 
     * @param eval
     *            The ExpressionEval to evaluate for true/false response.
     * @param maxTime
     *            a <code>long</code>, the max time to wait if the expression
     *            never returns true. if you pass -1 for this parameter,
     *            Long.MAX_TIME will be used, which is essentially more time
     *            than you or i will live on this planet.
     * @param verbose
     *            true to print any errors.
     * @return a <code>boolean</code>, <code>true</code> if the wait completed
     *         normally. <code>false</code> otherwise.
     */
    public static boolean napUntil(ExpressionEval eval, long maxTime, long interval, boolean verbose) {

        // not infinite, but a long time.
        if (maxTime == -1) {
            maxTime = Long.MAX_VALUE;
        }

        if (interval == -1) {
            interval = Constants.IDLE_TIME;
        }

        long start = (new Date().getTime());
        // wait a bit for tasks to start
        while ((boolean) eval.evaluate() != true) {
            if (!ThreadUtils.safeSleep(interval, verbose) || (maxTime >= 0 && (new Date().getTime()) - start > maxTime)) {
                return false;
            }
        }

        return true;
    }
}
