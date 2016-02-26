package com.hubcap.task.helpers;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.cli.CommandLine;

import com.google.gson.Gson;
import com.hubcap.Constants;
import com.hubcap.lowlevel.HttpClient;
import com.hubcap.lowlevel.ParsedHttpResponse;
import com.hubcap.process.ProcessModel;
import com.hubcap.task.TaskRunner;
import com.hubcap.task.helpers.drones.AggregatorDrone;
import com.hubcap.task.model.AggregatorModel;
import com.hubcap.task.model.GitHubRateLimit;
import com.hubcap.task.model.GitHubRepo;
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

    /**
     * CTOR
     * 
     * @param owner
     */
    public DefaultSearchHelper(TaskRunner owner) {

        super(owner);

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
            HttpClient client = new HttpClient();

            synchronized (ProcessModel.instance()) {
                String rlUrl = "https://api.github.com/rate_limit";
                String un = taskModel.getUserName();
                String pwd = taskModel.getPasswordOrToken();
                try {
                    ParsedHttpResponse data = client.getAuthorizedRequest(rlUrl, un, pwd, null);
                    Gson gson = new Gson();
                    GitHubRateLimit rateLimit = gson.fromJson(data.getContent(), GitHubRateLimit.class);

                    ProcessModel.instance().setRateLimitData(rateLimit);
                } catch (IOException ex) {
                    ErrorUtils.printStackTrace(ex);
                }
            }

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
                            Thread t = new Thread(new AggregatorDrone(this.taskModel, orgName, count));
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

            while (droneThreads.size() > 0) {
                Iterator<Thread> it = droneThreads.iterator();
                while (it.hasNext()) {
                    Thread currDroneThread = it.next();

                    if (currDroneThread.getState() == State.TERMINATED) {
                        it.remove();
                    }
                }

                // sleep and do it again
                if (!ThreadUtils.safeSleep(Constants.IDLE_TIME, false)) {
                    break;
                }
            }
        }

        // wait a tad

        synchronized (taskModel) {
            Map<String, Object> aggData = taskModel.getAggregateDataMap();

            if (aggData != null) {

                for (String key : aggData.keySet()) {

                    Object value = aggData.get(key);

                    if (value instanceof AggregatorModel == false) {
                        continue;
                    }

                    // ask the model to calculate from its current state
                    AggregatorModel model = (AggregatorModel) value;
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
