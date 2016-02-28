package com.hubcap;

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

/**
 * Global Constants for everyone to share. These constants help keep hard coded
 * magic values that arent' suitable for configuration files out of the general
 * code base. Add more constants here when needed, group by relative similarity
 * or relationship!
 * 
 * @author Decoded4620 2016
 */
public class Constants {

    public static final String JAVA_D = "D";

    // =============================================================================
    // HubCap Configuration Constants, Limits, Measures, and Values.

    /**
     * Maximum number of HTTP Requests that should be assigned to any thread
     * this will avoid long thread hang times and allow more threads to cycle
     * and be free
     */
    public static final int MAX_HTTP_REQUESTS_PER_THREAD = 4;

    /**
     * The Maximum number of concurrent HubCap Tasks that can be running while
     * this tool is open. Changing this to a larger number will likely affect
     * performance when running multiple Scavenge mode jos.
     */
    public static final int MAX_TASK_RUNNER_THREADS = 20;

    /**
     * The maximum number of concurrent helper threads that can run within a
     * single task runner.
     */
    public static final int MAX_HELPER_THREADS = 500;

    /**
     * Aggregating is a funny business. give it some room
     */
    public static final int MAX_AGGREGATOR_THREADS = 500;

    /**
     * The number of milliseconds that a task runner can run without producing
     * results prior to abandoning its own task. Task Runners are self
     * monitoring so they know how long they've been running, etc.
     */
    public static final int CMD_TIMEOUT_MS = 5000;

    public static final int TASK_RUN_STOP_WAIT_TIME_MS = 1000;

    /**
     * The number of milliseconds that HubCap will sleep if there are no free
     * Task Runners before it searches again.
     */
    public static final int FREE_TASK_RUNNER_WAIT_TIME = 64;

    public static final int NEW_THREAD_SPAWN_BREATHING_TIME = 100;

    /**
     * Number milliseconds between 'tick' calls. (17 ms = 60 frames)
     */
    public static final int TICK_INTERVAL = 17;

    /**
     * Amount of time to wait between checks for a shutdown status.
     */
    public static final int POOL_SHUTDOWN_CHECK_INTERVAL = 400;

    public static final int POOL_TERM_CHECK_INTERVAL = 128;

    /**
     * Once BUSY state is set, this many threads must complete before a not-busy
     * state will be restored.
     */
    public static final int MIN_NOT_BUSY_THREAD_COUNT = (int) Math.ceil(MAX_TASK_RUNNER_THREADS * .25);

    /**
     * Allows the thread to sleep most of the time during 'idle', but respond
     * quickly to be available for a new request
     */
    public static final int IDLE_TIME = 50;

    public static final int MINI_TIME = 10;

    // if we start hitting
    public static final int PRUNE_BOUNDS_LOW = (int) Math.floor(MAX_TASK_RUNNER_THREADS * .05);

    public static final int PRUNE_BOUNDS_HIGH = (int) Math.ceil(MAX_TASK_RUNNER_THREADS * .75);

    // at least 10% of all threads must be unrecoverable before we start the
    // pruner.
    public static final int MIN_UNRECOVERABLE_POOL_SIZE = (int) Math.max(1, Math.floor(MAX_TASK_RUNNER_THREADS * .025));

    // Exit synonyms
    public static final String CMD_EXIT = "exit";

    public static final String CMD_QUIT = "quit";

    public static final String CMD_DIE = "die";

    public static final String CMD_BYE = "bye";

    // authorization
    public static final String CMD_AUTH = "auth";

    // GitHub
    public static final String GITHUB_API_URL = "https://api.github.com";

    // =====================================================
    // Testing Constants

    public static final int TEST_WAIT_TIME = 50;

    /**
     * Fake work load, possibly heavy up to five seconds
     */
    public static final int FAKE_WORK_TIME = 500;

    public static final int FAKE_WORK_TIME_HEAVY = 1500;

}
