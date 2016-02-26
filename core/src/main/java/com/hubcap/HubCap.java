package com.hubcap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.hubcap.lowlevel.StringUtils;
import com.hubcap.process.ProcessModel;
import com.hubcap.process.ProcessState;
import com.hubcap.task.TaskRunner;
import com.hubcap.task.TaskRunnerListener;
import com.hubcap.task.state.TaskRunnerState;
import com.hubcap.utils.ErrorUtils;
import com.hubcap.utils.ThreadUtils;

/*
 * #%L
 * HubCap
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

public class HubCap {

    private static HubCap inst;

    /**
     * Static Accessor For HubCap
     * 
     * @return
     */
    public static HubCap instance() {
        if (inst == null) {
            inst = new HubCap();
        }
        return inst;
    }

    /**
     * Main Line Entry Point
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Hubcap");

        ThreadUtils.setMainThread(Thread.currentThread());

        HubCap instance = HubCap.instance();

        // process any initial arguments
        instance.processArgs(args);
    }

    // the current state of HubCap
    private ProcessState state = ProcessState.DORMANT;

    // The thread that manages the REPL.
    private Thread replThread;

    // The REPL
    private REPL repl = null;

    private AtomicLong jobsRequested = new AtomicLong();

    private AtomicLong jobsStarted = new AtomicLong();

    private AtomicLong jobStateChanges = new AtomicLong();

    private AtomicLong jobDataUpdates = new AtomicLong();

    private AtomicLong jobsErrored = new AtomicLong();

    private AtomicLong jobsCantRecover = new AtomicLong();

    private AtomicLong jobsCompleted = new AtomicLong();

    /**
     * private CTOR, use HubCap.instance()
     */
    private HubCap() {
    }

    public REPL repl() {
        return repl;
    }

    /**
     * Start the thread pool
     */
    public void startup() {
        if (this.state == ProcessState.DORMANT) {
            this.setState(ProcessState.STARTUP);

            // start the task runner which
            // will queue up all of our task runner threads and wait for the
            // next job.
            TaskRunner.startThreadPool();
        }
    }

    private Object mutex = new Object();

    private Thread shutdownThread;

    /**
     * Shutdown the current TaskSystem, regardless if tasks are running. This
     * will interrupt all threads.
     */
    public void shutdown() {
        synchronized (mutex) {
            if (this.getState() != ProcessState.SHUTDOWN) {
                ErrorUtils.printStackTrace(new Exception("SHUTDOWN!!"));
                this.setState(ProcessState.SHUTDOWN);
                if (shutdownThread == null) {

                    // spawn a daemon
                    shutdownThread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            System.out.println("Spawned shutdown thread...");
                            if (ThreadUtils.safeSleep(Constants.IDLE_TIME, ProcessModel.instance().getVerbose())) {
                                HubCap.instance().report();

                                shutdownREPL();

                                System.out.println("Shutdown threadpool");
                                TaskRunner.stopThreadPool();

                                if (!ThreadUtils.safeSleep(Constants.IDLE_TIME, ProcessModel.instance().getVerbose())) {
                                    return;
                                }
                            }
                            System.out.println("Exiting shutdown thread..");
                        }
                    });
                    shutdownThread.setName("Shutdown Thread");
                    shutdownThread.setDaemon(true);
                    shutdownThread.start();
                }
            }
        }
    }

    public void report() {
        System.out.println("---REPORT---");
        System.out.println("   - Total Tasks: " + TaskRunner.totalTaskCount());
        System.out.println("   - Active: " + TaskRunner.activeTaskCount());
        System.out.println("   - Free: " + TaskRunner.inactiveTaskCount());
        System.out.println("   - Jobs Requested: " + jobsRequested.get());
        System.out.println("   - Jobs Started: " + jobsStarted.get());
        System.out.println("   - jobs completed: " + jobsCompleted.get());
        System.out.println("   - Jobs Updates: " + jobDataUpdates.get());
        System.out.println("   - Jobs State Changes: " + jobStateChanges.get());
        System.out.println("   - jobs inError: " + jobsErrored.get());
        System.out.println("   - unrecoverable Jobs: " + jobsCantRecover.get());
        System.out.println("   - jobs in limbo: " + (TaskRunner.limboTaskCount()));
        System.out.println("   - lost jobs: " + (jobsStarted.get() - (jobsCompleted.get() + jobsErrored.get())));
        System.out.println("   - never started jobs: " + (jobsRequested.get() - jobsStarted.get()));
        System.out.println("   - busy args remaining: " + TaskRunner.countBusyArgs());
    }

    public boolean isREPL() {
        return (replThread != null && replThread.getState() != Thread.State.TERMINATED);
    }

    /**
     * Process the current set of arguments. You may pass in a set of listeners
     * to track this specific process. The listeners are 'ephemeral' in that
     * they are removed from the Process once it completes THIS job. It makes it
     * less likely to have leaky listeners on subsequence responses
     * 
     * @param args
     */
    public int processArgs(String[] args, TaskRunnerListener... listeners) {

        int ret = 0;
        if (this.state == ProcessState.SHUTDOWN) {
            System.err.println("Cannot process arguments, we're sutting down");
            ret = 1;
            return ret;
        }

        // check the Task System, and start if necessary
        if (!TaskRunner.isTaskSystemReady()) {
            startup();
        }

        // if we're already busy, or there are busy args left to munch
        // just push ours on the end and bail.
        if (this.state == ProcessState.BUSY || TaskRunner.countBusyArgs() > 0) {
            ret = 2;
            TaskRunner.addBusyArgs(args, listeners);
            return ret;
        }

        // No Arguments = REPL MODE
        if (args.length == 0) {
            System.out.println("No args, going REPL");
            if (!this.isREPL()) {
                startREPL();
            }
        }
        // Arguments with length > 0 are pushed to a TaskRunner
        else {

            // shutdown the REPLThread while the tasks are running
            if (isREPL()) {
                shutdownREPL();
            }

            jobsRequested.incrementAndGet();

            // find a free task
            TaskRunner runner = null;

            runner = TaskRunner.getNextAvailableRunner();

            if (runner != null) {

                this.setState(ProcessState.RUNNING);

                runner.addListener(new TaskRunnerListener() {

                    @Override
                    public void onTaskStateChange(TaskRunner runner, TaskRunnerState state) {
                        jobStateChanges.incrementAndGet();
                    }

                    @Override
                    public void onTaskStart(TaskRunner runner) {
                        jobsStarted.incrementAndGet();
                    }

                    @Override
                    public void onTaskError(TaskRunner runner, Exception e, boolean canRecoverFromError) {
                        jobsErrored.incrementAndGet();

                        if (!canRecoverFromError) {
                            jobsCantRecover.incrementAndGet();
                        }

                    }

                    @Override
                    public void onTaskDataReceived(TaskRunner runner) {
                        jobDataUpdates.incrementAndGet();
                    }

                    @Override
                    public void onTaskComplete(TaskRunner runner) {

                        jobsCompleted.incrementAndGet();

                        if (ProcessModel.instance().getREPLFallback()) {
                            // once this task completes
                            // check the process model for REPL Fallback
                            // if we're the final task
                            if (TaskRunner.waitingTaskCount() == 1) {
                                startREPL();
                            }
                        } else {
                            if (TaskRunner.waitingTaskCount() == 1) {
                                shutdown();
                            }
                        }
                    }
                });

                for (int i = 0; i < listeners.length; i++) {
                    runner.addListener(listeners[i]);
                }

                runner.setTaskInput(args);

            } else {
                this.setState(ProcessState.BUSY);
                jobsRequested.decrementAndGet();
                TaskRunner.addBusyArgs(args, listeners);
            }
        }

        return ret;
    }

    public void setState(ProcessState state) {
        if (this.state != state) {
            this.state = state;
        }
    }

    public ProcessState getState() {
        return this.state;
    }

    public void startREPL() {
        if (replThread == null) {
            System.out.println("startREPL()");
            repl = new REPL(this);
            replThread = new Thread(repl);
            replThread.setName("HubCap-REPL " + (new Date().getTime()));
            replThread.start();
        }
    }

    public void shutdownREPL() {
        if (replThread != null) {
            System.out.println("HubCap::shutdownREPL()");
            repl.shutdown();
            repl = null;
            replThread = null;
        }
    }

}
