package com.hubcap.task.helpers.drones;

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

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hubcap.Constants;
import com.hubcap.lowlevel.HttpClient;
import com.hubcap.lowlevel.ParsedHttpResponse;
import com.hubcap.process.ProcessModel;
import com.hubcap.task.TaskModel;
import com.hubcap.task.model.AggregatorModel;
import com.hubcap.task.model.GitHubPull;
import com.hubcap.task.model.GitHubPullData;
import com.hubcap.task.model.GitHubRepo;
import com.hubcap.utils.ThreadUtils;

public class AggregatorDrone implements Runnable {

    // model data for THIS aggregator only
    private AggregatorModel aggregatorModel;

    private TaskModel taskModel;

    private String orgName;

    private HttpClient client = new HttpClient();

    private Thread myThread;

    /**
     * CTOR
     * 
     * @param model
     * @param org
     * @param count
     */
    public AggregatorDrone(TaskModel model, String org, int count) {

        System.out.println("AggregatorDrone::Construct(" + model + ", " + org + ", final result count: " + count + ")");
        this.orgName = org;
        this.taskModel = model;
        aggregatorModel = new AggregatorModel();
        aggregatorModel.maxResults = count;
        taskModel.aggregateForKey(this.orgName, aggregatorModel);
        client.verbose = ProcessModel.instance().getVerbose();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {

        myThread = Thread.currentThread();

        if (ProcessModel.instance().getVerbose()) {
            System.out.println("AggregatorDrone::run()");
        }
        String since = null;
        // seed url, use this to find out what pages we've got
        String seedUrl = "https://api.github.com/orgs/" + orgName + "/repos";
        String un = taskModel.getUserName();
        String pwd = taskModel.getPasswordOrToken();

        // find the start and end pages first
        ParsedHttpResponse headResponse = client.safeHeadAuthorizedRequest(seedUrl, un, pwd, null);

        String currPageLink = seedUrl;
        String lastPageLink = "";
        String nextPageLink = "";
        String linkHeader = "";
        String nextPageLinkData = null;
        String lastPageLinkData = null;

        if (headResponse != null) {

            ArrayList<String> linkHeaderValues = (ArrayList<String>) headResponse.getHeader("Link");
            if (linkHeaderValues != null) {
                // find the current page etc
                // woohoo
                linkHeader = linkHeaderValues.get(0);

                String[] pages = linkHeader.split(", ");

                nextPageLinkData = pages[0];
                lastPageLinkData = pages[1];

                nextPageLink = nextPageLinkData.substring(1, nextPageLinkData.lastIndexOf('>'));
                lastPageLink = lastPageLinkData.substring(1, lastPageLinkData.lastIndexOf('>'));
            } else {
                // hmmm
            }

            List<GitHubRepo> aggregates = Collections.synchronizedList(new ArrayList<GitHubRepo>());

            aggregatorModel.aggregateForKey("aggregateData", aggregates);
            aggregatorModel.aggregateForKey("orgName", orgName);

            do {
                if (ProcessModel.instance().getVerbose()) {
                    System.out.println("current page: [" + currPageLink + "]");
                }

                String data = "";
                Gson gson = new Gson();

                ParsedHttpResponse r = client.safeAuthorizedRequest(currPageLink, un, pwd, null);

                if (r == null) {
                    break;
                }
                data = r.getContent();
                // replace the known bad var names we couldn't
                // serialized from
                // in java. Silly naming conventions :)
                data = data.replace("\"private\":", "\"isPrivate\":");

                ArrayList<String> currPageHeaderValues = (ArrayList<String>) r.getHeader("Link");

                if (currPageHeaderValues != null) {

                    linkHeader = currPageHeaderValues.get(0);
                    String[] pages = linkHeader.split(", ");

                    nextPageLinkData = pages[0];
                    lastPageLinkData = pages[1];

                    nextPageLink = nextPageLinkData.substring(1, nextPageLinkData.lastIndexOf('>'));
                    lastPageLink = lastPageLinkData.substring(1, lastPageLinkData.lastIndexOf('>'));

                    currPageLink = nextPageLink;
                }

                // create a list of GitHub repos
                List<GitHubRepo> reposForOrg = gson.fromJson(data, new TypeToken<List<GitHubRepo>>() {
                }.getType());

                // append more repos to the aggregate list for this org
                for (GitHubRepo current : reposForOrg) {
                    aggregates.add(current);
                }

                // sanity, reset this here.
                aggregatorModel.aggregateForKey("aggregateData", aggregates);

                // TODO REMOVE - help with rate limit!!
                break;
            } while (!currPageLink.equals(lastPageLink));

            if (ProcessModel.instance().getVerbose()) {
                System.out.println("Aggregated: " + aggregates.size() + " repositories");
            }
            int pullCnt = 0;

            List<Thread> runners = new ArrayList<Thread>();
            // after aggregation, lets go get the pull data.
            // do it in parallel.
            for (GitHubRepo repo : aggregates) {

                if (repo.isPrivate == false) {
                    Thread t = new Thread(new PullAggregator(repo, un, pwd));
                    t.setDaemon(false);
                    t.setName("PullAggregator::" + orgName + "::" + repo.name);
                    t.run();
                    runners.add(t);
                }
                // TODO - remove helps with rate limit
                // break;
            }

            while (runners.size() > 0) {
                Iterator<Thread> it = runners.iterator();
                while (it.hasNext()) {
                    Thread t = it.next();

                    if (t.getState() == State.TERMINATED || t.getState() == State.NEW) {

                        if (t.getState() == State.NEW) {
                            t.interrupt();
                        }

                        it.remove();
                    }
                }

                ThreadUtils.safeSleep(Constants.IDLE_TIME, ProcessModel.instance().getVerbose());
            }

            System.out.println("Finished processing: " + aggregates.size() + " repositories, and aggreggated: " + pullCnt + " pulls");
        } else {
            System.err.println("No response was found for: " + seedUrl);
        }
    }

    public class PullAggregatorDrone implements Runnable {

        private String un;

        private String pwd;

        public String pUrl;

        public int maxPulls = 3;

        List<GitHubPull> pullsToProcess = new ArrayList<GitHubPull>();

        public void addPull(GitHubPull pull, String un, String pwd) {

            this.un = un;
            this.pwd = pwd;
            pullsToProcess.add(pull);

        }

        public boolean isFull() {
            return pullsToProcess.size() > maxPulls;
        }

        @Override
        public void run() {

            while (pullsToProcess.size() > 0) {

                GitHubPull pull = pullsToProcess.remove(0);

                String pullUrl = pUrl + "/" + String.valueOf(pull.number);

                ParsedHttpResponse pullDataResponse = client.safeAuthorizedRequest(pullUrl, un, pwd, null);

                if (pullDataResponse == null) {
                    continue;
                }

                String pullData = pullDataResponse.getContent();
                Gson gson = new Gson();
                if (pullData != null) {
                    GitHubPullData data = gson.fromJson(pullData, GitHubPullData.class);
                    pull.pullData = data;
                } else {
                    System.out.println("No Pull Data returned for pull url: " + pUrl);
                }

                // TODO - REMOVE helps with rate limit
                // break;

            }
        }
    }

    public class PullAggregator implements Runnable {

        private GitHubRepo repo;

        private String un;

        private String pwd;

        public PullAggregator(GitHubRepo repo, String un, String pwd) {
            this.un = un;
            this.pwd = pwd;
            this.repo = repo;
        }

        @Override
        public void run() {

            // remove the template from the end
            // Lazy Mans Template expansion :P. Hey its (going on) 4 day project
            String pUrl = repo.pulls_url.replace("{/number}", "");

            // get the current page
            ParsedHttpResponse pullsResponse = client.safeAuthorizedRequest(pUrl, un, pwd, null);

            if (pullsResponse == null) {
                return;
            }

            // starts at first page here (clever right?)
            Gson gson = new Gson();

            String pulls = pullsResponse.getContent();
            List<GitHubPull> pullObjects = gson.fromJson(pulls, new TypeToken<List<GitHubPull>>() {
            }.getType());

            // up to 10 per drone
            List<Thread> pullDrones = new ArrayList<Thread>();
            PullAggregatorDrone pullDrone = new PullAggregatorDrone();
            for (GitHubPull pull : pullObjects) {

                if (pull.locked) {
                    continue;
                }

                pullDrone.pUrl = pUrl;
                pullDrone.addPull(pull, un, pwd);

                if (pullDrone.isFull()) {

                    Thread t = new Thread(pullDrone);
                    pullDrones.add(t);
                    t.setDaemon(false);
                    t.setName("PullAggregatorDrone::" + pull.url);
                    t.start();

                    // reset this for next round
                    pullDrone = new PullAggregatorDrone();
                }

                // TODO - REMOVE helps with rate limit
                // break;

            }

            // wait for death
            while (pullDrones.size() > 0) {

                Iterator<Thread> it = pullDrones.iterator();
                while (it.hasNext()) {
                    Thread t = it.next();

                    if (t.getState() == State.TERMINATED || t.getState() == State.NEW) {
                        t.interrupt();
                        it.remove();
                    }
                }
                ThreadUtils.safeSleep(Constants.IDLE_TIME, ProcessModel.instance().getVerbose());
            }

            // assign these to this repo
            repo.pulls = pullObjects;

            if (ProcessModel.instance().getVerbose()) {
                System.out.println("Aggregator Finished! Repo: " + repo.id + ": " + repo.name + " " + repo.url + ", pulls: " + repo.pulls.size() + ", forks: "
                        + repo.forks_count + ", stars: " + repo.stargazers_count);
            }

            return;
        }
    }
}
