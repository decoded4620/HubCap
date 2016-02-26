package com.hubcap.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.cli.CommandLine;

import com.hubcap.task.model.BaseModel;
import com.hubcap.task.state.TaskMode;

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

public class TaskModel extends BaseModel {

    private TaskMode taskMode = TaskMode.NONE;

    private CommandLine cli = null;

    private String userName = null;

    private String userPassOrToken = null;

    /**
     * CTOR
     * 
     * @param username
     * @param passwordOrToken
     */
    public TaskModel(String username, String passwordOrToken) {
        super();
        this.userPassOrToken = passwordOrToken;
        this.userName = username;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getPasswordOrToken() {
        return this.userPassOrToken;
    }

    public void setTaskMode(TaskMode mode) {
        this.taskMode = mode;
    }

    public TaskMode getTaskMode() {
        return taskMode;
    }

    public void setCommandLine(CommandLine cli) {
        this.cli = cli;
    }

    public CommandLine getCommandLine() {
        return this.cli;
    }
}
