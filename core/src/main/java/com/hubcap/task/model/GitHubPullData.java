package com.hubcap.task.model;

import java.util.Date;

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

public class GitHubPullData {

    /**
     * This is associated with a GitHubPull instance. the GitHubPull instance
     * reveals top level data about a pull for a specified Repo, whereas this
     * Object contains the verbose details of the pull
     */
    public GitHubPullData() {
    }

    public String url = null;

    public long id = 0;

    public String html_url = null;

    public String diff_url = null;

    public String patch_url = null;

    public String issue_url = null;

    public int number = 0;

    public String state = null;

    public boolean locked = false;

    public String title = null;

    public GitHubRemoteUser user = null;

    public String body;

    public Date created_at;

    public Date updated_at;

    public Date closed_at = null;

    public Date merged_at = null;

    public String merge_commit_sha = null;

    public GitHubRemoteUser assignee = null;

    public String commits_url = null;

    public String statuses_url = null;

    public boolean merged = false;

    public boolean mergeable = true;

    public String mergeable_state = null;

    public String merged_by = null;

    public long comments = 0;

    public long review_comments = 0;

    public long commits = 0;

    public long additions = 0;

    public long deletions = 0;

    public long changed_files = 0;

    public GitHubLinkRelationships _links;
}
