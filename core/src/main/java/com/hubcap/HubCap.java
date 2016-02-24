package com.hubcap;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.hubcap.task.TaskRunner;
import com.hubcap.task.TaskRunnerListener;
import com.hubcap.task.state.TaskRunnerState;
import com.hubcap.utils.ErrorUtils;

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

    // any arguments passed in when we're busy will be 'enqueued' here
    private List<String[]> busyArgs = Collections.synchronizedList(new ArrayList<String[]>());

    private AtomicLong jobsRequested = new AtomicLong();

    private AtomicLong jobsStarted = new AtomicLong();

    private AtomicLong jobStateChanges = new AtomicLong();

    private AtomicLong jobDataUpdates = new AtomicLong();

    private AtomicLong jobsErrored = new AtomicLong();

    private AtomicLong jobsCompleted = new AtomicLong();

    /**
     * private CTOR, use HubCap.instance()
     */
    private HubCap() {
        // create the REPL
        repl = new REPL(this);
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

    /**
     * Shutdown the current TaskSystem, regardless if tasks are running. This
     * will interrupt all threads.
     */
    public void shutdown() {
        if (this.getState() != ProcessState.SHUTDOWN) {
            // ErrorUtils.printStackTrace(new Exception("SHUTDOWN!!"));
            this.setState(ProcessState.SHUTDOWN);

            HubCap.instance().report();

            shutdownREPL();

            System.out.println("Shutdown threadpool");
            TaskRunner.stopThreadPool();

            ErrorUtils.safeSleep(Constants.IDLE_TIME);
        }
    }

    public void report() {
        System.out.println("---REPORT---");
        System.out.println("   - Active: " + TaskRunner.activeTaskCount());
        System.out.println("   - Free: " + TaskRunner.inactiveTaskCount());
        System.out.println("   - Jobs Requested: " + jobsRequested.get());
        System.out.println("   - Jobs Started: " + jobsStarted.get());
        System.out.println("   - jobs completed: " + jobsCompleted.get());
        System.out.println("   - Jobs Updates: " + jobDataUpdates.get());
        System.out.println("   - Jobs State Changes: " + jobStateChanges.get());
        System.out.println("   - jobs inError: " + jobsErrored.get());
        System.out.println("   - jobs in limbo: " + (TaskRunner.totalTaskCount() - (TaskRunner.activeTaskCount() + TaskRunner.inactiveTaskCount())));
        System.out.println("   - lost jobs: " + (jobsStarted.get() - (jobsCompleted.get() + jobsErrored.get())));
        System.out.println("   - never started jobs: " + (jobsRequested.get() - jobsStarted.get()));
        System.out.println("   - busy args remaining: " + busyArgs.size());
    }

    public boolean isREPL() {
        return (replThread != null && replThread.getState() != Thread.State.TERMINATED);
    }

    private void startREPL() {
        if (replThread == null) {
            System.out.println("startREPL()");
            replThread = new Thread(repl);
            replThread.setName("HubCap-REPL");
            replThread.start();
        }
    }

    private void shutdownREPL() {
        if (replThread != null) {
            System.out.println("shtdownREPL");
            replThread.interrupt();
            System.out.println("repl shutdown!");
            replThread = null;
        }
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

        // test for a shutdown or exit command first
        for (int i = 0; i < args.length; i++) {
            String currArg = args[i];
            if (currArg.equals(Constants.CMD_EXIT) || currArg.equals(Constants.CMD_BYE) || currArg.equals(Constants.CMD_DIE) || currArg.equals(Constants.CMD_QUIT)) {
                shutdown();
                break;
            }
        }

        // No Arguments = REPL MODE
        if (args.length == 0) {
            this.setState(ProcessState.REPL);
            startREPL();
        }
        // Arguments with length > 0 are pushed to a TaskRunner
        else {
            shutdownREPL();

            jobsRequested.incrementAndGet();

            // find a free task
            TaskRunner runner = null;

            // mark the time
            long now = (new Date()).getTime();
            long totalSearchTime = 0;
            boolean searchTimeout = false;

            runner = TaskRunner.getNextAvailableRunner();

            if (runner != null) {

                this.setState(ProcessState.RUNNING);

                runner.addListener(new TaskRunnerListener() {

                    private boolean didStart = false;

                    private boolean didComplete = false;

                    private long startTime;

                    private long endTime;

                    @Override
                    public void onTaskStateChange(TaskRunner runner, TaskRunnerState state) {
                        jobStateChanges.getAndIncrement();
                    }

                    @Override
                    public void onTaskStart(TaskRunner runner) {
                        if (!didStart && !didComplete) {
                            didStart = true;
                            jobsStarted.getAndIncrement();
                            startTime = (new Date().getTime());
                        }

                    }

                    @Override
                    public void onTaskError(TaskRunner runner, Exception e, boolean canRecoverFromError) {
                        if (didStart && !didComplete) {
                            jobsErrored.getAndIncrement();

                            endTime = (new Date().getTime());

                            pruneTasks(listeners);
                        } else {
                            ErrorUtils.printStackTrace(new Exception("UH OH"));
                        }

                        didStart = false;
                        didComplete = false;
                    }

                    @Override
                    public void onTaskDataReceived(TaskRunner runner, Object taskData) {
                        jobDataUpdates.getAndIncrement();
                    }

                    @Override
                    public void onTaskComplete(TaskRunner runner, Object aggregatedResults) {
                        if (didStart && !didComplete) {
                            didComplete = true;
                            endTime = (new Date().getTime());

                            jobsCompleted.getAndIncrement();
                            pruneTasks(listeners);
                        } else {
                            ErrorUtils.printStackTrace(new Exception("UH OH"));
                        }

                        didStart = false;
                        didComplete = false;
                    }
                });

                for (int i = 0; i < listeners.length; i++) {
                    runner.addListener(listeners[i]);
                }

                runner.setTaskInput(args);

            } else {
                this.setState(ProcessState.BUSY);

                jobsRequested.decrementAndGet();
                busyArgs.add(args);
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

    private void pruneTasks(TaskRunnerListener[] listeners) {
        if (TaskRunner.inactiveTaskCount() > Constants.MIN_NOT_BUSY_THREAD_COUNT || TaskRunner.activeTaskCount() == 0) {
            HubCap.this.setState(ProcessState.RUNNING);

            synchronized (busyArgs) {

                // find inactive

                int i = 0;
                while (TaskRunner.inactiveTaskCount() > 0 && HubCap.this.getState() != ProcessState.BUSY && busyArgs.size() > 0) {
                    HubCap.this.processArgs(busyArgs.remove(0), listeners);
                    ++i;
                }

                if (i > 0) {
                    System.out.println("Added " + i + " busy args to munch, " + busyArgs.size() + " remain!");
                }
            }
        }
    }
}
