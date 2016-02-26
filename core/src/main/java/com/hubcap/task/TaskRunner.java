/**
 * 
 */
package com.hubcap.task;

import java.lang.Thread.State;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.cli.ParseException;

import com.hubcap.Constants;
import com.hubcap.HubCap;
import com.hubcap.inputs.CLIOptionBuilder;
import com.hubcap.lowlevel.ExpressionEval;
import com.hubcap.process.ProcessModel;
import com.hubcap.process.ProcessState;
import com.hubcap.task.helpers.DebugSearchHelper;
import com.hubcap.task.helpers.DeepSearchHelper;
import com.hubcap.task.helpers.DefaultSearchHelper;
import com.hubcap.task.helpers.ScavengerHelper;
import com.hubcap.task.helpers.SearchHelperListener;
import com.hubcap.task.helpers.TaskRunnerHelper;
import com.hubcap.task.state.TaskRunnerState;
import com.hubcap.utils.ErrorUtils;
import com.hubcap.utils.ThreadUtils;

// my access token
// https://github.com/settings/tokens/new
// 129bf1a604dd4c46ce235b8d9d6e1ac261e50c1e
/**
 * @author Decoded4620 2016
 */
public class TaskRunner implements Runnable {

    // at most 10 percent of CPU when basic search is being done.
    // task runner Thread metrics
    public static final float CPU_LOAD_TR = 0.05f;

    public static final float CPU_WAIT_TR = 2f;

    public static final float CPU_COMPUTE_TR = 1f;

    // task runner helper Thread metrics
    public static final float CPU_LOAD_TRH = 0.05f;

    public static final float CPU_WAIT_TRH = 10f;

    public static final float CPU_COMPUTE_TRH = 1f;

    // keeps a sane list of task runners, uses concurrency package
    private static List<TaskRunner> runningTasks = Collections.synchronizedList(new ArrayList<TaskRunner>());

    private static List<TaskRunner> freeTasks = Collections.synchronizedList(new ArrayList<TaskRunner>());

    private static List<TaskRunner> unrecoverableErrorTasks = Collections.synchronizedList(new ArrayList<TaskRunner>());

    private static List<TaskRunner> orphans = Collections.synchronizedList(new ArrayList<TaskRunner>());

    private static List<TaskRunner> limboTasks = Collections.synchronizedList(new ArrayList<TaskRunner>());

    // any arguments passed in when we're busy will be 'enqueued' here
    private static List<String[]> busyArgs = Collections.synchronizedList(new ArrayList<String[]>());

    private static List<TaskRunnerListener[]> busyListeners = Collections.synchronizedList(new ArrayList<TaskRunnerListener[]>());

    // keeps a sane list of task threads, uses concurrency package
    private static List<Thread> taskThreads = Collections.synchronizedList(new ArrayList<Thread>());

    private static ThreadFactory factory = null;

    private static ExecutorService threadPool = null;

    private static Thread monitorThread;

    private static Thread pruningThread;

    private static Thread busyMuncherThread;

    private static boolean isTaskSystemReady = false;

    /**
     * If there are more awaiting requests for tasks...
     * 
     * @param listeners
     */
    public static void getMoreBusyTasks() {

        if (TaskRunner.inactiveTaskCount() == 0) {
            ThreadUtils.waitUntil(new ExpressionEval() {

                @Override
                public Object evaluate() {
                    // TODO Auto-generated method stub
                    return TaskRunner.inactiveTaskCount() > 0;
                }
            }, -1, Constants.IDLE_TIME, ProcessModel.instance().getVerbose());
        }

        if (TaskRunner.inactiveTaskCount() >= Constants.MIN_NOT_BUSY_THREAD_COUNT && TaskRunner.waitingTaskCount() > 0) {

            int i = 0;
            int size = 0;
            Object[] arr = null;
            Object[] larr = null;
            synchronized (TaskRunner.busyArgs) {
                synchronized (TaskRunner.busyListeners) {
                    arr = busyArgs.toArray(new Object[busyArgs.size()]);
                    larr = busyListeners.toArray(new Object[busyListeners.size()]);

                    busyArgs.clear();
                    busyListeners.clear();
                }
            }

            size = arr.length;

            if (size == 0) {
                return;
            }

            // find inactive
            // trim
            if (size > 0) {

                HubCap.instance().setState(ProcessState.RUNNING);

                for (i = 0; i < size; ++i) {
                    HubCap.instance().processArgs((String[]) arr[i], (TaskRunnerListener[]) larr[i]);
                }
            }

            if (i > 0) {
                if (TaskRunner.activeTaskCount() == 0) {
                    // at least one job was added
                    ThreadUtils.waitUntil(new ExpressionEval() {

                        @Override
                        public Object evaluate() {

                            boolean ret = TaskRunner.activeTaskCount() > 0 || HubCap.instance().getState() == ProcessState.SHUTDOWN;

                            if (ret) {
                                if (ProcessModel.instance().getVerbose()) {
                                    System.out.println("Waited successfully for busy tasks to start.");
                                }
                            }

                            return ret;
                        }
                    }, -1, Constants.NEW_THREAD_SPAWN_BREATHING_TIME, ProcessModel.instance().getVerbose());
                }
            }

        }
    }

