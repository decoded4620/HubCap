package com.hubcap;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

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

import org.junit.Test;

import com.hubcap.lowlevel.ExpressionEval;
import com.hubcap.process.ProcessModel;
import com.hubcap.task.REPL;
import com.hubcap.task.TaskRunner;
import com.hubcap.task.TaskRunnerListener;
import com.hubcap.task.helpers.DebugSearchHelper;
import com.hubcap.task.state.TaskRunnerState;
import com.hubcap.utils.ErrorUtils;
import com.hubcap.utils.ThreadUtils;

public class HubCapTest {

    @BeforeClass
    public static void setup() {
        int i = 5;
        System.out.println("------------------------------------------------------------------SETUP");
        System.out.println("Please Take a " + i + " seconds to start your JMX Console or Profiling tools");

        while (--i >= 0) {
            System.out.print(i + ".. ");
            ThreadUtils.safeSleep(1000, ProcessModel.instance().getVerbose());
        }

        // create HubCap
        HubCap.instance();
        // HubCap.instance().processArgs(REPL.processREPLInput("auth decoded4620 129bf1a604dd4c46ce235b8d9d6e1ac261e50c1e"));
        HubCap.instance().processArgs(REPL.processREPLInput("auth theWither2468 364Apples"));
    }

    @Test
    public void test0() {
        System.out.println("TEST DEFAULT");
        System.out.println("--------------------------------------------------");
        String[] args = {
            "edgecase",
            "3"
        };

        boolean caught = false;
        try {
            HubCap.main(args);
        } catch (Exception e) {
            caught = true;
        }

        // wait indefinitely until active task count is 0
        ThreadUtils.napUntil(new ExpressionEval() {

            @Override
            public Object evaluate() {
                return TaskRunner.activeTaskCount() > 0;
            }
        }, -1, Constants.IDLE_TIME, ProcessModel.instance().getVerbose());

        Assert.assertEquals(caught, false);

        // wait indefinitely until active task count is 0
        ThreadUtils.napUntil(new ExpressionEval() {

            @Override
            public Object evaluate() {
                if (TaskRunner.waitingTaskCount() < 20) {
                    System.out.println("waiting for: " + TaskRunner.waitingTaskCount());
                }
                return TaskRunner.waitingTaskCount() == 0;
            }
        }, -1, Constants.TASK_RUN_STOP_WAIT_TIME_MS, ProcessModel.instance().getVerbose());

        System.out.println("pass!");
    }

    class TestListener implements TaskRunnerListener {

        boolean stch = false;

        boolean st = false;

        boolean err = false;

        boolean dr = false;

        boolean cmp = false;

        boolean c = false;

        @Override
        public void onTaskStateChange(TaskRunner runner, TaskRunnerState state) {
            stch = true;
        }

        @Override
        public void onTaskStart(TaskRunner runner) {
            st = true;
        }

        @Override
        public void onTaskError(TaskRunner runner, Exception e, boolean canRecoverFromError) {
            err = false;
        }

        @Override
        public void onTaskDataReceived(TaskRunner runner) {
            dr = true;
        }

        @Override
        public void onTaskComplete(TaskRunner runner) {
            cmp = true;
        }

        public void checkLifecylce() {
            Assert.assertTrue(stch);
            Assert.assertTrue(st);
            Assert.assertTrue(dr);
            Assert.assertTrue(cmp);
            Assert.assertFalse(err);
        }
    }

