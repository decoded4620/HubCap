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
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.hubcap.Constants;
import com.hubcap.lowlevel.HttpClient;
import com.hubcap.lowlevel.ParsedHttpResponse;
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

    /**
     * CTOR
     * 
     * @param model
     * @param org
     * @param maxResults
     */
    public GitHubOrgScavengerDrone(TaskModel model, String org, int maxResults) {

        System.out.println("GitHubOrgScavengerDrone::Construct(" + model + ", " + org + ", final result count: " + maxResults + ")");
        this.orgName = org;
        this.sharedResource_taskModel = model;
        scavengerModel = new ScavengerModel();

        sharedResource_taskModel.addScavengerModel(scavengerModel);
        sharedResource_taskModel.aggregateForKey(this.orgName, scavengerModel);

        scavengerModel.maxResults = maxResults;
        client.verbose = ProcessModel.instance().getVerbose();
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
            System.out.println("AggregatorDrone::run()");
        }
        // seed url, use this to find out what pages we've got
        String seedUrl = Constants.GITHUB_API_URL + "/orgs/" + orgName + "/repos";

        // enable authorization automatically for this drone
        client.setIsAuthorizedClient(true, ProcessModel.instance().getSessionUser(), ProcessModel.instance().getSessionPasswordOrToken());

        // find the start and end pages first
        ParsedHttpResponse headResponse = client.safeHeadRequest(seedUrl, null);

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
                    System.out.println("current page: [" + currPageLink + "]");
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

            int pullCnt = 0;

            List<Thread> runners = new ArrayList<Thread>();
            if (ProcessModel.instance().getVerbose()) {
                System.out.println("Aggregating Pulls for : " + aggregates.size() + " repositories");
            }
            // after aggregation, lets go get the pull data.
            // do it in parallel.
            for (GitHubRepo repo : aggregates) {
                if (!repo.isPrivate) {
                    HttpClient aggregatorClient = new HttpClient();
                    client.copyAuth(aggregatorClient);
                    Thread t = new Thread(new PullAggregator(repo, aggregatorClient));
                    t.setDaemon(false);
                    t.setName("PullAggregator::" + orgName + "::" + repo.name);
                    t.run();
                    runners.add(t);
                }
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

        myThread = null;
    }

    public class PullAggregatorDrone implements Runnable {

        public String pUrl;

        private HttpClient pullClient;

        public int maxPulls = Constants.MAX_HTTP_REQUESTS_PER_THREAD;

        List<GitHubPull> pullsToProcess = new ArrayList<GitHubPull>();

        public PullAggregatorDrone(HttpClient pullClient) {
            this.pullClient = pullClient;
        }

        public void addPull(GitHubPull pull) {
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
                    } else {
                        System.out.println("No Pull Data returned for pull url: " + pUrl);
                    }
                }
            }
        }
    }

    public class PullAggregator implements Runnable {

        private GitHubRepo repo;

        private HttpClient aggregatorClient;

        public PullAggregator(GitHubRepo repo, HttpClient inClient) {
            this.repo = repo;
            this.aggregatorClient = inClient;
        }

        @Override
        public void run() {

            if (ProcessModel.instance().getVerbose()) {
                System.out.println("GitHubScavengerDrone::run() => " + Thread.currentThread().getName());
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

            // up to 10 per drone
            List<Thread> pullDrones = new ArrayList<Thread>();

            HttpClient pullClient = new HttpClient();
            aggregatorClient.copyAuth(pullClient);
            PullAggregatorDrone pullDrone = new PullAggregatorDrone(pullClient);

            for (GitHubPull pull : pullObjects) {

                if (pull.locked) {
                    continue;
                }

                pullDrone.pUrl = pUrl;
                pullDrone.addPull(pull);

                if (pullDrone.isFull()) {

                    Thread t = new Thread(pullDrone);
                    pullDrones.add(t);
                    t.setDaemon(false);
                    t.setName("PullAggregatorDrone::" + pull.url);
                    t.start();

                    pullClient = new HttpClient();
                    pullClient.setIsAuthorizedClient(true, ProcessModel.instance().getSessionUser(), ProcessModel.instance().getSessionPasswordOrToken());
                    // reset this for next round
                    pullDrone = new PullAggregatorDrone(pullClient);
                }
            }

            if (ProcessModel.instance().getVerbose()) {
                System.out.println("\tWaiting for " + pullDrones.size() + " Pull Aggregator Drones to fetch data for us");
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
                System.out.println("GitHubScavengerDrone Collected: " + repo.id + ": " + repo.name + "\n\turl:[" + repo.url + "]\n\tpulls: " + repo.pulls.size()
                        + "\n\tforks: " + repo.forks_count + "\n\tstars: " + repo.stargazers_count);
            }

            return;
        }
    }
}
