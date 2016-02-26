package com.hubcap.task.model;

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

import java.util.Date;
import java.util.List;

/**
 * GitHub Repository Descriptor
 * 
 * @author Decoded4620 2016
 */
public class GitHubRepo {

    public long id;

    public GitHubRepoOwner owner;

    public GitHubRepoPermissions permissions;

    public String name = "";

    public String full_name = "";

    public String description = "";

    public boolean fork = true;

    public boolean isPrivate = false;

    public String url = "";

    public String html_url = "";

    public String archive_url = "";

    public String assignees_url = "";

    public String blobs_url = "";

    public String branches_url = "";

    public String clone_url = "";

    public String collaborators_url = "";

    public String comments_url = "";

    public String commits_url = "";

    public String compare_url = "";

    public String contents_url = "";

    public String contributors_url = "";

    public String deployments_url = "";

    public String downloads_url = "";

    public String events_url = "";

    public String forks_url = "";

    public String git_commits_url = "";

    public String git_refs_url = "";

    public String git_tags_url = "";

    public String git_url = "";

    public String hooks_url = "";

    public String issue_comment_url = "";

    public String issue_events_url = "";

    public String issues_url = "";

    public String keys_url = "";

    public String labels_url = "";

    public String languages_url = "";

    public String merges_url = "";

    public String milestones_url = "";

    public String mirror_url = "";

    public String notifications_url = "";

    public String pulls_url = "";

    public String releases_url = "";

    public String ssh_url = "";

    public String stargazers_url = "";

    public String statuses_url = "";

    public String subscribers_url = "";

    public String subscription_url = "";

    public String svn_url = "";

    public String tags_url = "";

    public String teams_url = "";

    public String trees_url = "";

    public String homepage = "";

    public String language = null;

    public long forks_count = 9;

    public long stargazers_count = 80;

    public long watchers_count = 80;

    public long size = 108;

    public String default_branch = "";

    public long open_issues_count = 0;

    public boolean has_issues = true;

    public boolean has_wiki = true;

    public boolean has_pages = false;

    public boolean has_downloads = true;

    public Date pushed_at = null;

    public Date created_at = null;

    public Date updated_at = null;

    // derived from pulls url
    public List<GitHubPull> pulls;

    public GitHubRepo() {
    }

}
