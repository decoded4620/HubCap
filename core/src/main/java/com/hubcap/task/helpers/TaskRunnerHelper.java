package com.hubcap.task.helpers;

import java.util.Properties;

import com.hubcap.task.TaskModel;
import com.hubcap.task.TaskRunner;

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

public class TaskRunnerHelper implements Runnable {

    // object to handle events from this search helper
    protected SearchHelperListener listener;

    // the task model
    protected TaskModel taskModel;

    protected Properties opts;

    // the task runner which owns this task helper
    protected TaskRunner owner;

    public TaskRunnerHelper(TaskRunner owner) {

        this.owner = owner;
        this.taskModel = this.owner.getTaskModel();

        opts = taskModel.getCommandLine().getOptionProperties("D");
        // TODO Auto-generated constructor stub
    }

    public void setListener(SearchHelperListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

    public void die() {
        this.listener = null;
        this.owner = null;
        this.taskModel = null;
    }

}