    @Test
    public void test1() {

        try {
            System.out.println("TEST MULTIPLE");
            System.out.println("--------------------------------------------------");
            HubCap hub = HubCap.instance();
            // start a crap ton of threads

            TestListener testListener = new TestListener();

            hub.processArgs(REPL.processREPLInput("edgecase 4 railslove 4"), testListener);
            // wait indefinitely until active task count is 0
            ThreadUtils.napUntil(new ExpressionEval() {

                @Override
                public Object evaluate() {
                    return TaskRunner.activeTaskCount() > 0;
                }
            }, -1, Constants.IDLE_TIME, ProcessModel.instance().getVerbose());

            // wait indefinitely until active task count is 0
            ThreadUtils.napUntil(new ExpressionEval() {

                @Override
                public Object evaluate() {
                    if (TaskRunner.waitingTaskCount() < 20) {
                        System.out.println("waiting for: " + TaskRunner.waitingTaskCount());
                    }
                    return TaskRunner.waitingTaskCount() == 0;
                }
            }, -1, Constants.TASK_RUN_STOP_WAIT_TIME_MS, ProcessModel.instance().getVerbose());

            testListener.checkLifecylce();

            System.out.println("pass!");

        } catch (Exception e) {
            ErrorUtils.printStackTrace(e);
        }
    }

    @Test
    public void test2() {

        try {
            System.out.println("TEST OVERLOAD");
            System.out.println("--------------------------------------------------");
            HubCap hub = HubCap.instance();
            // start a crap ton of threads

            DebugSearchHelper.debugWorkTime = Constants.FAKE_WORK_TIME_HEAVY;
            DebugSearchHelper.debug_errorChance = 0.025;

            // run several one offs
            hub.processArgs(REPL.processREPLInput("angular 10 jquery 10 sevenwire 10 wrenchlabs 10 railslove 10 netguru 10 NanoHttpd 10 trabian 10 UntoThisLast 10"));

            // wait for at most X milliseconds for active task count to be more
            // than 0
            ThreadUtils.napUntil(new ExpressionEval() {

                @Override
                public Object evaluate() {
                    return TaskRunner.activeTaskCount() > 0;
                }
            }, -1, Constants.IDLE_TIME, ProcessModel.instance().getVerbose());

            // sleep loop indefinitely until active task count is 0
            ThreadUtils.napUntil(new ExpressionEval() {

                @Override
                public Object evaluate() {
                    if (TaskRunner.waitingTaskCount() < Math.ceil(Constants.MAX_TASK_RUNNER_THREADS / 2)) {
                        System.out.println("waiting for: " + TaskRunner.waitingTaskCount());
                    }
                    return TaskRunner.waitingTaskCount() == 0;
                }
            }, -1, Constants.TASK_RUN_STOP_WAIT_TIME_MS, ProcessModel.instance().getVerbose());

            System.out.println("pass!");

        } catch (Exception e) {
            ErrorUtils.printStackTrace(e);
        }
    }

    @AfterClass
    public static void tearDown() {
        System.out.println("------------------------------------------------------------------TEAR DOWN START");

        HubCap hub = HubCap.instance();
        // attempt to exit

        while (TaskRunner.activeTaskCount() > 0) {
            ThreadUtils.safeSleep(1000, true);
        }
        hub.processArgs(REPL.processREPLInput("exit"));

        // wait for at most X milliseconds for active task count to be > 0
        ThreadUtils.napUntil(new ExpressionEval() {

            @Override
            public Object evaluate() {
                return TaskRunner.activeTaskCount() > 0;
            }
        }, -1, Constants.IDLE_TIME, ProcessModel.instance().getVerbose());

        // wait for at most X milliseconds for active task count to be > 0
        ThreadUtils.napUntil(new ExpressionEval() {

            @Override
            public Object evaluate() {
                if (TaskRunner.waitingTaskCount() < 100) {
                    System.out.println("Awaiting " + TaskRunner.waitingTaskCount() + " tasks to complete");
                }
                return TaskRunner.waitingTaskCount() == 0;
            }
        }, -1, Constants.TASK_RUN_STOP_WAIT_TIME_MS, ProcessModel.instance().getVerbose());

        System.out.println("------------------------------------------------------------------TEAR DOWN END");

        // garbage collection
        System.gc();
        System.runFinalization();

        ThreadUtils.safeSleep(5000, ProcessModel.instance().getVerbose());

        System.out.println("Exit.");
    }
}
