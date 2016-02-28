package com.hubcap.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.hubcap.Constants;
import com.hubcap.lowlevel.HttpClient;
import com.hubcap.lowlevel.ParsedHttpResponse;
import com.hubcap.task.model.GitHubRateLimit;
import com.hubcap.task.model.GitHubUser;
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

/**
 * The process model contains a few properties whose lifetime and values
 * transcend a single task. verbose / quiet printing, session user, and REPL
 * settings are included to name a few.
 * 
 * @author Decoded4620 2016
 */
public class ProcessModel {

    private static ProcessModel inst;

    public static ProcessModel instance() {
        if (inst == null) {
            inst = new ProcessModel();
        }
        return inst;
    }

    // the GitHubUser authorizing requests made by HubCap
    private GitHubUser sessionUser = null;

    // the user's Rate Limiting data as reported by GitHub
    private GitHubRateLimit rateLimitData;

    // If True, the REPL is kept alive. so the user can spawn several command
    // line requests,
    // hitting ENTER will spawn the new job. jobs are run in parallel meaning
    // there are no guaranteed
    // delivery order.
    private boolean isREPLFallback = false;

    private boolean verbose = false;

    public synchronized void setREPLFallback(boolean value) {
        isREPLFallback = value;
    }

    public synchronized boolean getREPLFallback() {
        return isREPLFallback;
    }

    public synchronized void setVerbose(boolean value) {
        verbose = value;
    }

    public synchronized boolean getVerbose() {
        return verbose;
    }

    public synchronized String getSessionUser() {
        return this.sessionUser.userName;
    }

    public synchronized String getSessionPasswordOrToken() {
        return this.sessionUser.passwordOrToken;
    }

    /**
     * Authorizes a user for this session. Each Subsequent call will authorize a
     * different user, thus overriding the previous user.
     * 
     * @param un
     * @param tokenOrPass
     */
    public synchronized void auth(String un, String tokenOrPass) {
        this.sessionUser.userName = un;
        this.sessionUser.passwordOrToken = tokenOrPass;
    }

    /**
     * Rate Limit data based on github's view
     * 
     * @param data
     */
    private synchronized void setRateLimitData(GitHubRateLimit data) {

        System.out.println("setRateLimitData(" + data.rate.remaining + ", " + data.rate.limit + ", resets at: " + (new Date(data.rate.reset * 1000)).toString() + ")");
        this.rateLimitData = data;
    }

    public synchronized GitHubRateLimit getRateLimitData() {
        return this.rateLimitData;
    }

    /**
     * Refresh our current rate limit details so we can check prior to running
     * more tasks.
     */
    public synchronized void updateRateLimitData() {

        HttpClient client = new HttpClient();

        String rlUrl = Constants.GITHUB_API_URL + "/rate_limit";

        String un = sessionUser.userName;
        String pwd = sessionUser.passwordOrToken;

        try {
            ParsedHttpResponse data = client.getAuthorizedRequest(rlUrl, un, pwd, null);
            Gson gson = new Gson();
            GitHubRateLimit rateLimit = gson.fromJson(data.getContent(), GitHubRateLimit.class);
            setRateLimitData(rateLimit);
        } catch (IOException ex) {
            ErrorUtils.printStackTrace(ex);
        }
    }

    /**
     * Singleton Access CTOR When process model is constructed it will attempt
     * to load a local file with your GitHub userName and your password or
     * authorization token. If found, the session is 'auto-authorized'. If not
     * found the session is default, and uses default GitHub rate limits.
     */
    private ProcessModel() {

        boolean isException = false;
        GitHubUser user = null;

        try {
            String str = new String(Files.readAllBytes(Paths.get("resources/local.json")));
            Gson gson = new Gson();

            try {
                user = gson.fromJson(str, GitHubUser.class);
            } catch (JsonParseException ex) {
                ErrorUtils.printStackTrace(ex);
                isException = true;
            }
        } catch (IOException ex) {
            isException = true;
            ErrorUtils.printStackTrace(ex);
        } catch (Exception e) {
            ErrorUtils.printStackTrace(e);
        } finally {
            if (user == null) {
                user = new GitHubUser();
            }
            if (isException) {

                user.userName = "";
                user.passwordOrToken = "";
            }
            this.sessionUser = user;
        }
    }
}
