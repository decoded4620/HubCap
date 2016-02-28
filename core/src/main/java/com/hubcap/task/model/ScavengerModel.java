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

import com.hubcap.process.ProcessModel;

public class ScavengerModel extends BaseModel {

    public static final String KEY_AGG_DATA = "aggregateData";

    public static final String KEY_ORG_NAME = "orgName";

    public static final String KEY_TOP_BY_STARS = "topNByStars";

    public static final String KEY_TOP_BY_FORKS = "topNByForks";

    public static final String KEY_TOP_BY_PRS = "topNByPR";

    public static final String KEY_TOP_BY_CONTRIB = "";

    public int maxResults;

    // The 4 dimensions
    private List<GitHubRepo> topNReposByStars = Collections.synchronizedList(new ArrayList<GitHubRepo>());

    private List<GitHubRepo> topNReposByForks = Collections.synchronizedList(new ArrayList<GitHubRepo>());

    private List<GitHubRepo> topNReposByPR = Collections.synchronizedList(new ArrayList<GitHubRepo>());

    private List<GitHubRepo> topNReposByContrib = Collections.synchronizedList(new ArrayList<GitHubRepo>());

    public ScavengerModel() {
        // TODO Auto-generated constructor stub\
        super();
    }

    public void setReposData() {

    }

    @Override
    public Object calculate() {

        ScavengerModelResult result = new ScavengerModelResult();
        // at this point, lets go through the task model and sort that damn
        // thing.

        @SuppressWarnings("unchecked")
        List<GitHubRepo> reposForOrg = (List<GitHubRepo>) getAggregatedDataForKey(KEY_AGG_DATA);

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
        aggregateForKey(KEY_TOP_BY_STARS, topNReposByStars);

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
        aggregateForKey(KEY_TOP_BY_FORKS, topNReposByStars);

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

        aggregateForKey(KEY_TOP_BY_PRS, topNReposByPR);

        // topN by contribution percentage
        Collections.sort(reposForOrg, new Comparator<GitHubRepo>() {

            @Override
            public int compare(GitHubRepo a, GitHubRepo b) {

                float f1 = a.forks_count;
                float f2 = b.forks_count;

                float p1 = a.pulls == null ? 0 : a.pulls.size();
                float p2 = b.pulls == null ? 0 : b.pulls.size();

                float v1 = (f1 > 0 ? p1 / f1 : 0);
                float v2 = (f2 > 0 ? p2 / f2 : 0);

                if (v1 > v2) {
                    return -1;
                }

                if (v1 == v2) {
                    return 0;
                }

                return 1;
            }
        });

        // get the top N forks
        for (int i = 0; i < size; ++i) {
            topNReposByContrib.add(reposForOrg.get(i));
        }

        aggregateForKey(KEY_TOP_BY_CONTRIB, topNReposByContrib);

        GitHubRepo[] repos;
        repos = topNReposByStars.toArray(new GitHubRepo[topNReposByStars.size()]);

        if (ProcessModel.instance().getVerbose()) {
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

            repos = topNReposByContrib.toArray(new GitHubRepo[topNReposByContrib.size()]);
            System.out.println("[" + getAggregatedDataForKey("orgName") + "] - Top " + size + " repositories by Contribution");
            for (GitHubRepo repo : repos) {
                System.out.println("   - " + repo.name + "[" + repo.url + "] forks: " + repo.forks_count + ", pulls: " + (repo.pulls == null ? 0 : repo.pulls.size()));
            }
        }

        result.topByStars = new GitHubRepo[topNReposByStars.size()];
        topNReposByStars.toArray(result.topByStars);

        result.topByForks = new GitHubRepo[topNReposByForks.size()];
        topNReposByForks.toArray(result.topByForks);

        result.topByPR = new GitHubRepo[topNReposByPR.size()];
        topNReposByPR.toArray(result.topByPR);

        result.topByContrib = new GitHubRepo[topNReposByContrib.size()];
        topNReposByContrib.toArray(result.topByContrib);

        return result;
    }

    public class ScavengerModelResult {

        public GitHubRepo[] topByStars;

        public GitHubRepo[] topByForks;

        public GitHubRepo[] topByPR;

        public GitHubRepo[] topByContrib;
    }
}
