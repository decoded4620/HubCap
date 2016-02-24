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

public class ErrorUtils {

    public static void printStackTrace(Exception e) {
        if (e != null) {

            System.err.print(e.getClass().getName() + "::");
            System.err.println((e.getMessage() == null || e.getMessage().equals("")) ? e.toString() : e.getMessage());
            StackTraceElement[] elements = e.getStackTrace();

            for (int i = 0; i < elements.length; i++) {
                StackTraceElement element = elements[i];

                System.err.println("    - (at " + element.getClassName() + ":" + element.getLineNumber() + ")");
            }
        }
    }

    /**
     * Wait safely, returns true if wait finished normally. Returns false
     * otherwise. Blocking.
     * 
     * @param factory
     * @return
     */
    public static boolean safeWait(Object factory) {
        try {
            factory.wait();
            return true;
        } catch (InterruptedException ex) {
            return false;
        }
    }

    /**
     * Wrapper for <code>Thread.sleep</code> that makes it safe.
     * 
     * @param millis
     *            a <code>long</code>
     */
    public static boolean safeSleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException ex) {
            ErrorUtils.printStackTrace(ex);
            return false;
        }
    }
}