    private static void runTask(TaskRunner task) {
        if (!runningTasks.contains(task)) {

            // start the task now
            task.setTaskState(TaskRunnerState.START);

            limboTasks.remove(task);
            freeTasks.remove(task);
            runningTasks.add(task);
            // System.out.println("runTask: " + task + ", available: " +
            // freeTasks.size());
        }
    }

    private static void freeTask(TaskRunner task) {
        if (!freeTasks.contains(task)) {

            limboTasks.remove(task);
            runningTasks.remove(task);
            freeTasks.add(task);
            // System.out.println("freeTask: " + task);
        }
    }

    private static void limboTask(TaskRunner task) {

        if (!limboTasks.contains(task)) {
            freeTasks.remove(task);
            runningTasks.remove(task);
            limboTasks.add(task);
            // System.out.println("limbo: " + task);
        }
    }

    private static void unrecoverableTask(TaskRunner task) {
        if (task.getTaskState() == TaskRunnerState.ERROR) {
            if (!unrecoverableErrorTasks.contains(task)) {
                freeTasks.remove(task);
                runningTasks.remove(task);
                limboTasks.remove(task);
                unrecoverableErrorTasks.add(task);
                task.setTaskState(TaskRunnerState.SHUTDOWN);
            }
        }
    }

    public static int countBusyArgs() {
        return busyArgs.size();
    }

    public static void addBusyArgs(String[] args, TaskRunnerListener[] listeners) {
        // push both
        busyArgs.add(args);
        busyListeners.add(listeners);
    }

