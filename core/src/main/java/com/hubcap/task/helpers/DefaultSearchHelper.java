package com.hubcap.task.helpers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hubcap.lowlevel.HttpClient;
import com.hubcap.process.ProcessModel;
import com.hubcap.task.TaskRunner;
import com.hubcap.task.model.GitHubLimitsByResource;
import com.hubcap.task.model.GitHubPull;
import com.hubcap.task.model.GitHubRateLimit;
import com.hubcap.task.model.GitHubRepo;
import com.hubcap.utils.ErrorUtils;

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
                    String data = client.getAuthorizedRequest(rlUrl, un, pwd, null);
                    Gson gson = new Gson();
                    GitHubRateLimit rateLimit = gson.fromJson(data, GitHubRateLimit.class);

                    ProcessModel.instance().setRateLimitData(rateLimit);
                } catch (IOException ex) {
                    ErrorUtils.printStackTrace(ex);
                }
            }
            for (int i = 0; i < cLen; i += 2) {

                String since = null;
                String orgName = cmd.getArgs()[i];
                int count = Integer.parseInt(cmd.getArgs()[i + 1]);

                int maxResults = opts.containsKey("maxResults") ? Integer.parseInt((String) opts.get("maxResults")) : 1000;
                int totalResults = 0;

                if (ProcessModel.instance().getRateLimitData().rate.remaining > maxResults * 2) {
                    // pound that API until we get what we want!!
                    while (totalResults < maxResults) {
                        try {

                            String url = "https://api.github.com/orgs/" + orgName + "/repos";
                            String un = taskModel.getUserName();
                            String pwd = taskModel.getPasswordOrToken();
                            if (since != null) {
                                url += "?since=" + since;
                            }

                            Map<String, String> headers = new HashMap<String, String>();
                            String data = client.getAuthorizedRequest(url, un, pwd, headers);

                            if (data != null) {

                                Gson gson = new Gson();

                                // replace the known bad var names we couldn't
                                // serialized from
                                // in java. Silly naming conventions :)
                                data.replace("\"private\":", "\"isPrivate\":");

                                PrintWriter writer = new PrintWriter("resources/results.json", "UTF-8");
                                writer.println(data);
                                writer.close();

                                // create a list of GitHub repos
                                List<GitHubRepo> reposForOrg = gson.fromJson(data, new TypeToken<List<GitHubRepo>>() {
                                }.getType());
                                totalResults += reposForOrg.size();

                                // no more results?
                                if (reposForOrg.size() == 0 || reposForOrg.size() < 30) {
                                    break;
                                }

                                // aggregate the data within the task model
                                taskModel.setMaxResults(count);
                                taskModel.aggregate(reposForOrg);

                                String newSince = String.valueOf(reposForOrg.get(reposForOrg.size() - 1).id);

                                if (newSince.equals(since)) {

                                    break;
                                }

                                since = newSince;
                                if (this.listener != null) {
                                    this.listener.processTaskHelperData(reposForOrg);
                                }

                                System.out.println("Found: " + reposForOrg.size() + " repositories using: " + url + " total results: " + totalResults + ", since is now: "
                                        + since);

                                String state = null;

                                for (GitHubRepo repo : reposForOrg) {

                                    String pUrl = repo.pulls_url.replace("{/number}", "");
                                    // get the pulls
                                    String pulls = client.getAuthorizedRequest(pUrl, un, pwd, null);
                                    List<GitHubPull> pullObjects = gson.fromJson(pulls, new TypeToken<List<GitHubPull>>() {
                                    }.getType());
                                    repo.pulls = pullObjects;

                                    System.out.println("Repo: " + repo.id + ": " + repo.name + " " + repo.url);
                                }
                                if (totalResults >= maxResults) {
                                    break;
                                }
                            }
                        } catch (IOException ex) {
                            if (ProcessModel.instance().getVerbose())
                                ErrorUtils.printStackTrace(ex);
                        }
                    }
                } else {
                    System.err.println("Your rate limit is exhausted, try again later!");
                }
            }
        }
        die();
    }
}
