package com.hubcap.task.helpers;

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

import java.util.Date;

import com.hubcap.Constants;
import com.hubcap.lowlevel.SewingMachine;
import com.hubcap.task.TaskRunner;

public class DebugSearchHelper extends DefaultSearchHelper {

    public static long debugWorkTime = Constants.FAKE_WORK_TIME;

    public static double debug_errorChance = 0.0D;

    public DebugSearchHelper(SewingMachine sewingMachine, TaskRunner owner) {
        super(sewingMachine, owner);
    }

    @Override
    public void run() {

        // try {
        // fake work here

        double random = Math.random();

        // X % of jobs will fail with severe error (to test this case)
        // if this flag has a value.
        // WARNING, ONLY USE THIS FLAG IN TESTING
        if (random < debug_errorChance) {
            boolean canRecoverFromError = random > debug_errorChance / 2;

            if (listener != null) {
                listener.processTaskHelperError(new Exception("Random Debug Failure"), canRecoverFromError);
            }

            return;
        } else {
            long now = new Date().getTime();
            int i = 0;
            int b = 0;
            int deltaMax = (int) (debugWorkTime);

            // TODO - DELETE THIS BEFORE SHIPPING!
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // !!!! DELETE ME START
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

            while ((new Date().getTime()) - now < deltaMax) {
                double x = 2 * 3 / 4 + 56 * 3.14150 * 5f;

                ++i;

                if (i % 200000 == 0) {
                    try {
                        b++;
                        Thread.sleep(Constants.TEST_WAIT_TIME * 4);

                        // Fake some task data here
                        FakeTaskData fdata = new FakeTaskData();
                        fdata.setFakeData(b * i * x);
                        if (listener != null) {
                            listener.processTaskHelperData(fdata);
                        }
                    } catch (InterruptedException e) {

                        if (listener != null) {
                            listener.processTaskHelperError(e, true);
                        }

                        break;
                    }
                }
            }
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // !!!! DELETE ME END
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        }
    }

    // TODO - DELETE THIS BEFORE SHIPPING!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!! DELETE ME START
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    class FakeTaskData {

        private double data = 0.0D;

        public FakeTaskData() {
        }

        public void setFakeData(double fakeData) {
            this.data = fakeData;
        }

        public double getFakeData() {
            return this.data;
        }
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!! DELETE ME END
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
}
