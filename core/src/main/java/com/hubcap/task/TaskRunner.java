/**
 * 
 */
package com.hubcap.task;

import java.lang.Thread.State;

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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.hubcap.Constants;
import com.hubcap.inputs.CLIOptionBuilder;
import com.hubcap.task.state.TaskRunnerState;
import com.hubcap.utils.ErrorUtils;
import com.hubcap.utils.ThreadUtils;

/**
 * @author Decoded4620 2016
 */
public class TaskRunner implements Runnable {

    // keeps a sane list of task runners, uses concurrency package
    private static List<TaskRunner> runningTasks = Collections.synchronizedList(new ArrayList<TaskRunner>());

    private static List<TaskRunner> freeTasks = Collections.synchronizedList(new ArrayList<TaskRunner>());

    private static List<TaskRunner> orphans = Collections.synchronizedList(new ArrayList<TaskRunner>());

    // keeps a sane list of task threads, uses concurrency package
    private static List<Thread> taskThreads = Collections.synchronizedList(new ArrayList<Thread>());

    private static AtomicInteger limboTasks = new AtomicInteger(0);

    private static ThreadFactory factory = null;

    private static ExecutorService threadPool = null;

    private static Thread monitorThread;

    private static boolean isTaskSystemReady = false;

    private static void runTask(TaskRunner task) {
        if (!runningTasks.contains(task)) {
            runningTasks.add(task);
        }
    }

    private static void freeTask(TaskRunner task) {
        if (!freeTasks.contains(task)) {
            freeTasks.add(task);
        }
    }

