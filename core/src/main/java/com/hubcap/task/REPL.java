package com.hubcap.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
import java.util.concurrent.Callable;

import com.hubcap.Constants;
import com.hubcap.HubCap;
import com.hubcap.process.ProcessModel;
import com.hubcap.utils.ThreadUtils;

/**
 * The Read-Eval-Print-Loop for HubCap
 * 
 * @author Decoded4620 2016
 */
public class REPL implements Runnable {

    private HubCap instance;

    public REPL(HubCap instance) {
        this.instance = instance;
    }

    enum ParseStates {
        NONE,
        INARG,
        INQARG,
        COMPLETE,
        ERROR
    }

    /**
     * Process input from the command line, an build the arguments much the same
     * way that that the command line would do. Valid argument formats are
     * "arg", "--opt", arg, --opt, -Dkey=value, -Dkey="quoted value",
     * "quoted value"
     * 
     * @param input
     *            a String of 'arguments' for the command line. Separated by a
     *            space (unless within a quoted value)
     * @return a <code>String[]</code> of arguments to pass to a TaskRunner for
     *         processing.
     */
    public static String[] processREPLInput(String input) {

        String[] argList;
        ArrayList<String> args = new ArrayList<String>();

        // insure no leading / trailing spaces.
        input = input.trim();
        int charIdx = 0;
        StringBuilder currArgBuilder = null;

        // null chars to start
        char currChar = '\0';
        char prevChar = '\0';

        ParseStates pState = ParseStates.NONE;

        while (charIdx < input.length() && pState != ParseStates.ERROR) {

            if (charIdx > 0) {
                prevChar = currChar;
            }

            currChar = input.charAt(charIdx);

            // start off with the next character
            if (pState == ParseStates.NONE) {
                if (currChar != ' ') {
                    pState = ParseStates.INARG;
                }
            }

            // this means we're parsing an argument
            // but its not 'quoted' argument
            if (pState == ParseStates.INARG) {
                if (currArgBuilder == null) {
                    currArgBuilder = new StringBuilder();
                }

                // check illegal
                if (currChar == '\'') {
                    pState = ParseStates.ERROR;
                }
                if (currChar == '"') {
                    pState = ParseStates.INQARG;
                    currArgBuilder.append(currChar);
                } else if (currChar == ' ') {
                    // end of arg
                    args.add(currArgBuilder.toString());
                    currArgBuilder = null;
                    pState = ParseStates.NONE;
                } else {
                    currArgBuilder.append(currChar);
                }
            }

            // we're currently processing a quoted portion of an argument
            else if (pState == ParseStates.INQARG) {
                if (currChar == '"') {
                    // ignore escaped quotes
                    if (prevChar != '\\') {
                        currArgBuilder.append(currChar);
                        pState = ParseStates.INARG;
                    } else {
                        currArgBuilder.append(currChar);
                    }
                } else {
                    currArgBuilder.append(currChar);
                }
            }

            charIdx++;
        }

        // add the final arg
        if (currArgBuilder != null) {
            String arg = currArgBuilder.toString();
            if (arg != null && !arg.isEmpty()) {
                args.add(currArgBuilder.toString());
            }
        }
        pState = ParseStates.COMPLETE;

        argList = new String[args.size()];
        args.toArray(argList);

        return argList;
    }

    public void shutdown() {
        if (scanner != null) {
            scanner.shutdown();
            scanner = null;

            if (myThread != null) {
                ThreadUtils.fold(myThread, false, ProcessModel.instance().getVerbose());
            }
        }
    }

    Evaluator scanner = new Evaluator();

    Thread myThread;

    @Override
    public void run() {

        myThread = Thread.currentThread();
        while (true) {

            try {
                Object line = scanner.call();
                System.out.println("line: " + line.toString().trim());
                if (instance.processArgs(processREPLInput(line.toString())) == 1) {
                    break;
                }
            } catch (Exception e) {
                break;
            }
        }
    }

    public class Evaluator implements Callable<String> {

        private boolean running = true;

        public void shutdown() {
            running = false;
        }

        public void start() {
            running = true;
        }

        public String call() throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String input = null;
            do {
                try {
                    // wait until we have data to complete a readLine()
                    while (!br.ready() && running) {
                        Thread.sleep(Constants.IDLE_TIME);
                        if (!running) {
                            break;
                        }
                    }

                    if (!running) {
                        break;
                    }

                    input = br.readLine();

                } catch (InterruptedException e) {
                    return null;
                }
            } while ("".equals(input));

            return input;
        }
    }

}
