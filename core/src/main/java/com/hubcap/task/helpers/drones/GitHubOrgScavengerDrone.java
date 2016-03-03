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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.hubcap.Constants;
import com.hubcap.lowlevel.HttpClient;
import com.hubcap.lowlevel.ParsedHttpResponse;
import com.hubcap.lowlevel.SewingMachine;
import com.hubcap.lowlevel.ThreadSpoolTask;
import com.hubcap.process.ProcessModel;
import com.hubcap.task.model.GitHubPull;
import com.hubcap.task.model.GitHubPullData;
import com.hubcap.task.model.GitHubRepo;
import com.hubcap.task.model.ScavengerModel;
import com.hubcap.task.model.TaskModel;
import com.hubcap.utils.ErrorUtils;
import com.hubcap.utils.ThreadUtils;

/**
 * The GitHubOrgScavengerDrone is a collector of information for GitHub
 * Repositories. Its job is to focus on a single organization. If you pass more
 * than a single pair of parameters, (i.e. Org1 10 Org2 10), then an
 * AggregatorDrone is created for each Organization. The Drone will find all
 * repositories belonging to the Organization, as well as spawning several
 * PullAggregatorDrones which aggregate the pull data for Repositories.
 * 
 * @author Decoded4620 2016
 */
public class GitHubOrgScavengerDrone implements Runnable {

    // Represents the data aggregated specifically by This AggregatorDrone
    // instance
    private ScavengerModel scavengerModel;

    // the shared resource taskModel that all AggregatorDrone instances are
    // writing
    // to.
    // Any modification operations, or iteration operations should be done
    // within synchronized{} blocks.
    private TaskModel sharedResource_taskModel;

    // the organization that this repository is Crawling.
    private String orgName;

    // the Communication Object. There is one per aggregate drone to insure
    // true threaded autonomy when making calls.
    // rather than having a singleton instance, for example.
    private HttpClient client = new HttpClient();

    // the current thread that is executing this Drone.
    // it should likely not be the main thread, as these objects
    // are specifically created via thread factory (rather than leaving the
    // choice up to the Executor's implementation)
    private Thread myThread;

    // threaded task management at its finest.
    private SewingMachine sewingMachine;

    /**
     * CTOR
     * 
     * @param model
     * @param org
     * @param maxResults
     */
    public GitHubOrgScavengerDrone(SewingMachine sewingMachine, TaskModel model, String org, int maxResults) {

        if (ProcessModel.instance().getVerbose()) {
            System.out.println("GitHubOrgScavengerDrone::Construct(" + model + ", " + org + ", final result count: " + maxResults + ")");
        }
        this.orgName = org;
        this.sharedResource_taskModel = model;
        scavengerModel = new ScavengerModel();
        this.sewingMachine = sewingMachine;

        sharedResource_taskModel.addScavengerModel(scavengerModel);
        sharedResource_taskModel.aggregateForKey(this.orgName, scavengerModel);

        scavengerModel.maxResults = maxResults;
    }

