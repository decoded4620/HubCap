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

    // =============================================================================
    // HubCap Configuration Constants, Limits, Measures, and Values.
    /**
     * The Maximum number of concurrent HubCap Tasks that can be running while
     * this tool is open. Changing this to a larger number will likely affect
     * performance when running multiple Scavenge mode jos.
     */
    public static final int MAX_THREADS = 1000;

    /**
     * The amount of time to wait for a free Task Runner to process a new
     * command before timing out the request completely. This is generous to
     * insure we don't time out on big tasks.
     */
    public static final int CMD_TIMEOUT_MS = 100;

    /**
     * The number of milliseconds that HubCap will sleep if there are no free
     * Task Runners before it searches again.
     */
    public static final int FREE_TASK_RUNNER_WAIT_TIME = 64;

    /**
     * Number milliseconds between 'tick' calls. (17 ms = 60 frames)
     */
    public static final int TICK_INTERVAL = 17;

    /**
     * Amount of time to wait between checks for a shutdown status.
     */
    public static final int POOL_SHUTDOWN_CHECK_INTERVAL = 128;

    public static final int POOL_TERM_CHECK_INTERVAL = 128;

    /**
     * Once BUSY state is set, this many threads must complete before a not-busy
     * state will be restored.
     */
    public static final int MIN_NOT_BUSY_THREAD_COUNT = MAX_THREADS / 3;

    /**
     * Allows the thread to sleep most of the time during 'idle', but respond
     * quickly to be available for a new request
     */
    public static final int IDLE_TIME = 50;

    public static final int MINI_TIME = 10;

    // Exit synonyms
    public static final String CMD_EXIT = "exit";

    public static final String CMD_QUIT = "quit";

    public static final String CMD_DIE = "die";

    public static final String CMD_BYE = "bye";

    // =====================================================
    // Testing Constants

    public static final int TEST_WAIT_TIME = 50;

    /**
     * Fake work load, possibly heavy up to five seconds
     */
    public static final int FAKE_WORK_TIME = 500;

    public static final int FAKE_WORK_TIME_HEAVY = 2500;

}
