package com.hubcap.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
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
 * This represents the Complex State of HubCap based on the current inputs
 * 
 * @author Decoded4620 2016
 */
public class ProcessModel {

    private static ProcessModel inst;

    private GitHubUser sessionUser = new GitHubUser();

    private GitHubRateLimit rateLimitData;

    public static ProcessModel instance() {
        if (inst == null) {
            inst = new ProcessModel();
        }
        return inst;
    }

    private boolean isREPLFallback = false;

    private boolean verbose = false;

    public void setREPLFallback(boolean value) {
        isREPLFallback = value;
    }

    public boolean getREPLFallback() {
        return isREPLFallback;
    }

    public void setVerbose(boolean value) {
        verbose = value;
    }

    public boolean getVerbose() {
        return verbose;
    }

    public String getSessionUser() {
        return this.sessionUser.userName;
    }

    public String getSessionPasswordOrToken() {
        return this.sessionUser.passwordOrToken;
    }

    public void auth(String un, String tokenOrPass) {
        this.sessionUser.userName = un;
        this.sessionUser.passwordOrToken = tokenOrPass;
    }

    public void setRateLimitData(GitHubRateLimit data) {

        System.out.println("setRateLimitData(" + data.rate.remaining + ", " + data.rate.limit + ")");
        this.rateLimitData = data;
    }

    public GitHubRateLimit getRateLimitData() {
        return this.rateLimitData;
    }

    /**
     * Singleton Access CTOR
     */
    private ProcessModel() {

        try {
            String str = new String(Files.readAllBytes(Paths.get("resources/local.json")));
            Gson gson = new Gson();

            try {
                GitHubUser user = gson.fromJson(str, GitHubUser.class);
                this.sessionUser = user;
            } catch (JsonParseException ex) {
                ErrorUtils.printStackTrace(ex);
                GitHubUser noUser = new GitHubUser();
                noUser.userName = "";
                noUser.passwordOrToken = "";

                this.sessionUser = noUser;
            }
        } catch (IOException ex) {
            ErrorUtils.printStackTrace(ex);
            GitHubUser noUser = new GitHubUser();
            noUser.userName = "";
            noUser.passwordOrToken = "";

            this.sessionUser = noUser;
        }
    }
}