    /**
     * Thread reference for interruption purposes.
     * 
     * @return
     */
    public Thread getThread() {
        return myThread;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {

        myThread = Thread.currentThread();

        if (ProcessModel.instance().getVerbose()) {
            System.out.println("GitHubOrgScavengerDrone::run()");
        }
        // seed url, use this to find out what pages we've got
        String seedUrl = Constants.GITHUB_API_URL + "/orgs/" + orgName + "/repos";

        // enable authorization automatically for this drone
        client.setIsAuthorizedClient(true, ProcessModel.instance().getSessionUser(), ProcessModel.instance().getSessionPasswordOrToken());

        // find the start and end pages first
        ParsedHttpResponse headResponse = client.safeHeadRequest(seedUrl, null);

        if (ProcessModel.instance().getVerbose()) {
            System.out.println("    ==> found header for paging info!");
        }

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

            scavengerModel.aggregateForKey(ScavengerModel.KEY_AGG_DATA, aggregates);
            scavengerModel.aggregateForKey(ScavengerModel.KEY_ORG_NAME, orgName);

            do {
                if (ProcessModel.instance().getVerbose()) {
                    System.out.println("\t\tprocessing current page: [" + currPageLink + "]");
                }

                String data = "";
                Gson gson = new Gson();

                ParsedHttpResponse r = client.safeGetRequest(currPageLink, null);

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
                scavengerModel.aggregateForKey(ScavengerModel.KEY_AGG_DATA, aggregates);

            } while (!currPageLink.equals(lastPageLink));

            if (ProcessModel.instance().getVerbose()) {
                System.out.println("Aggregating Pulls for : " + aggregates.size() + " repositories");
            }

            try {

                // after aggregation, lets go get the pull data.
                // do it in parallel.
                if (ProcessModel.instance().getVerbose()) {
                    System.out.println("Repositories found for " + orgName + ": " + aggregates.size());
                }

                for (GitHubRepo repo : aggregates) {
                    if (!repo.isPrivate) {
                        HttpClient aggregatorClient = new HttpClient();
                        client.copyAuth(aggregatorClient);

                        try {
                            PullDataCrawlerTask aggregator = new PullDataCrawlerTask(repo, aggregatorClient);
                            sewingMachine.assignTask(aggregator);
                        } catch (InstantiationException e) {
                            break;
                        }
                    }
                }

                // wait for a start
                while (sewingMachine.taskCount() == 0) {
                    ThreadUtils.safeSleep(Constants.NEW_THREAD_SPAWN_BREATHING_TIME, ProcessModel.instance().getVerbose());
                }

                // wait for running spools to be 0
                while (sewingMachine.getThread() != null && sewingMachine.runningSpools() > 0) {
                    if (!ThreadUtils.safeSleep(Constants.IDLE_TIME, ProcessModel.instance().getVerbose())) {
                        System.err.println("Bailing early, ");
                        SewingMachine.report();
                        break;
                    }
                }

                if (ProcessModel.instance().getVerbose()) {
                    System.out.println("waiting for: " + sewingMachine.taskCount() + " tasks");
                }

                System.out.println("Finished processing: " + aggregates.size() + " repositories");
            } catch (Exception ex) {
                ErrorUtils.printStackTrace(ex);
            }
        } else {
            System.err.println("No response was found for: " + seedUrl);
        }

        myThread = null;
    }

    public class PullDataCrawlerTask implements ThreadSpoolTask {

        private GitHubRepo repo;

        private HttpClient aggregatorClient;

        // private SewingMachine pullTaskPool;

        public PullDataCrawlerTask(GitHubRepo repo, HttpClient inClient) throws InstantiationException {
            this.repo = repo;
            this.aggregatorClient = inClient;

        }

