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
import java.util.Iterator;
import java.util.List;

import com.hubcap.task.model.TaskModel.TaskModelResult;
import com.hubcap.utils.ErrorUtils;

/**
 * The Top level Results Model. Aggregated for each new Command
 * 
 * @author Decoded4620 2016
 */
public class ResultsModel extends BaseModel {

    private List<TaskModel> taskModels = null;

    public ResultsModel() {
        taskModels = Collections.synchronizedList(new ArrayList<TaskModel>());
    }

    public TaskModel[] getTaskModels() {
        if (taskModels == null) {
            return new TaskModel[0];
        }

        TaskModel[] m = new TaskModel[taskModels.size()];
        return taskModels.toArray(m);
    }

    public void addTaskModel(TaskModel taskModel) {
        taskModels.add(taskModel);
    }

    @Override
    public Object calculate() {

        ProcessResults r = new ProcessResults();
        synchronized (taskModels) {
            ErrorUtils.printStackTrace(new Exception("ResultsModel::calculate()"));
            r.results = new TaskModelResult[taskModels.size()];

            Iterator<TaskModel> it = taskModels.iterator();

            int i = 0;
            while (it.hasNext()) {
                TaskModel t = it.next();
                if (r.results != null && t != null) {
                    r.results[i++] = (TaskModelResult) t.calculate();
                }
            }
        }

        return r;
    }

    public class ProcessResults {

        public TaskModelResult[] results;
    }
}
