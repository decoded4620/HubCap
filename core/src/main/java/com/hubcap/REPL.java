package com.hubcap;

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
import java.util.Scanner;

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
    public String[] processREPLInput(String input) {

        String[] argList;
        ArrayList<String> args = new ArrayList<String>();

        // insure no leading / trailing spaces.
        input = input.trim();
        int charIdx = 0;
        String currArg = null;
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

    @Override
    public void run() {

        Scanner sc = new Scanner(System.in);
        // REPL Loop if necessary
        while (sc.hasNextLine()) {

            String line = sc.nextLine().trim();
            if (instance.processArgs(processREPLInput(line)) == 1) {
                sc.close();
                break;
            }
        }
    }

}