        @Override
        public void perform() {

            if (ProcessModel.instance().getVerbose()) {
                System.out.println(Thread.currentThread().getName() + "::PullDataCrawlerTask::perform()");
            }

            // remove the template from the end
            // Lazy Mans Template expansion :P. Hey its (going on) 4 day project
            String pUrl = repo.pulls_url.replace("{/number}", "");

            // get the current page
            ParsedHttpResponse pullsResponse = aggregatorClient.safeGetRequest(pUrl, null);

            if (pullsResponse == null) {
                return;
            }

            // starts at first page here (clever right?)
            Gson gson = new Gson();

            String pulls = pullsResponse.getContent();
            List<GitHubPull> pullObjects = gson.fromJson(pulls, new TypeToken<List<GitHubPull>>() {
            }.getType());

            if (pullObjects.size() > 0) {
                // up to 10 per drone
                PullDataChunkCrawlerTask pullDataChunkTask = null;
                try {

                    for (GitHubPull pull : pullObjects) {

                        if (pull.locked) {
                            continue;
                        }

                        if (pullDataChunkTask == null) {
                            HttpClient pullClient = new HttpClient();
                            aggregatorClient.copyAuth(pullClient);
                            pullClient.setIsAuthorizedClient(true, ProcessModel.instance().getSessionUser(), ProcessModel.instance().getSessionPasswordOrToken());
                            pullDataChunkTask = new PullDataChunkCrawlerTask(pullClient);
                            // reset this for next round
                        }

                        pullDataChunkTask.pUrl = pUrl;
                        pullDataChunkTask.addPull(pull);

                        if (pullDataChunkTask.isFull()) {
                            sewingMachine.assignTask(pullDataChunkTask);
                            pullDataChunkTask = null;
                        }
                    }

                    if (pullDataChunkTask != null) {
                        sewingMachine.assignTask(pullDataChunkTask);
                        pullDataChunkTask = null;
                    }
                } catch (InstantiationException e) {
                    ErrorUtils.printStackTrace(e);
                }

                // wait for a start
                while (sewingMachine.taskCount() == 0) {
                    ThreadUtils.safeSleep(Constants.NEW_THREAD_SPAWN_BREATHING_TIME, ProcessModel.instance().getVerbose());
                }

                while (sewingMachine.getThread() != null && sewingMachine.taskCount() > 0) {
                    if (!ThreadUtils.safeSleep(Constants.IDLE_TIME, ProcessModel.instance().getVerbose())) {
                        System.err.println("Bailing early, ");
                        SewingMachine.report();
                        break;
                    }
                }

                // assign these to this repo
                repo.pulls = pullObjects;
                if (ProcessModel.instance().getVerbose()) {
                    System.out.println("PullDataCrawlerTask Collected: " + repo.id + ": " + repo.name + "\n\turl:[" + repo.url + "]\n\tpulls: " + repo.pulls.size()
                            + "\n\tforks: " + repo.forks_count + "\n\tstars: " + repo.stargazers_count);
                }

            }

            return;
        }
    }

    public class PullDataChunkCrawlerTask implements ThreadSpoolTask {

        public String pUrl;

        private HttpClient pullClient;

        public int maxPulls = Constants.MAX_HTTP_REQUESTS_PER_THREAD;

        List<GitHubPull> pullsToProcess = new ArrayList<GitHubPull>();

        public PullDataChunkCrawlerTask(HttpClient pullClient) throws InstantiationException {
            this.pullClient = pullClient;
        }

        public void addPull(GitHubPull pull) {
            pullsToProcess.add(pull);
        }

        public boolean isFull() {
            return pullsToProcess.size() > maxPulls;
        }

        @Override
        public void perform() {
            if (ProcessModel.instance().getVerbose()) {
                System.out.println(Thread.currentThread().getName() + "::PullDataChunkCrawlerTask::perform(), processing " + pullsToProcess.size() + " pulls");
            }
            int processed = 0;
            while (pullsToProcess.size() > 0) {

                GitHubPull pull = pullsToProcess.remove(0);
                String pullUrl = pUrl + "/" + String.valueOf(pull.number);

                ParsedHttpResponse pullDataResponse = this.pullClient.safeGetRequest(pullUrl, null);

                if (pullDataResponse != null) {

                    String pullData = pullDataResponse.getContent();
                    Gson gson = new Gson();

                    if (pullData != null) {
                        try {
                            pullData = pullData.replace("\\\"", "'");
                            GitHubPullData pullDataObj = gson.fromJson(pullData, GitHubPullData.class);
                            pull.pullData = pullDataObj;
                        } catch (JsonParseException e) {
                            if (ProcessModel.instance().getVerbose()) {
                                ErrorUtils.printStackTrace(e);
                            }
                        }
                        processed++;

                    } else {
                        System.err.println("No Pull Data returned for pull url: " + pUrl);
                    }
                } else {
                    System.err.println("Pull Response was NULL for url: " + pUrl);
                }
            }

            if (ProcessModel.instance().getVerbose()) {
                System.err.println(Thread.currentThread().getName() + "::PullDataChunkCrawlerTask::perform(completed " + processed + " pulls)");
            }
        }
    }
}
