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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.hubcap.process.ProcessModel;
import com.hubcap.utils.ErrorUtils;

public class HttpClient {

    private HttpTransport transport;

    private boolean autoAuth = false;

    private String un = null;

    private String pwd = null;

    public boolean verbose = false;

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

    public void setIsAuthorizedClient(boolean enable, String... credentials) {
        if (verbose) {
            System.out.println("setIsAuthorizedClient(" + enable + ")");
        }
        if (enable) {

            autoAuth = true;
            if (credentials.length > 1) {
                un = credentials[0];
                pwd = credentials[1];

                if (verbose) {
                    System.out.println("user: " + un + " was authorized!");
                }
            }
        } else {
            autoAuth = false;
            un = null;
            pwd = null;
        }
    }

    public void copyAuth(HttpClient client) {
        if (un != null && pwd != null) {
            client.setIsAuthorizedClient(true, un, pwd);
        }
    }

    /**
     * Copy User Input headers to our HttpRequest object.
     * 
     * @param headers
     * @param request
     */
    private void copyHeaders(Map<String, String> headers, HttpRequest request) {

        if (headers != null && request != null) {
            if (ProcessModel.instance().getVerbose()) {
                System.out.println("HttpClient::copyHeaders()" + (headers == null ? "" : "- adding: " + headers.size() + " new headers"));
            }

            for (String key : headers.keySet()) {
                request.headers.set(key, headers.get(key));
            }
        }
    }

    private void setAuth(HttpHeaders headers, String userName, String pass) {
        if (headers.authorization == null) {
            headers.setBasicAuthentication(userName, pass);
        } else {
            if (verbose) {
                System.out.println("Already authorized header!");
            }
        }
    }

    /**
     * Read a ParsedHttpResponse response back from a HttpResponse
     * 
     * @param response
     *            HttpResponse from one of the request types.
     * @return ParsedHttpResponse.
     * @throws IOException
     */
    private ParsedHttpResponse readResponse(HttpResponse response) throws IOException {
        ParsedHttpResponse parsedResponse = new ParsedHttpResponse(response);
        return parsedResponse;
    }

    /**
     * Returns a ParsedHttpResponse for a headRequest (much like doing 'curl
     * -I') This is used when all we care about is the header. I.e. we want to
     * know the page we start and end on for paginated data and thats returned
     * in the header. This call will allow a quick setup for that value.
     * 
     * @param reqUrl
     *            The request url for which we want to download the headers.
     * @param headers
     * @return
     * @throws IOException
     */
    public ParsedHttpResponse headRequest(String reqUrl, Map<String, String> headers) throws IOException {
        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildHeadRequest();
        request.url = url;

        if (autoAuth) {
            setAuth(request.headers, un, pwd);
        }

        copyHeaders(headers, request);

        // if auto auth is set for HttpClient,
        // the user can just call 'getRequest' without having to pass new
        // parameters in each time.
        if (autoAuth) {
            if (request.headers.authorization == null) {
                request.headers.setBasicAuthentication(un, pwd);
            }
        }

        HttpResponse response = request.execute();
        ParsedHttpResponse r = readResponse(response);
        return r;
    }

    /**
     * Wrapper for head request that catches the exception and returns NULL on
     * failure. Prints stack trace if verbose mode enabled.
     * 
     * @param headUrl
     * @param headers
     * @return
     */
    public ParsedHttpResponse safeHeadRequest(String headUrl, Map<String, String> headers) {
        try {
            return headRequest(headUrl, headers);
        } catch (IOException e) {
            if (verbose) {
                ErrorUtils.printStackTrace(e);
            }
        } catch (Exception e) {
            if (verbose) {
                ErrorUtils.printStackTrace(e);
            }
        }
        return null;
    }

    /**
     * Performs an Authorized Head Request using the specified credentials.
     * 
     * @param reqUrl
     *            Request Url
     * @param un
     *            Authentication User Name
     * @param pwd
     *            Authenticated Password
     * @param headers
     *            Any additional headers which you plan to send
     * @return a ParsedHttpResponse.
     * @throws IOException
     */
    public ParsedHttpResponse headAuthorizedRequest(String reqUrl, String un, String pwd, Map<String, String> headers) throws IOException {
        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildHeadRequest();
        request.url = url;

        request.headers.setBasicAuthentication(un, pwd);
        copyHeaders(headers, request);

        HttpResponse response = request.execute();
        ParsedHttpResponse r = readResponse(response);
        return r;
    }

    /**
     * Performs a 'safe' authorized head request. If an error is thrown, it is
     * trapped here and null is returned. The stack is printed if verbose mode
     * enabled.
     * 
     * @param headUrl
     * @param un
     * @param pwd
     * @param headers
     * @return
     */
    public ParsedHttpResponse safeHeadAuthorizedRequest(String headUrl, String un, String pwd, Map<String, String> headers) {
        try {
            return headAuthorizedRequest(headUrl, un, pwd, headers);
        } catch (IOException e) {
            if (verbose) {
                ErrorUtils.printStackTrace(e);
            }
        } catch (Exception e) {
            if (verbose) {
                ErrorUtils.printStackTrace(e);
            }
        }
        return null;
    }

    /**
     * Peforms a safe get request.
     * 
     * @param reqUrl
     * @param un
     * @param pwd
     * @param headers
     * @return
     */
    public ParsedHttpResponse safeGetRequest(String reqUrl, Map<String, String> headers) {
        try {
            return getRequest(reqUrl, headers);
        } catch (IOException e) {

            if (verbose) {
                ErrorUtils.printStackTrace(e);
            }
            return null;
        }
    }

    /**
     * Peforms a safe 'authorized' get request.
     * 
     * @param reqUrl
     * @param un
     * @param pwd
     * @param headers
     * @return
     */
    public ParsedHttpResponse safeGetAuthorizedRequest(String reqUrl, String un, String pwd, Map<String, String> headers) {
        try {
            return getAuthorizedRequest(reqUrl, un, pwd, headers);
        } catch (IOException e) {

            if (verbose) {
                ErrorUtils.printStackTrace(e);
            }
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
        copyHeaders(headers, request);

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

        copyHeaders(headers, request);

        // if auto auth is set for HttpClient,
        // the user can just call 'getRequest' without having to pass new
        // parameters in each time.
        if (autoAuth) {
            setAuth(request.headers, un, pwd);
        }

        HttpResponse response = request.execute();
        ParsedHttpResponse r = readResponse(response);
        return r;
    }

    public ParsedHttpResponse postAuthorizedRequest(String reqUrl, String postData, String un, String pwd, Map<String, String> headers) throws IOException {
        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildPostRequest();
        request.url = url;
        request.headers.setBasicAuthentication(un, pwd);

        copyHeaders(headers, request);

        HttpResponse response = request.execute();
        ParsedHttpResponse r = readResponse(response);
        return r;
    }

    public ParsedHttpResponse postRequest(String reqUrl, String postData, Map<String, String> headers) throws IOException {
        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildPostRequest();
        request.url = url;

        copyHeaders(headers, request);

        // if auto auth is set for HttpClient,
        // the user can just call 'getRequest' without having to pass new
        // parameters in each time.
        if (autoAuth) {
            setAuth(request.headers, un, pwd);
        }

        HttpResponse response = request.execute();
        ParsedHttpResponse r = readResponse(response);
        return r;
    }
}
