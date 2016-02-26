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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;

import com.hubcap.task.state.TaskMode;

public class BaseModel {

    private List<Object> aggregateData = Collections.synchronizedList(new ArrayList<Object>());

    private Map<String, Object> aggregateDataMap;

    public BaseModel() {
        // this insures we can set
        aggregateDataMap = Collections.synchronizedMap(new HashMap<String, Object>());
    }

    public void aggregate(Object moreData) {
        synchronized (aggregateData) {
            if (aggregateData.contains(moreData) == false) {
                aggregateData.add(moreData);
            }
        }
    }

    public void aggregateForKey(String key, Object moreData) {
        synchronized (aggregateDataMap) {
            if (aggregateDataMap.containsKey(key) == false) {
                aggregateDataMap.put(key, moreData);
            }
        }
    }

    public Object getAggregatedDataForKey(String key) {
        synchronized (aggregateDataMap) {
            return aggregateDataMap.get(key);
        }
    }

    public List<Object> getAggregateData() {
        return Collections.unmodifiableList(aggregateData);
    }

    public Map<String, Object> getAggregateDataMap() {
        return Collections.unmodifiableMap(aggregateDataMap);
    }

    public Object calculate() {
        return null;
    }

}
