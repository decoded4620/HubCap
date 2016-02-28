package com.hubcap.inputs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;

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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.hubcap.Constants;
import com.hubcap.HubCap;
import com.hubcap.lowlevel.HubCapOptionsParser;
import com.hubcap.process.ProcessModel;
import com.hubcap.task.model.TaskModel;
import com.hubcap.task.state.TaskMode;
import com.hubcap.utils.ErrorUtils;

/**
 * processes the input arguments and populates the TaskModel based on these
 * 
 * @author Decoded4620 2016
 */
public class CLIOptionBuilder {

    // original inputs and opions from the user
    private static CommandLineParser parser = new HubCapOptionsParser();

    private static Options availableOptions = null;

    private static String helpContent;

    private static void buildAvailableOptions() {

        availableOptions = new Options();

        Path p = Paths.get("resources/help", "help.txt");
        String helpStr = "help is on the way...check the readme!";

        if (helpContent == null) {
            try {
                helpContent = new String(Files.readAllBytes(p));
            } catch (IOException ex) {
                helpContent = helpStr;
                System.err.println("Help File Couldn't be loaded! " + p.toString());

                if (ProcessModel.instance().getVerbose())
                    ErrorUtils.printStackTrace(ex);
            }
        }

        availableOptions.addOption("h", "help", false, helpContent);
        availableOptions.addOption("v", "verbose", false, "Be verbose");

        availableOptions.addOption("r", "no-repl", false, "DONT fallback to repl mode");
        availableOptions.addOption("s", "shutdown-after", false, "Shutdown after results are complete");
        availableOptions.addOption("m", "mode", true, "Set the task mode, valid settings are 'search', 'deepsearch', 'watch', 'debug', default is 'search'");
        availableOptions.addOption("q", "quiet", true, "Be quiet (cancels verbose)");

        Option property = Option.builder("D").hasArgs().valueSeparator('=').build();
        availableOptions.addOption(property);
    }

    public static TaskModel buildInputOptionsModel(String[] args) throws ParseException {

        synchronized (parser) {
            if (availableOptions == null) {
                buildAvailableOptions();
            }
            CommandLine cmd = null;

            try {
                cmd = parser.parse(availableOptions, args);
            } catch (ParseException ex) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("How to use", availableOptions);
                throw ex;
            }

            // check for some default globals to be available within the
            // provided
            // options and setup the ProcessModel prior to running the task.
            if (cmd.hasOption("r")) {
                ProcessModel.instance().setREPLFallback(false);
            } else {
                // default to 'true' if no flag is passed.
                // this gives the user the chance to pass false to force a
                // shutdown.
                ProcessModel.instance().setREPLFallback(true);
            }

            if (cmd.hasOption("h")) {
                // automatically generate the help statement
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("How to use", availableOptions);

                return null;
            }

            // check for some default globals to be available within the
            // provided
            // options and setup the ProcessModel prior to running the task.
            if (cmd.hasOption("v")) {
                ProcessModel.instance().setVerbose(true);
            }

            String[] argv = cmd.getArgs();

            for (int i = 0; i < argv.length; i++) {
                String arg = argv[i];
                if (arg.equals(Constants.CMD_EXIT) || arg.equals(Constants.CMD_BYE) || arg.equals(Constants.CMD_DIE) || arg.equals(Constants.CMD_QUIT)) {
                    System.out.println("Exit called from a command!");
                    HubCap.instance().shutdown();
                    return null;
                }

                if (arg.equals(Constants.CMD_AUTH)) {

                    if (argv.length > i + 2) {
                        System.out.println("Authorizing User for this session: ");
                        ProcessModel.instance().auth(argv[i + 1], argv[i + 2]);
                    } else {
                        System.out.println("Cannot authorize, args are malformed, expected username and password");
                    }
                }
            }

            TaskModel model = new TaskModel();

            model.setCommandLine(cmd);

            // set the task mode
            String modeStr = cmd.getOptionValue("mode");
            if (modeStr == null) {
                model.setTaskMode(TaskMode.SEARCH);
            } else {
                switch (modeStr) {
                    case "debug":
                        model.setTaskMode(TaskMode.DEBUG);
                        break;
                    case "search":
                        model.setTaskMode(TaskMode.SEARCH);
                        break;
                    case "deepsearch":
                        model.setTaskMode(TaskMode.DEEP_SEARCH);
                        break;

                    case "watch":
                        model.setTaskMode(TaskMode.SCAVENGER);
                        break;
                }
            }
            return model;
        }
    }

}
