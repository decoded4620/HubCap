package com.hubcap.task.helpers;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.cli.CommandLine;

import com.hubcap.Constants;
import com.hubcap.lowlevel.SewingMachine;
import com.hubcap.process.ProcessModel;
import com.hubcap.task.TaskRunner;
import com.hubcap.task.helpers.drones.GitHubOrgScavengerDrone;
import com.hubcap.task.model.ScavengerModel;
import com.hubcap.utils.ErrorUtils;
import com.hubcap.utils.ThreadUtils;

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

public class DefaultSearchHelper extends TaskRunnerHelper {

    private AtomicLong searchHelperId = new AtomicLong(0);

    private List<Thread> droneThreads = Collections.synchronizedList(new ArrayList<Thread>());

    private SewingMachine sewingMachine;

    /**
     * CTOR
     * 
     * @param owner
     */
    public DefaultSearchHelper(SewingMachine sewingMachine, TaskRunner owner) {

        super(owner);
        this.sewingMachine = sewingMachine;
    }

    @Override
    public void run() {

        if (taskModel == null) {
            if (this.listener != null) {
                this.listener.processTaskHelperError(new Exception("No Task Model Provided to DefaultSearchHelper, cannot run!"), false);
            }
            die();
            return;
        }

        CommandLine cmd = taskModel.getCommandLine();

        int cLen = cmd.getArgs().length;
        // default search arguments are in the format
        // orgname (String) count (int)

        if (cLen < 1) {
            if (this.listener != null) {
                this.listener.processTaskHelperError(new Exception("Default Search requires 1 argument, Organization (optional) Count"), false);
            }
            die();
        }

        if (cLen % 2 == 0) {

            // each helper has its own HttpClient
            ProcessModel.instance().updateRateLimitData();

            for (int i = 0; i < cLen; i += 2) {

                String orgName = cmd.getArgs()[i];
                int count = 10;

                try {
                    count = Integer.parseInt(cmd.getArgs()[i + 1]);
                } catch (NumberFormatException ex) {
                    ErrorUtils.printStackTrace(ex);
                }

                final long remainingRate = ProcessModel.instance().getRateLimitData().rate.remaining;
                final long maxResults = opts.containsKey("maxResults") ? Integer.parseInt((String) opts.get("maxResults")) : (remainingRate - 1);
                int maxPages = 100;
                if (remainingRate >= maxResults) {

                    // pound that API until we get what we want!!

                    // breaks out of the loop after
                    // max pages
                    searchHelperId.incrementAndGet();

                    if (searchHelperId.get() > maxPages) {
                        break;
                    }

                    try {
                        synchronized (droneThreads) {

                            Thread t = new Thread(new GitHubOrgScavengerDrone(sewingMachine, this.taskModel, orgName, count));
                            droneThreads.add(t);
                            t.setName("drone" + String.valueOf(owner.getTaskId()) + "-" + String.valueOf(new Date().getTime()));
                            t.setDaemon(false);
                            t.start();
                        }
                    } catch (RejectedExecutionException ex) {
                        ErrorUtils.printStackTrace(ex);
                        break;
                    }

                } else {
                    System.err.println("Your rate limit is exhausted, try again later!");
                }
            }
        }

        if (ProcessModel.instance().getVerbose()) {
            System.out.println("Waiting for Drone Threads: " + droneThreads.size());
        }

        // wait for all threads to complete
        while (droneThreads.size() > 0) {
            Iterator<Thread> it = droneThreads.iterator();
            while (it.hasNext()) {
                Thread currDroneThread = it.next();

                if (currDroneThread.getState() == State.TERMINATED) {

                    if (ProcessModel.instance().getVerbose()) {
                        System.err.println("Removing Drone Thread: " + currDroneThread.getName());
                    }

                    it.remove();
                }
            }

            // sleep and do it again
            if (!ThreadUtils.safeSleep(Constants.NEW_THREAD_SPAWN_BREATHING_TIME + Constants.NEW_THREAD_SPAWN_BREATHING_TIME * 1 / SewingMachine.MAX_THREADS_PER_MACHINE,
                    ProcessModel.instance().getVerbose())) {
                System.err.println("INTERRUPTED WAIT FOR DRONE THREADS!");
                break;
            }
        }

        System.out.println("No More Drones!");
        // wait a tad

        synchronized (taskModel) {
            Map<String, Object> aggData = taskModel.getAggregateDataMap();

            if (aggData != null) {

                for (String key : aggData.keySet()) {

                    Object value = aggData.get(key);

                    if (value instanceof ScavengerModel == false) {
                        continue;
                    }

                    // ask the model to calculate from its current state
                    ScavengerModel model = (ScavengerModel) value;
                    synchronized (model) {
                        model.calculate();
                    }
                }

                listener.processTaskHelperData(taskModel);
            }
        }
        die();
    }
}
