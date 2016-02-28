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
     * Stack trace for any number of exceptions
     * 
     * @param e
     * @return
     */
    public static String getStackTrace(Exception... e) {
        if (e == null) {
            e = new Exception[0];
            e[0] = new Exception("Debug ErrorUtils Excetion");
        }

        StringBuilder st = new StringBuilder();
        for (int i = 0; i < e.length; i++) {
            Exception ex = e[i];
            st.append(ex.getClass().getName() + "::");
            st.append((ex.getMessage() == null || ex.getMessage().equals("")) ? e.toString() : ex.getMessage() + "\n");
            StackTraceElement[] elements = ex.getStackTrace();

            for (int j = 0; j < elements.length; j++) {
                StackTraceElement element = elements[j];

                st.append("    - (at " + element.getClassName() + ":" + element.getLineNumber() + ")\n");
            }

            st.append("\n\n");
        }

        return st.toString();
    }

}