    /**
     * Returns the next available task runner.
     * 
     * @return a <code>TaskRunner</code>
     */
    public static TaskRunner getNextAvailableRunner() {

        TaskRunner ret = null;
        ArrayList<TaskRunner> fixErrors = new ArrayList<TaskRunner>();

        synchronized (freeTasks) {
            Iterator<TaskRunner> i = freeTasks.iterator();

            while (i.hasNext()) {
                TaskRunner runner = i.next();
                if (runner.getTaskState() == TaskRunnerState.ERROR) {
                    // replace any errored runners with a new freshy.
                    // this will go to the end of the list
                    // so it could feasably be the next available one.
                    fixErrors.add(runner);
                } else if (runner.getTaskState() == TaskRunnerState.IDLE || runner.getTaskState() == TaskRunnerState.DORMANT) {
                    ret = runner;

                    // track counted task in 'limbo' until we get
                    // it added to the runningTaskList
                    // this way 'totalTasks' calls will be accurate.
                    limboTasks.incrementAndGet();
                    // prime it now and remove it from the free list
                    runner.setTaskState(TaskRunnerState.PRIMED);
                    i.remove();
                    break;
                }
            }
        }

        if (fixErrors.size() > 0) {

            TaskRunner[] errors = fixErrors.toArray(new TaskRunner[fixErrors.size()]);

            // sanity
            fixErrors.clear();

            for (int x = 0; x < errors.length; ++x) {
                TaskRunner currentRunner = errors[x];
                TaskRunner newOrOrphanedRunner = null;
                boolean err = false;
                try {

                    // even tho we stop tracking this object here
                    // it may still be executing on the threadPool.
                    // so if we aren't able to execute the next orphan / new
                    // TaskRunner
                    // replacement,
                    // then we'll keep trying for every no task request to
                    // fix the previous errors.
                    if (runningTasks.contains(currentRunner)) {
                        runningTasks.remove(currentRunner);
                    }

                    // grab an orphan, or make a new runner.
                    newOrOrphanedRunner = orphans.size() > 0 ? orphans.remove(orphans.size() - 1) : new TaskRunner();

                    threadPool.execute(newOrOrphanedRunner);

                    if (!freeTasks.contains(newOrOrphanedRunner)) {
                        TaskRunner.freeTask(newOrOrphanedRunner);
                    }

                } catch (RejectedExecutionException rex) {

                    ErrorUtils.printStackTrace(rex);
                    err = true;
                } catch (NullPointerException nex) {
                    ErrorUtils.printStackTrace(nex);
                    err = true;
                } finally {
                    if (err) {
                        if (freeTasks.contains(newOrOrphanedRunner)) {
                            freeTasks.remove(newOrOrphanedRunner);
                        }

                        if (!orphans.contains(newOrOrphanedRunner)) {
                            // put it back on the orphan list
                            orphans.add(newOrOrphanedRunner);
                        }
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Returns true the thread pool executor has been started.
     * 
     * @return a <code>Boolean</code>
     */
    public static boolean isTaskSystemReady() {
        return isTaskSystemReady;
    }

    /**
     * Thread-safe active task count.
     * 
     * @return a <code>int</code>, 0 if there are no currently active tasks
     */
    public static int activeTaskCount() {

        int act = 0;
        synchronized (runningTasks) {
            act = runningTasks.size();
        }
        return act;
    }

    /**
     * Thread-Safe inactive task count.
     * 
     * @return
     */
    public static int inactiveTaskCount() {
        int inact = 0;
        synchronized (freeTasks) {
            inact = freeTasks.size();
        }
        return inact;
    }

    public static int totalTaskCount() {
        int total;
        synchronized (freeTasks) {
            synchronized (runningTasks) {
                total = freeTasks.size() + runningTasks.size() + limboTasks.get();
            }
        }

        return total;
    }

    /**
     * Stop the thread pool executor, and kill all current threads
     */
    public static void stopThreadPool() {

        if (isTaskSystemReady) {
            System.out.println("stopThreadPool()");

            if (TaskRunner.factory != null) {
                TaskRunner.factory = null;
            }

            threadPool.shutdown();

            for (TaskRunner t : runningTasks) {
                t.setTaskState(TaskRunnerState.SHUTDOWN);
            }
            runningTasks.clear();

            for (TaskRunner t : freeTasks) {
                t.setTaskState(TaskRunnerState.SHUTDOWN);
            }
            freeTasks.clear();

            if (taskThreads != null) {
                for (Thread t : taskThreads) {
                    try {

                        t.interrupt();
                        if (t.getState() == State.WAITING || t.getState() == State.TIMED_WAITING) {
                            // stop the this task thread.
                            t.join();
                        }

                    } catch (InterruptedException e) {
                        // let the dev see this error to avoid ultimate hang
                        ErrorUtils.printStackTrace(e);
                    }
                }

                taskThreads.clear();
            }

            if (monitorThread != null) {
                monitorThread.interrupt();
                monitorThread = null;
            }

            isTaskSystemReady = false;
        }
    }

    /**
     * Starts the ThreadPoolExecutor which builds a set of TaskRunner instances
     * which will wait for inputs (from the user)
     */
    public static void startThreadPool() {
        if (!isTaskSystemReady) {
            System.out.println("startThreadPool()");

            isTaskSystemReady = true;

            // used to id the threads 'atomically'
            final AtomicLong count = new AtomicLong(0);
            if (TaskRunner.factory == null) {

                TaskRunner.factory = new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        if (runningTasks.contains(r)) {
                            throw new IllegalStateException("Cannot add duplicate runnable to running tasks");
                        }

                        // TODO Auto-generated method stub
                        Thread thread = new Thread(r);
                        thread.setDaemon(false);
                        thread.setName("HubcapTaskRunnerThread-" + count.getAndIncrement());
                        taskThreads.add(thread);
                        return thread;
                    }
                };

                // calculates the current stable thread count based on the
                // assumption
                // that it takes 'X' times the amount of time to transfer data
                // (from github)
                // as it does to process said data (including Gson
                // transformation)
                // and the limit of Y% use of CPU. MAX_THREADS provides a safe
                // and stable cap for
                // systems that are so 'badass' that we would break the cap. \
                // (i.e. i have 32 cores and 12 disks = (2*32*12*1(1+5/1) =
                // 4600 threads, a bit high)...)
                int numThreads = ThreadUtils.getStableThreadCount(.50f, 20f, 1, Constants.MAX_THREADS);

                synchronized (freeTasks) {
                    System.out.println("creating: " + numThreads + " threads for hubcap");
                    TaskRunner.threadPool = Executors.newFixedThreadPool(numThreads, TaskRunner.factory);
                    for (int i = 0; i < numThreads; ++i) {
                        TaskRunner tr = new TaskRunner();
                        threadPool.execute(tr);
                        TaskRunner.freeTask(tr);
                    }
                }
                // pass the monitoring code to another thread
                // so we don't block the REPL loop
                monitorThread = new Thread(new Runnable() {

                    @Override
                    public void run() {

                        while (!threadPool.isShutdown()) {
                            try {
                                Thread.sleep(Constants.POOL_SHUTDOWN_CHECK_INTERVAL);
                            } catch (InterruptedException ex) {
                                break;
                            }
                        }

                        System.out.println("Thread Pool was shutdown");

                        while (!threadPool.isTerminated()) {
                            try {
                                Thread.sleep(Constants.POOL_TERM_CHECK_INTERVAL);
                            } catch (InterruptedException ex) {
                                break;
                            }
                        }

                        System.out.println("Finished all threads");
                    }
                });

                monitorThread.setName("TaskMonDaemon");
                monitorThread.setDaemon(false);

                // start monitoring
                monitorThread.start();
            }
        } else {
            throw new IllegalStateException("Hubcap task runner can only be initialized once!");
        }
    }

    // uniq id for this task
    private long taskId;

    // set of listeners for actions that happen from this Task Runner
    // use concurrent list since listeners may be added from another thread, and
    // since listener callbacks may be accessing this list (i.e. removing
    // themselves from it)
    private List<TaskRunnerListener> listeners = Collections.synchronizedList(new ArrayList<TaskRunnerListener>());

    private String[] taskArgs = null;

    // the current task state
    private TaskRunnerState taskState = TaskRunnerState.DORMANT;

    private String errorMessage = "";

    private boolean canRecoverFromError = false;

    /**
     * Private Constructor, use 'createNew()' to make new task runners.
     */
    private TaskRunner() {
        // TODO Auto-generated constructor stub
        taskId = (new Date().getTime());
        Random r = new Random(taskId);

        // get a pseudo random to lessen the chance that two tasks started at
        // the same time will have the same id.
        // getTime() returns milliseconds, and milliseconds are rather long in
        // the scheme of things.
        taskId += Math.abs(r.nextLong());
    }

    /**
     * Add a listener to handle Task Events. Note: Listeners are ephemeral and
     * removed automatically from a task once it completes, or has an error.
     * 
     * @param listener
     * @return
     */
    public boolean addListener(TaskRunnerListener listener) {
        boolean added = false;

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        return added;
    }

    /**
     * Read Only - the id for this task.
     * 
     * @return a <code>long</code>
     */
    public long getTaskId() {
        return this.taskId;
    }

    /**
     * returns the current state of this task runner
     */
    public TaskRunnerState getTaskState() {
        return this.taskState;
    }

    private void setTaskState(TaskRunnerState state) {
        if (this.taskState != state) {
            this.taskState = state;
            TaskRunnerListener[] list = listeners.toArray(new TaskRunnerListener[listeners.size()]);
            for (int i = 0; i < list.length; i++) {
                TaskRunnerListener listener = list[i];
                listener.onTaskStateChange(this, state);
            }
        }
    }

    public void shutdown() {
        setTaskState(TaskRunnerState.SHUTDOWN);
        this.taskArgs = null;
    }

    /**
     * Takes a set of input arguments And processes the task instructions from
     * them. This task is a unique thread that will shutdown once finished.
     * 
     * @param args
     */
    public void setTaskInput(String[] args) {
        // only allow primed tasks to take input
        if (this.taskState == TaskRunnerState.PRIMED) {
            // start the task. No more input will be accepted
            this.taskArgs = args;
            setTaskState(TaskRunnerState.START);
        }
    }

    /**
     * Processes the 'START' state
     */
    protected void processStateStart() {
        if (this.taskState == TaskRunnerState.START) {

            runningTasks.add(this);
            // state is out of limbo now
            limboTasks.decrementAndGet();

            TaskRunnerListener[] list = listeners.toArray(new TaskRunnerListener[listeners.size()]);

            for (int i = 0; i < list.length; i++) {
                TaskRunnerListener listener = list[i];
                listener.onTaskStart(this);
            }

            if (this.taskArgs.length > 0) {
                // we're now in progress
                // System.out.println("TaskRunner::processStateStart() - state: "
                // + this.taskState + " args: " + this.taskArgs.length);

                setTaskState(TaskRunnerState.ACTIVE);

                Options options = new Options();
                CommandLineParser parser = new DefaultParser();

                try {
                    CLIOptionBuilder.buildApacheOptions(options);

                    CommandLine cmd = parser.parse(options, this.taskArgs);

                    String[] argv = cmd.getArgs();
                    Properties opts = cmd.getOptionProperties("D");

                    // opts.keySet();
                    // System.out.print("Args: ");
                    // for (String arg : argv) {
                    // System.out.print(arg + " ");
                    // }
                    // System.out.print("\nOpts: ");
                    // Set<Object> keys = opts.keySet();
                    // for (Object key : keys) {
                    // System.out.print("[" + key + "]=" +
                    // opts.getProperty(key.toString()) + ",  ");
                    // }

                    // automatically generate the help statement
                    // HelpFormatter formatter = new HelpFormatter();
                    // formatter.printHelp("sample", options);

                } catch (ParseException e) {

                    ErrorUtils.printStackTrace(e);

                    setTaskState(TaskRunnerState.ERROR);
                    this.canRecoverFromError = true;
                    this.errorMessage = "Inputs were malformed, please try again";
                }

            } else {
                setTaskState(TaskRunnerState.ERROR);
                this.errorMessage = "You must provide at least one argument to run a HubCap Task";
                this.canRecoverFromError = true;
            }
        } else {
            System.err.println("Can't Start...Task State is: " + this.taskState);
        }
    }

    /**
     * Processes the IDLE state
     */
    protected void processStateIdle() {
        if (taskState == TaskRunnerState.IDLE) {
            try {
                Thread.sleep(Constants.IDLE_TIME);
            } catch (InterruptedException e) {
                setTaskState(TaskRunnerState.SHUTDOWN);
            }
        }
    }

    private List<Object> aggregateData = Collections.synchronizedList(new ArrayList<Object>());

    protected void aggregateData(Object moreData) {
        if (aggregateData.contains(moreData) == false) {
            aggregateData.add(moreData);
        }
    }

    /**
     * Process some data for the current task. This may not be the only data, so
     * it will be 'aggregated'
     * 
     * @param data
     *            a <code>Object</code> of any type including primitive wrappers
     *            (Boolean, Integer, Float, Double, Long )
     */
    private void processTaskData(Object data) {

        TaskRunnerListener[] list = listeners.toArray(new TaskRunnerListener[listeners.size()]);

        for (int i = 0; i < list.length; i++) {
            TaskRunnerListener listener = list[i];
            listener.onTaskDataReceived(this, data);
        }
    }

    // TODO - DELETE THIS BEFORE SHIPPING!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!! DELETE ME START
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    class FakeTaskData {

        private double data = 0.0D;

        public FakeTaskData() {
        }

        public void setFakeData(double fakeData) {
            this.data = fakeData;
        }

        public double getFakeData() {
            return this.data;
        }
    }

    public static long workTime = Constants.FAKE_WORK_TIME;

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!! DELETE ME END
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    /**
     * Process the ACTIVE state
     */
    protected void processStateActive() {

        if (this.getTaskState() == TaskRunnerState.ACTIVE) {
            // try {
            // System.out.println("TaskRunner::processStateActive() - start task");
            // fake work here

            double random = Math.random();

            // X % of jobs will fail with severe error (to test this case)
            if (random < .005) {
                setTaskState(TaskRunnerState.ERROR);
                this.canRecoverFromError = random > .002;
                this.errorMessage = "Random Severe failure occurred!!!";
            } else {
                long now = new Date().getTime();
                int i = 0;
                int b = 0;
                int deltaMax = (int) (Constants.FREE_TASK_RUNNER_WAIT_TIME + Math.random() * TaskRunner.workTime);

                // TODO - DELETE THIS BEFORE SHIPPING!
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                // !!!! DELETE ME START
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                while ((new Date().getTime()) - now < deltaMax) {
                    double x = 2 * 3 / 4 + 56 * 3.14150 * 5f;

                    ++i;

                    if (i % 200000 == 0) {
                        try {
                            b++;
                            Thread.sleep(Constants.TEST_WAIT_TIME * 4);

                            // Fake some task data here
                            FakeTaskData fdata = new FakeTaskData();
                            fdata.setFakeData(b * i * x);
                            processTaskData(fdata);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                // !!!! DELETE ME END
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                setTaskState(TaskRunnerState.COMPLETE);

                // Nth percentile only
                double multiplier = 1.05;

                // only if we build up
                // if (TaskRunner.activeTaskCount() > runningTasks.size() /
                // multiplier) {
                // System.out.println("Task[" + this.taskId +
                // "] complete, processed: " + i + " items with " + b +
                // " sleep breaks, " + TaskRunner.inactiveTaskCount()
                // + " remaining,  time: " + ((new Date().getTime()) - now) +
                // "ms");
                // }

                // } catch (InterruptedException e) {
                // this.taskState = TaskRunnerState.SHUTDOWN;
                // }
            }
        }
    }

    protected void processStateComplete() {

        if (this.taskState == TaskRunnerState.COMPLETE) {
            runningTasks.remove(this);
            limboTasks.incrementAndGet();
            TaskRunnerListener[] list = listeners.toArray(new TaskRunnerListener[listeners.size()]);
            this.listeners.clear();

            for (int i = 0; i < list.length; i++) {
                TaskRunnerListener listener = list[i];
                listener.onTaskComplete(this, this.aggregateData);
            }

            limboTasks.decrementAndGet();
            TaskRunner.freeTask(this);
            setTaskState(TaskRunnerState.IDLE);
        }
    }

    protected void processStateError() {

        if (this.taskState == TaskRunnerState.ERROR) {

            runningTasks.remove(this);
            limboTasks.incrementAndGet();
            TaskRunnerListener[] list = listeners.toArray(new TaskRunnerListener[listeners.size()]);
            this.listeners.clear();

            for (int i = 0; i < list.length; i++) {
                TaskRunnerListener listener = list[i];
                listener.onTaskError(this, new Exception(this.errorMessage), this.canRecoverFromError);
            }
            limboTasks.decrementAndGet();

            if (this.canRecoverFromError == false) {
                setTaskState(TaskRunnerState.SHUTDOWN);
            } else {
                TaskRunner.freeTask(this);
                setTaskState(TaskRunnerState.IDLE);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        // System.out.println("TaskRunner::Start()");
        while (this.taskState != TaskRunnerState.SHUTDOWN) {

            // process a healthy tick
            tick();

            if (this.taskState == TaskRunnerState.ERROR) {
                // tick once more to process the error
                tick();
                System.err.println("ERROR");
                break;
            }
            try {
                Thread.sleep(Constants.TICK_INTERVAL);
            } catch (InterruptedException e) {
                setTaskState(TaskRunnerState.SHUTDOWN);
                break;
            }
        }
    }

    /**
     * Tick the current thread This could be a no-op depending on the current
     * task state.
     */
    private void tick() {

        // Process the current state
        // NOTE: the current state expectancy
        // is ordered below to be the most performant.
        // i.e. active and idle are hit repeatedly, so they are first,
        // where as error, we rarely expect, so its last.
        switch (this.taskState) {

        // likely often hit cases
            case ACTIVE:
                processStateActive();
                break;

            case IDLE:
                processStateIdle();
                break;

            case START:
                processStateStart();
                break;

            case PRIMED:
                break;

            case COMPLETE:
                processStateComplete();
                break;

            // less likely cases
            case DORMANT:
                break;

            case ERROR:
                processStateError();
                break;

            default:
                // error case!
                setTaskState(TaskRunnerState.SHUTDOWN);
                break;
        }
    }
}
