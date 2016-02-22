package com.hubcap;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/*
 * #%L
 * HubCap
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

import org.junit.Test;

public class HubCapTest {

    @BeforeClass
    public static void setup() {
        Assert.assertEquals(true, true);
    }

    @Test
    public void defaultTest() {

        String[] args = {
            "decoded4620",
            "10"
        };

        boolean caught = false;
        try {
            HubCap.main(args);
        } catch (Exception e) {
            caught = true;
        }
        Assert.assertEquals(caught, false);

        try {
            HubCap.main(new String[0]);
        } catch (Exception e) {

            caught = true;
        }

        Assert.assertEquals(caught, true);
    }

    @AfterClass
    public static void tearDown() {
        Assert.assertEquals(true, true);
    }
}
