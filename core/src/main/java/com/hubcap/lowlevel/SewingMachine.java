package com.hubcap.lowlevel;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

import com.hubcap.Constants;
import com.hubcap.lowlevel.ThreadSpool.ThreadSpoolState;
import com.hubcap.utils.ThreadUtils;

/**
 * Task Manager which uses a basic threadpool to much through tasks. NOTE: There
 * can only be a limited number of these guys instantiated or the Thread count
 * gets ridiculous.
 * 
 * @author Decoded4620 2016
 */
public class SewingMachine implements Runnable {

    // no more than this many Sewing Machines
    // or our thread count will be heinous.
    public static final int MAX_SEWING_MACHINES = 1;

    public static final int MAX_THREADS_PER_MACHINE = 512;

    // NOTE: Java only
    private static List<ThreadSpoolTask> taskSet = Collections.synchronizedList(new ArrayList<ThreadSpoolTask>());

    private static List<Thread> threads = Collections.synchronizedList(new ArrayList<Thread>());

    public static boolean verbose = false;

    public static SewingMachine spawn(int numThreads, String threadNamePrefix) throws InstantiationException {
        if (threads.size() >= MAX_SEWING_MACHINES) {
            // wait in case we breach the max
            if (verbose) {
                System.err.println(threads.size() + " are running, waiting a few millis for more threads to be free");
            }
            while (threads.size() >= MAX_SEWING_MACHINES) {
                if (!ThreadUtils.safeSleep(Constants.POOL_SHUTDOWN_CHECK_INTERVAL, verbose)) {
                    break;
                }
            }
            // wait in case we breach the max
            if (verbose) {
                System.out.println("found a free thread!");
            }
        }

        SewingMachine machine = new SewingMachine(numThreads, threadNamePrefix);
        threads.add(machine.getThread());
        System.out.println("SewingMachine::spawn() - threads: " + threads.size());
        return machine;
    }

    public static void report() {
        System.out.println("[SewingMachine Report]: instances: " + threads.size());
    }

    private String threadNamePrefix = null;

    private int numThreads = 0;

    private ExecutorService threadPool;

    private List<ThreadSpool> spoolRack = Collections.synchronizedList(new ArrayList<ThreadSpool>());

    private Thread myThread;

    private String id;

    /**
     * Constructor
     * 
     * @param numThreads
     *            the number of threads to use to process the queue.
     * @param threadNamePrefix
     *            The Prefix for thread names
     */
    public SewingMachine(int numThreads, String threadNamePrefix) throws InstantiationException {

        synchronized (threads) {
            this.numThreads = Math.min(SewingMachine.MAX_THREADS_PER_MACHINE, numThreads);
            id = "SewingMachine::" + threads.size();
            if (verbose) {
                System.out.println(id + "::Construct(" + threadNamePrefix + ", threads: " + this.numThreads + ")");
            }
            this.threadNamePrefix = threadNamePrefix;

            if (myThread == null) {
                Thread thread = new Thread(this);
                thread.setName("SewingMachine::" + threadNamePrefix);
                thread.setDaemon(false);
                thread.start();
                myThread = thread;
            }

            if (threads.size() > MAX_SEWING_MACHINES) {
                throw new InstantiationException("Too Many Sewing Machines already constructed");
            }
        }
    }

    public Thread getThread() {
        return myThread;
    }

    public boolean shuttingDown() {
        return this.isShuttingDown;
    }

    private boolean isShuttingDown = false;

    public void gracefulShutdown() {
        if (!isShuttingDown) {
            isShuttingDown = true;
            if (threadPool != null) {

                synchronized (spoolRack) {
                    for (ThreadSpool spool : spoolRack) {
                        spool.state = ThreadSpoolState.SHUTDOWN;
                    }
                }
                System.out.println(id + "::gracefulShutdown() waiting for " + taskCount() + " tasks still unperformed");
                threadPool.shutdown();
            }
        }
    }

    public void shutdown() {
        if (!isShuttingDown) {
            isShuttingDown = true;
            if (threadPool != null) {
                System.out.println(id + "::shutdown() with " + taskCount() + " tasks still unperformed");

                // insure interruption
                for (ThreadSpool spool : spoolRack) {
                    spool.state = ThreadSpoolState.SHUTDOWN;
                }

                threadPool.shutdownNow();
                threads.clear();
            }
        }
    }

    public int taskCount() {
        return taskSet.size();
    }

    public int runningSpools() {
        return ThreadSpool.activeSpools.get();
    }

    public synchronized void assignTask(ThreadSpoolTask t) {
        if (verbose) {
            System.out.println(id + "::assignTask(" + t + ") -> current count: " + taskSet.size());
        }

        taskSet.add(t);
    }

    @Override
    public void run() {

        if (verbose) {
            System.out.println(id + "::run(" + numThreads + ")");
        }

        // stack up all the threads now with a safety buffer
        threadPool = Executors.newFixedThreadPool(this.numThreads, new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(false);
                t.setName(SewingMachine.this.threadNamePrefix + (new Date().getTime()));
                return t;
            }
        });

        for (int i = 0; i < numThreads; ++i) {

            ThreadSpool spool = new ThreadSpool(this, taskSet, id + "-ThreadSpool-" + i);

            try {
                threadPool.execute(spool);
                spoolRack.add(spool);
            } catch (RejectedExecutionException ex) {
                System.err.println(id + "::Hit a ceiling, will only process with: " + spoolRack.size() + " spools");
                // wait
                break;
            }
        }

        while (!threadPool.isShutdown()) {
            if (!ThreadUtils.safeSleep(Constants.IDLE_TIME, false)) {
                threadPool.shutdownNow();
                break;
            }
        }

        if (verbose) {
            System.err.println(id + " - ThreadPool Shutdown");
        }
        while (!threadPool.isTerminated()) {
            if (!ThreadUtils.safeSleep(Constants.IDLE_TIME, false)) {
                threadPool.shutdownNow();
                break;
            }

        }

        if (!threads.contains(myThread)) {
            System.err.println("Cannot remove my thread!");
        } else {
            threads.remove(myThread);
        }
        myThread = null;

        if (verbose) {
            System.out.println(id + "::exit() - remaining: " + threads.size());
        }
    }
}
