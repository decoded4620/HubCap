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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.hubcap.Constants;
import com.hubcap.utils.ErrorUtils;
import com.hubcap.utils.ThreadUtils;

/**
 * Sewing Machine Thread Spool. Governed by Sewing Machine to process sewing
 * machin Tasks. (ThreadSpoolTask objects)
 * 
 * @author Decoded4620 2016
 */
public class ThreadSpool implements Runnable {

    public enum ThreadSpoolState {
        WORKING,
        DORMANT,
        SHUTDOWN
    }

    private SewingMachine owner;

    public ThreadSpoolState state = ThreadSpoolState.DORMANT;

    public static boolean verbose = false;

    // NOTE: Java only
    private List<ThreadSpoolTask> taskSet = null;

    private String id;

    public static AtomicInteger activeSpools = new AtomicInteger(0);

    public ThreadSpool(SewingMachine owner, List<ThreadSpoolTask> taskSet, String id) {
        this.taskSet = taskSet;
        this.id = id;
        this.owner = owner;
        if (verbose) {
            System.out.println(id + "::Construct(sharing: " + taskSet.size() + " tasks, " + id + ")");
        }
    }

    @Override
    public void run() {

        state = ThreadSpoolState.DORMANT;
        while (true) {
            if (taskSet != null) {
                ThreadSpoolTask t = null;

                synchronized (taskSet) {
                    Iterator<ThreadSpoolTask> it = taskSet.iterator();
                    if (it.hasNext()) {
                        // next
                        t = it.next();
                        it.remove();
                    }
                }

                if (t != null) {

                    state = ThreadSpoolState.WORKING;
                    activeSpools.incrementAndGet();
                    try {
                        t.perform();
                    } catch (Exception e) {
                        ErrorUtils.printStackTrace(e);
                    }
                    activeSpools.decrementAndGet();
                    state = ThreadSpoolState.DORMANT;
                }
            }
            // if interrupted
            if (!ThreadUtils.safeSleep(Constants.IDLE_TIME, false)) {
                System.err.println(id + "  -- ThreadSpool Task is interrupted - active spools: " + String.valueOf(activeSpools.get()) + " remain");
                break;
            }

            if ((owner.shuttingDown() || state == ThreadSpoolState.SHUTDOWN) && taskSet.size() == 0) {
                break;
            }
        }

        if (verbose && activeSpools.get() > 0) {
            System.out.println(id + "::shutdown() - " + String.valueOf(activeSpools.get()) + " active spools remain");
        }
    }
}
