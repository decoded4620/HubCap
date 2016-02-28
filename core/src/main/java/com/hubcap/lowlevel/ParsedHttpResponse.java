package com.hubcap.lowlevel;

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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.http.HttpResponse;

public class ParsedHttpResponse {

    private String content = null;

    private Map<String, Object> headers = null;

    private String contentType;

    private String contentEncoding;

    private boolean isSuccessStatusCode;

    public ParsedHttpResponse(HttpResponse response) throws IOException {

        StringBuilder b = new StringBuilder();
        InputStream iStream;

        try {
            iStream = response.getContent();
        } catch (EOFException e) {
            iStream = null;
        }

        int ch;
        b.append("");

        if (iStream != null) {
            while ((ch = iStream.read()) != -1) {
                b.append((char) ch);
            }
        }

        this.content = b.toString();
        this.contentType = response.contentType;
        this.contentEncoding = response.contentEncoding;
        this.headers = new HashMap<String, Object>();
        this.isSuccessStatusCode = response.isSuccessStatusCode;
        for (String key : response.headers.keySet()) {
            this.headers.put(key, response.headers.get(key));
        }
    }

    public Boolean getIsSuccessStatusCode() {
        return this.isSuccessStatusCode;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getContentEncoding() {
        return this.contentEncoding;
    }

    public String getContent() {
        return this.content;
    }

    public Object getHeader(String key) {
        return this.headers.get(key);
    }

    public Map<String, Object> getHeaders() {
        return Collections.unmodifiableMap(this.headers);
    }
}
