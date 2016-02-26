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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AggregatorModel extends BaseModel {

    public int maxResults;

    private List<GitHubRepo> topNReposByStars = Collections.synchronizedList(new ArrayList<GitHubRepo>());

    private List<GitHubRepo> topNReposByForks = Collections.synchronizedList(new ArrayList<GitHubRepo>());

    private List<GitHubRepo> topNReposByPR = Collections.synchronizedList(new ArrayList<GitHubRepo>());

    private List<GitHubRepo> topNReposByContrib = Collections.synchronizedList(new ArrayList<GitHubRepo>());

    public AggregatorModel() {
        // TODO Auto-generated constructor stub\
        super();
    }

    public void setReposData() {

    }

    @Override
    public Object calculate() {

        boolean ret = false;
        // at this point, lets go through the task model and sort that damn
        // thing.

        @SuppressWarnings("unchecked")
        List<GitHubRepo> reposForOrg = (List<GitHubRepo>) getAggregatedDataForKey("aggregateData");

        if (reposForOrg == null) {
            return null;
        }

        int size = Math.min(maxResults, reposForOrg.size() - 1);

        // topN by stars
        Collections.sort(reposForOrg, new Comparator<GitHubRepo>() {

            @Override
            public int compare(GitHubRepo a, GitHubRepo b) {
                if (a.stargazers_count > b.stargazers_count) {
                    return -1;
                }

                if (a.stargazers_count == b.stargazers_count) {
                    return 0;
                }

                return 1;
            }
        });

        // get the top N stars
        for (int i = 0; i < size; ++i) {
            topNReposByStars.add(reposForOrg.get(i));
        }
        aggregateForKey("topNByStars", topNReposByStars);

        // topN by fork
        Collections.sort(reposForOrg, new Comparator<GitHubRepo>() {

            @Override
            public int compare(GitHubRepo a, GitHubRepo b) {
                if (a.forks_count > b.forks_count) {
                    return -1;
                }

                if (a.forks_count == b.forks_count) {
                    return 0;
                }

                return 1;
            }
        });
        // get the top N forks
        // get the top N stars
        for (int i = 0; i < size; ++i) {
            topNReposByForks.add(reposForOrg.get(i));
        }
        aggregateForKey("topNByForks", topNReposByStars);

        // topN by fork
        Collections.sort(reposForOrg, new Comparator<GitHubRepo>() {

            @Override
            public int compare(GitHubRepo a, GitHubRepo b) {
                if (a.pulls != null && b.pulls == null) {
                    return -1;
                }

                if (a.pulls == null && b.pulls == null) {
                    return 0;
                }

                if (a.pulls == null && b.pulls != null) {
                    return 1;
                }

                if (a.pulls.size() > b.pulls.size()) {
                    return -1;
                }

                if (a.pulls.size() == b.pulls.size()) {
                    return 0;
                }

                return 1;
            }
        });

        // get the top N forks
        for (int i = 0; i < size; ++i) {
            topNReposByPR.add(reposForOrg.get(i));
        }

        aggregateForKey("topNByPR", topNReposByStars);

        GitHubRepo[] repos;
        repos = topNReposByStars.toArray(new GitHubRepo[topNReposByStars.size()]);

        System.out.println("[" + getAggregatedDataForKey("orgName") + "] - Top " + size + " repositories by stargazers count");
        for (GitHubRepo repo : repos) {
            System.out.println("   - " + repo.name + "[" + repo.url + "] stars: " + repo.stargazers_count);
        }

        repos = topNReposByForks.toArray(new GitHubRepo[topNReposByForks.size()]);

        System.out.println("[" + getAggregatedDataForKey("orgName") + "] - Top " + size + " repositories by Forks");
        for (GitHubRepo repo : repos) {
            System.out.println("   - " + repo.name + "[" + repo.url + "] forks: " + repo.forks_count);
        }

        repos = topNReposByPR.toArray(new GitHubRepo[topNReposByPR.size()]);
        System.out.println("[" + getAggregatedDataForKey("orgName") + "] - Top " + size + " repositories by Pull Request count");
        for (GitHubRepo repo : repos) {
            System.out.println("   - " + repo.name + "[" + repo.url + "] pulls: " + (repo.pulls == null ? 0 : repo.pulls.size()));
        }

        ret = true;

        return ret;
    }
}
