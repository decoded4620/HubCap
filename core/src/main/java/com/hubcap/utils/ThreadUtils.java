package com.hubcap.utils;

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

import java.io.File;

public class ThreadUtils {

    /**
     * Does a basic calculation on the number of threads you can concuurently
     * run given the desired Cpu Usage, the Cpu wait time, and the Cpu calc
     * time. For slower threaded tasks, such as downloading large files, etc,
     * you may pass in a large number for cpuWaitTime vs. cpuCalcTime (if
     * processing the of data takes .1 X the time it takes to download for
     * example)
     * 
     * @param cpuUsagePct
     *            a float between 0 and 1
     * @param cpuWaitTime
     *            a float
     * @param cpuCalcTime
     *            a float
     * @return the number of concurrent threads that can run with likely
     *         stability given the parameters.
     */
    public static int getStableThreadCount(float cpuUsagePct, float cpuWaitTime, float cpuCalcTime, int maxThreads) {

        // number of processor cores
        int cores = Runtime.getRuntime().availableProcessors();

        // number of disks on the file system (known)
        File[] paths;

        // returns pathnames for files and directory
        paths = File.listRoots();
        int disks = paths.length;

        // c can be 0 -> N
        double c = (cpuWaitTime > 0 && cpuCalcTime > 0) ? cpuWaitTime / cpuCalcTime : 0;
        // final thread calculation
        int numThreads = (int) (2 * cores * disks * cpuUsagePct * (1 + c));
        // clamp the max threads between 1 and MAX_THREADS

        numThreads = Math.max(1, numThreads);
        if (maxThreads > 0) {
            numThreads = Math.min(maxThreads, numThreads);
        }

        return numThreads;
    }
}