    /**
     * Returns the next available task runner.
     * 
     * @return a <code>TaskRunner</code>
     */
    public static TaskRunner getNextAvailableRunner() {

        TaskRunner ret = null;

        synchronized (freeTasks) {
            Iterator<TaskRunner> i = freeTasks.iterator();

            while (i.hasNext()) {
                TaskRunner runner = i.next();
                if (runner.getTaskState() == TaskRunnerState.IDLE || runner.getTaskState() == TaskRunnerState.DORMANT) {
                    ret = runner;

                    // track counted task in 'limbo' until we get
                    // it added to the runningTaskList
                    // this way 'totalTasks' calls will be accurate.
                    TaskRunner.limboTask(ret);
                    ret.setTaskState(TaskRunnerState.PRIMED);
                    // prime it now and remove it from the free list
                    return ret;
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

    public static int waitingTaskCount() {

        int act = 0;
        synchronized (runningTasks) {
            synchronized (busyArgs) {
                synchronized (limboTasks) {
                    act = runningTasks.size() + busyArgs.size() + limboTasks.size();
                }
            }
        }

        return act;
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

    public static int limboTaskCount() {
        int act = 0;

        synchronized (limboTasks) {
            act = limboTasks.size();
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
                total = freeTasks.size() + runningTasks.size() + limboTasks.size();
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

            // insure no more threads can be executed.
            threadPool.shutdown();

            Object[] set = {
                runningTasks,
                freeTasks,
                limboTasks
            };

            for (Object item : set) {

                @SuppressWarnings("unchecked")
                List<TaskRunner> list = (List<TaskRunner>) item;

                if (list != null) {
                    synchronized (list) {
                        for (TaskRunner t : list)
                            t.shutdown();
                    }
                }
            }

            synchronized (taskThreads) {
                for (Thread t : taskThreads)
                    if (t.getState() != State.TERMINATED)
                        ThreadUtils.fold(t, false, ProcessModel.instance().getVerbose());

            }

            taskThreads.clear();

            if (monitorThread != null) {
                System.out.println("Killing Monitor thread");
                monitorThread.interrupt();
                monitorThread = null;
            }

            if (pruningThread != null) {
                System.out.println("Killing Pruning thread");
                pruningThread.interrupt();
                pruningThread = null;
            }

            if (busyMuncherThread != null) {
                System.out.println("Killing busy muncher thread");
                busyMuncherThread.interrupt();
                busyMuncherThread = null;
            }

            runningTasks.clear();
            freeTasks.clear();
            limboTasks.clear();

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
                int numThreads = ThreadUtils.getStableThreadCount(CPU_LOAD_TR, CPU_WAIT_TR, CPU_COMPUTE_TR, Constants.MAX_TASK_RUNNER_THREADS);

                System.out.println("creating: " + numThreads + " threads for hubcap");
                TaskRunner.threadPool = Executors.newFixedThreadPool(numThreads, TaskRunner.factory);
                for (int i = 0; i < numThreads; ++i) {
                    TaskRunner tr = new TaskRunner();
                    threadPool.execute(tr);
                }

                // pass the monitoring code to another thread
                // so we don't block the REPL loop
                monitorThread = new Thread(new Runnable() {

                    @Override
                    public void run() {

                        while (!threadPool.isShutdown()) {
                            try {
                                TaskRunner.rebalance();
                                Thread.sleep(Constants.POOL_SHUTDOWN_CHECK_INTERVAL);

                            } catch (InterruptedException ex) {
                                if (ProcessModel.instance().getVerbose()) {
                                    ErrorUtils.printStackTrace(ex);
                                }
                                break;
                            }
                        }

                        System.out.println("Thread Pool was shutdown");

                        while (!threadPool.isTerminated()) {
                            try {
                                Thread.sleep(Constants.POOL_TERM_CHECK_INTERVAL);
                            } catch (InterruptedException ex) {
                                ErrorUtils.printStackTrace(ex);
                                break;
                            }
                        }

                        System.out.println("Thread pool terminated.");
                    }
                });

                monitorThread.setName("TaskMonDaemon");
                monitorThread.setDaemon(false);

                // start monitoring
                monitorThread.start();

                System.out.println("Thread pool started!");
            }
        } else {
            throw new IllegalStateException("Hubcap task runner can only be initialized once!");
        }
    }

    public static void rebalance() {

        // spy on any unrecoverable tasks.
        if (unrecoverableErrorTasks.size() >= Constants.MIN_UNRECOVERABLE_POOL_SIZE && TaskRunner.activeTaskCount() <= Math.floor(Constants.MAX_TASK_RUNNER_THREADS / 10)) {

            if (pruningThread == null) {
                pruningThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        prune();

                        pruningThread = null;
                    }
                });
                synchronized (pruningThread) {
                    pruningThread.setName("__PruningThread");
                    pruningThread.setDaemon(true);
                    pruningThread.start();
                }
            }
        }

        if (busyMuncherThread == null && busyArgs.size() > 0) {
            busyMuncherThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    // pile on any more busy work if available we don't want
                    // any idle threads
                    TaskRunner.getMoreBusyTasks();
                    busyMuncherThread = null;
                }
            });

