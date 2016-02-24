package com.hubcap;

import java.util.Date;

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

import com.hubcap.task.TaskRunner;
import com.hubcap.task.TaskRunnerListener;
import com.hubcap.task.state.TaskRunnerState;
import com.hubcap.utils.ErrorUtils;

public class HubCapTest {

    private static TaskRunnerListener listener = null;

    @BeforeClass
    public static void setup() {
        System.out.println("------------------------------------------------------------------SETUP");

        listener = new TaskRunnerListener() {

            @Override
            public void onTaskStateChange(TaskRunner runner, TaskRunnerState state) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTaskStart(TaskRunner runner) {
                // TODO Auto-generated method stub
                // System.err.println("onTaskStart(" + runner.getTaskId() +
                // ")");
            }

            @Override
            public void onTaskError(TaskRunner runner, Exception e, boolean canRecoverFromError) {
                // TODO Auto-generated method stub
                System.err.println("onTaskError(" + runner.getTaskId() + ", " + e.toString() + ", can recover? " + canRecoverFromError);
            }

            @Override
            public void onTaskDataReceived(TaskRunner runner, Object taskData) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTaskComplete(TaskRunner runner, Object aggregatedResults) {
                // TODO Auto-generated method stub
                // System.err.println("onTaskComplete(" + runner.getTaskId() +
                // ", Result Type" + aggregatedResults.getClass().getName());
            }
        };

        HubCap.instance();
        Assert.assertEquals(true, true);
    }

    @Test
    public void test0() {
        System.out.println("TEST DEFAULT");
        System.out.println("--------------------------------------------------");
        String[] args = {
            "decoded4620",
            "10"
        };

        boolean caught = false;
        try {
            HubCap.main(args);
        } catch (Exception e) {
            caught = true;
        }

        long now = (new Date().getTime());
        // wait a bit for tasks to start
        while (TaskRunner.activeTaskCount() == 0) {
            if (!ErrorUtils.safeSleep(Constants.MINI_TIME) || (new Date().getTime()) - now > 1000) {
                break;
            }
        }

        Assert.assertEquals(caught, false);
        System.out.println("Task Count: " + TaskRunner.activeTaskCount());
        while (TaskRunner.activeTaskCount() > 0 && ErrorUtils.safeSleep(Constants.FREE_TASK_RUNNER_WAIT_TIME)) {
            System.out.println("Awaiting " + TaskRunner.activeTaskCount() + " tasks to complete");
        }
    }

    @Test
    public void test1() {

        try {
            System.out.println("TEST MANY");
            System.out.println("--------------------------------------------------");
            HubCap hub = HubCap.instance();
            // start a crap ton of threads

            for (int i = 0; i < Constants.MAX_THREADS; i++) {
                hub.processArgs(hub.repl().processREPLInput("-w -Dkey=value -Dkey2=value2 \"C:\\Program Files\\Notepad++\\Notepad.exe\" -Dkey3=\"value three\""), listener);
            }
            long now = (new Date().getTime());
            // wait a bit for tasks to start
            while (TaskRunner.activeTaskCount() == 0) {
                if (!ErrorUtils.safeSleep(Constants.IDLE_TIME) || (new Date().getTime()) - now > 1000) {
                    break;
                }
            }

            System.out.println("Task Count: " + TaskRunner.activeTaskCount());

            // continue to wait until all tasks have completed
            while (TaskRunner.activeTaskCount() > 0 && ErrorUtils.safeSleep(Constants.FREE_TASK_RUNNER_WAIT_TIME)) {
                System.out.println("Awaiting " + TaskRunner.activeTaskCount() + " tasks to complete...");
            }
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

            int multiplier = 6;

            TaskRunner.workTime = Constants.FAKE_WORK_TIME_HEAVY;

            for (int j = 0; j < multiplier; ++j) {

                for (int i = 0; i < Constants.MAX_THREADS / 8; i++) {

                    hub.processArgs(hub.repl().processREPLInput("-w -Dkey=value -Dkey2=value2 \"C:\\Program Files\\Notepad++\\Notepad.exe\" -Dkey3=\"value three\""), listener);
                    hub.processArgs(hub.repl().processREPLInput("123 xyz -Dkey=value -w"), listener);
                    hub.processArgs(hub.repl().processREPLInput("-w \"C:\\Program Files\\Notepad++\\Notepad.exe\" -Dkey=\"quoted value!\""), listener);
                    hub.processArgs(hub.repl().processREPLInput("3333 4444 OOOO MMM GGGG -wf x"), listener);
                }

                if (!ErrorUtils.safeSleep(Constants.MINI_TIME)) {
                    break;
                }

                System.out.println("Active now: " + TaskRunner.activeTaskCount());

                if (!ErrorUtils.safeSleep(Constants.MINI_TIME)) {
                    break;
                }
            }
            long now = (new Date().getTime());
            // wait a bit for tasks to start
            while (TaskRunner.activeTaskCount() == 0) {
                if (!ErrorUtils.safeSleep(Constants.IDLE_TIME) || (new Date().getTime()) - now > Constants.TICK_INTERVAL * 10) {
                    break;
                }
            }

            // continue to wait until all tasks have completed
            while (ErrorUtils.safeSleep(Constants.FREE_TASK_RUNNER_WAIT_TIME) && TaskRunner.activeTaskCount() > 0) {
                System.out.println("waiting for tasks: " + TaskRunner.activeTaskCount());
            }

            System.out.println("all tasks complete!");

        } catch (Exception e) {
            ErrorUtils.printStackTrace(e);
        }
    }

    @AfterClass
    public static void tearDown() {
        System.out.println("------------------------------------------------------------------TEAR DOWN START");

        HubCap hub = HubCap.instance();
        // attempt to exit
        hub.processArgs(hub.repl().processREPLInput("exit"));

        long now = (new Date().getTime());

        // wait a bit for tasks to start
        while (TaskRunner.activeTaskCount() == 0) {

            if (!ErrorUtils.safeSleep(100) || (new Date().getTime()) - now > 1000) {
                break;
            }
        }

        System.out.println("Task Count: " + TaskRunner.activeTaskCount());
        // give it a chance to shutdown
        while (TaskRunner.activeTaskCount() > 0 && ErrorUtils.safeSleep(Constants.FREE_TASK_RUNNER_WAIT_TIME)) {
            System.out.println("Awaiting " + TaskRunner.activeTaskCount() + " tasks to complete");
        }

        System.out.println("------------------------------------------------------------------TEAR DOWN END");

        ErrorUtils.safeSleep(5000);

        System.out.println("Exit.");
    }
}
