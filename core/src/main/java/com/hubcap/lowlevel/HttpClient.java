package com.hubcap.lowlevel;

import java.io.EOFException;

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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Map;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.Base64;
import com.hubcap.utils.ErrorUtils;

public class HttpClient {

    private HttpTransport transport;

    /**
     * CTOR attempts to instantiate a new Google Transport, and throws any
     * encountered exceptions
     * 
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public HttpClient() {
        transport = new HttpTransport();
    }

    public boolean verbose = false;

    private ParsedHttpResponse readResponse(HttpResponse response) throws IOException {

        ParsedHttpResponse parsedResponse = new ParsedHttpResponse(response);
        return parsedResponse;
    }

    public ParsedHttpResponse headAuthorizedRequest(String reqUrl, String un, String pwd, Map<String, String> headers) throws IOException {
        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildHeadRequest();
        request.url = url;

        request.headers.setBasicAuthentication(un, pwd);

        if (headers != null) {
            for (String key : headers.keySet()) {
                request.headers.set(key, headers.get(key));
            }
        }

        HttpResponse response = request.execute();
        ParsedHttpResponse r = readResponse(response);
        return r;
    }

    public ParsedHttpResponse safeHeadAuthorizedRequest(String headUrl, String un, String pwd, Map<String, String> headers) {
        try {
            return headAuthorizedRequest(headUrl, un, pwd, headers);
        } catch (IOException e) {
            ErrorUtils.printStackTrace(e);

            return null;
        } catch (Exception e) {
            ErrorUtils.printStackTrace(e);
            return null;
        }
    }

    public ParsedHttpResponse safeAuthorizedRequest(String reqUrl, String un, String pwd, Map<String, String> headers) {
        try {
            return getAuthorizedRequest(reqUrl, un, pwd, headers);
        } catch (IOException e) {
            ErrorUtils.printStackTrace(e);
            return null;
        }
    }

    public ParsedHttpResponse getAuthorizedRequest(String reqUrl, String un, String pwd, Map<String, String> headers) throws IOException {
        //
        if (verbose) {
            System.out.println("getAuthorizedRequest(" + reqUrl + ", " + un + ", %password hidden%)");
        }

        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildGetRequest();
        request.url = url;

        request.headers.setBasicAuthentication(un, pwd);

        if (headers != null) {
            for (String key : headers.keySet()) {
                request.headers.set(key, headers.get(key));
            }
        }

        HttpResponse response = request.execute();
        ParsedHttpResponse r = readResponse(response);
        return r;
    }

    /**
     * Builds and runs a get request
     * 
     * @param reqUrl
     * @return
     * @throws IOException
     */
    public ParsedHttpResponse getRequest(String reqUrl, Map<String, String> headers) throws IOException {

        if (verbose) {
            System.out.println("getRequest(" + reqUrl + ")");
        }
        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildGetRequest();
        request.url = url;

        HttpResponse response = request.execute();
        ParsedHttpResponse r = readResponse(response);
        return r;
    }

    public ParsedHttpResponse postAuthorizedRequest(String reqUrl, String postData, String un, String pwd, Map<String, String> headers) throws IOException {
        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildPostRequest();
        request.url = url;
        request.headers.setBasicAuthentication(un, pwd);
        HttpResponse response = request.execute();
        ParsedHttpResponse r = readResponse(response);
        return r;
    }

    public ParsedHttpResponse postRequest(String reqUrl, String postData, Map<String, String> headers) throws IOException {
        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildPostRequest();
        request.url = url;

        HttpResponse response = request.execute();
        ParsedHttpResponse r = readResponse(response);
        return r;
    }
}