            synchronized (busyMuncherThread) {
                busyMuncherThread.setName("__BusyMuncher");
                busyMuncherThread.setDaemon(true);
                busyMuncherThread.start();
            }
        }
    }

    private static void prune() {

        TaskRunner[] errors = null;
        errors = unrecoverableErrorTasks.toArray(new TaskRunner[unrecoverableErrorTasks.size()]);
        // sanity
        unrecoverableErrorTasks.clear();

        if (errors.length > 0) {

            for (int x = 0; x < errors.length; ++x) {

                TaskRunner currentRunner = errors[x];

                if (currentRunner == null) {
                    continue;
                }

                // sanity in case the task fails to remove itself for
                // any reason.
                Thread t = currentRunner.getTaskThread();

                if (t != null) {
                    taskThreads.remove(t);
                }

                // remove this from the running list
                TaskRunner newOrOrphanedRunner = null;
                boolean err = false;
                try {
                    // grab an orphan, or make a new runner.
                    newOrOrphanedRunner = orphans.size() > 0 ? orphans.remove(orphans.size() - 1) : new TaskRunner();

                    if (HubCap.instance().getState() != ProcessState.SHUTDOWN) {
                        // runner will add its own thread to the taskthread pool
                        threadPool.execute(newOrOrphanedRunner);
                    }

                } catch (RejectedExecutionException rex) {
                    ErrorUtils.printStackTrace(rex);
                    err = true;
                } catch (NullPointerException nex) {
                    ErrorUtils.printStackTrace(nex);
                    err = true;
                } finally {
                    if (err) {
                        // put it back on the orphan list
                        orphans.add(newOrOrphanedRunner);
                    }
                }
            }
        }
    }

    // uniq id for this task
    private long taskId;

    // The thread running this TaskRunner.
    private Thread myThread;

    private ExecutorService helperPool = null;

    private ThreadFactory helperFactory = null;

    // set of listeners for actions that happen from this Task Runner
    // use concurrent list since listeners may be added from another thread, and
    // since listener callbacks may be accessing this list (i.e. removing
    // themselves from it)
    private List<TaskRunnerListener> listeners = Collections.synchronizedList(new ArrayList<TaskRunnerListener>());

    // set of parallel helper threads that can be used to gather results
    private List<TaskRunnerHelper> helpers = Collections.synchronizedList(new ArrayList<TaskRunnerHelper>());

    private AtomicInteger helperCount = new AtomicInteger(0);

    // the input arguments that were passed to this TaskRunner
    private String[] taskArgs = null;

    // the current task state
    private TaskRunnerState taskState = TaskRunnerState.DORMANT;

    // the current task model
    private TaskModel taskModel = null;

    // the current error message, if any
    private String errorMessage = null;

    // if set false, and we're in an ERROR state
    // this thread will be abandoned and a new one created.
    private boolean canRecoverFromError = true;

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

    public Thread getTaskThread() {
        return myThread;
    }

    public TaskModel getTaskModel() {
        return taskModel;
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

            // run the task now
            TaskRunner.runTask(this);
        }
    }

    /**
     * Processes the 'START' state
     */
    protected void processStateStart() {

        if (this.taskState == TaskRunnerState.START) {

            if (helperPool == null) {

                // get a stable thread count up to MAX_HELPER_THREADS based on
                // CPU LOAD per helper
                int maxHelpers = ThreadUtils.getStableThreadCount(CPU_LOAD_TRH, CPU_WAIT_TRH, CPU_COMPUTE_TRH, Constants.MAX_HELPER_THREADS);

                if (this.helperFactory == null) {
                    this.helperFactory = new ThreadFactory() {

                        @Override
                        public Thread newThread(Runnable r) {
                            if (helpers.contains(r)) {
                                throw new IllegalStateException("Cannot add duplicate runnable to running tasks");
                            }

                            Thread thread = new Thread(r);
                            thread.setDaemon(false);
                            thread.setName("TaskRunnerHelper-" + helperCount.getAndIncrement());
                            taskThreads.add(thread);
                            return thread;
                        }
                    };
                }

                // create the pool but don't fire up any helpers
                helperPool = Executors.newFixedThreadPool(maxHelpers, TaskRunner.factory);
            }

            TaskRunnerListener[] list = listeners.toArray(new TaskRunnerListener[listeners.size()]);

            for (int i = 0; i < list.length; i++) {
                TaskRunnerListener listener = list[i];
                listener.onTaskStart(this);
            }

            try {
                // create the task model using the input arguments
                setTaskState(TaskRunnerState.ACTIVE);

                this.taskModel = CLIOptionBuilder.buildInputOptionsModel(this.taskArgs);

            } catch (ParseException e) {

                ErrorUtils.printStackTrace(e);
                setTaskState(TaskRunnerState.ERROR);
                this.canRecoverFromError = true;
                this.errorMessage = "Inputs were malformed, please try again";
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
        } else {
            System.err.println("cannot processIdle if state is not IDLE.");
        }
    }

    /**
     * Spawns a TaskRunnerHelper which performs some task in aggregation of the
     * data we want. Sometimes there is only a single helper. But for scavenger
     * mode, etc there can be many. The helperPool supports multiple for that
     * reason.
     * 
     * @param helper
     */
    protected TaskRunnerHelper spawnHelper(Class<? extends TaskRunnerHelper> clazz, SearchHelperListener listener) {
        try {

            Constructor<?> c = clazz.getConstructors()[0];
            TaskRunnerHelper helper = (TaskRunnerHelper) c.newInstance(this);
            try {

                helperPool.execute(helper);
                this.helpers.add(helper);
                helper.setListener(listener);
                return helper;
            } catch (RejectedExecutionException ex) {
                ErrorUtils.printStackTrace(ex);
            }
        } catch (IllegalAccessException ex) {
            ErrorUtils.printStackTrace(ex);
        } catch (InstantiationException ex) {
            ErrorUtils.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            ErrorUtils.printStackTrace(ex);
        }

        return null;
    }

    /**
     * Process the ACTIVE state
     */
    protected void processStateActive() {

        if (this.getTaskState() == TaskRunnerState.ACTIVE) {

            if (taskModel != null) {
                // the task mode
                TaskRunnerHelper helper = null;

                // default listener
                SearchHelperListener listener = new SearchHelperListener() {

                    @Override
                    public void processTaskHelperError(Exception ex, boolean canRecover) {
                        // TODO Auto-generated method stub
                        TaskRunner.this.setTaskState(TaskRunnerState.ERROR);

                        errorMessage = ErrorUtils.getStackTrace(ex);
                        canRecoverFromError = canRecover;

                        TaskRunner.this.setTaskState(TaskRunnerState.ERROR);

                        if (ProcessModel.instance().getVerbose()) {
                            ErrorUtils.printStackTrace(ex);
                        }
                    }

                    @Override
                    public void processTaskHelperData(Object taskData) {
                        processTaskData(taskData);

                    }

                    @Override
                    public void processTaskHelperDataForKey(String key, Object taskData) {
                        processTaskDataForKey(key, taskData);
                    }
                };

                switch (taskModel.getTaskMode()) {
                    case SEARCH:
                        helper = this.spawnHelper(DefaultSearchHelper.class, listener);
                        break;
                    case DEEP_SEARCH:
                        helper = this.spawnHelper(DeepSearchHelper.class, listener);
                        break;
                    case SCAVENGER:
                        helper = this.spawnHelper(ScavengerHelper.class, listener);
                    case DEBUG: {

                        // special debug listener
                        SearchHelperListener debugListener = new SearchHelperListener() {

                            @Override
                            public void processTaskHelperError(Exception ex, boolean canRecover) {
                                // TODO Auto-generated method stub
                                TaskRunner.this.setTaskState(TaskRunnerState.ERROR);

                                errorMessage =
                                        canRecover ? "A minor error happened, task will reset, results may be unavailable."
                                                : "A severe failure happened, task thread unrecoverable.";
                                TaskRunner.this.setTaskState(TaskRunnerState.ERROR);

                                System.err.println("Process Task Helper Error");
                                if (ProcessModel.instance().getVerbose()) {
                                    ErrorUtils.printStackTrace(ex);
                                }
                            }

                            @Override
                            public void processTaskHelperData(Object taskData) {
                                processTaskData(taskData);

                            }

                            @Override
                            public void processTaskHelperDataForKey(String key, Object taskData) {
                                processTaskDataForKey(key, taskData);
                            }
                        };
                        helper = this.spawnHelper(DebugSearchHelper.class, debugListener);
                        break;
                    }
                    default:
                        break;
                }
            }

            this.helperPool.shutdown();

            while (!this.helperPool.isTerminated()) {
                if (!ThreadUtils.safeSleep(Constants.IDLE_TIME, ProcessModel.instance().getVerbose())) {
                    this.helperPool.shutdownNow();
                    break;
                }
            }

            // only set idle if we weren't shutdown
            if (this.getTaskState() != TaskRunnerState.SHUTDOWN && getTaskState() != TaskRunnerState.ERROR) {
                setTaskState(TaskRunnerState.COMPLETE);
            }
        }

    }

    protected void processStateComplete() {
        if (this.taskState == TaskRunnerState.COMPLETE) {
            TaskRunnerListener[] list = listeners.toArray(new TaskRunnerListener[listeners.size()]);
            this.listeners.clear();

            int lLen = list.length;
            for (int i = 0; i < lLen; i++) {
                TaskRunnerListener listener = list[i];
                listener.onTaskComplete(this);
            }

            // clear any data so we don't leak
            taskModel = null;

            // only set idle if we weren't shutdown
            if (this.getTaskState() != TaskRunnerState.SHUTDOWN && this.getTaskState() != TaskRunnerState.ERROR) {
                setTaskState(TaskRunnerState.IDLE);
            }

            // free at the very end
            TaskRunner.freeTask(this);
        }
    }

    protected void processStateShutdown() {
        TaskRunner.freeTask(this);
    }

    protected void processStateError() {

        if (this.taskState == TaskRunnerState.ERROR) {

            TaskRunnerListener[] list = listeners.toArray(new TaskRunnerListener[listeners.size()]);
            this.listeners.clear();

            for (int i = 0; i < list.length; i++) {
                TaskRunnerListener listener = list[i];
                if (listener != null) {
                    listener.onTaskError(this, new Exception(this.errorMessage), this.canRecoverFromError);
                }
            }

            // clear any data so we don't leak
            this.taskModel = null;

            if (this.canRecoverFromError == true) {
                // only set idle if we weren't shutdown
                TaskRunner.freeTask(this);

                if (this.getTaskState() != TaskRunnerState.SHUTDOWN) {
                    setTaskState(TaskRunnerState.IDLE);
                }
            } else {
                TaskRunner.unrecoverableTask(this);
            }
        } else {
            System.err.println(getTaskId() + "::processStateError() FAIL! Cannot process error state, state is: " + this.taskState);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        myThread = Thread.currentThread();

        if (taskThreads.contains(myThread) == false) {
            taskThreads.add(myThread);
        }

        // sanity
        TaskRunner.freeTask(this);

        while (this.taskState != TaskRunnerState.SHUTDOWN) {

            // process a healthy tick
            tick();

            if (!ThreadUtils.safeSleep(Constants.TICK_INTERVAL, ProcessModel.instance().getVerbose())) {

                if (this.taskState == TaskRunnerState.ACTIVE) {

                    this.setTaskState(TaskRunnerState.SHUTDOWN);

                    if (ProcessModel.instance().getVerbose()) {
                        System.err.println("Shutting down active task " + getTaskId() + "!!!!");
                    }
                } else {
                    if (ProcessModel.instance().getVerbose()) {
                        System.out.println("Task " + getTaskId() + " Shutdown normally, state was: " + this.taskState);
                    }
                }

                break;
            }
        }

        // flush out any state.
        tick();

        if (taskThreads.contains(myThread)) {
            taskThreads.remove(myThread);
        }

        if (myThread != ThreadUtils.getMainThread()) {
            ThreadUtils.fold(myThread, false, ProcessModel.instance().getVerbose());
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

            case SHUTDOWN:
                processStateShutdown();
                break;

            default:
                System.err.println("Unknown State Not Supported! " + this.taskState);
                // error case!
                setTaskState(TaskRunnerState.SHUTDOWN);
                break;
        }
    }

    private void processTaskDataForKey(String key, Object data) {
        taskModel.aggregateForKey(key, data);
        TaskRunnerListener[] list = listeners.toArray(new TaskRunnerListener[listeners.size()]);

        for (int i = 0; i < list.length; i++) {
            TaskRunnerListener listener = list[i];
            listener.onTaskDataReceived(this);
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

        taskModel.aggregate(data);
        TaskRunnerListener[] list = listeners.toArray(new TaskRunnerListener[listeners.size()]);

        for (int i = 0; i < list.length; i++) {
            TaskRunnerListener listener = list[i];
            listener.onTaskDataReceived(this);
        }
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

}
