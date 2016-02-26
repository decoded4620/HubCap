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
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Map;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.Base64;

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

    private String readResponse(HttpResponse response) throws IOException {

        StringBuilder b = new StringBuilder();
        InputStream iStream = response.getContent();
        int ch;
        while ((ch = iStream.read()) != -1) {
            b.append((char) ch);
        }

        String r = b.toString();

        return r;
    }

    public String getAuthorizedRequest(String reqUrl, String un, String pwd, Map<String, String> headers) throws IOException {
        //
        System.out.println("Req Url:" + reqUrl + ", " + un + ", " + pwd);
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
        String r = readResponse(response);
        return r;
    }

    /**
     * Builds and runs a get request
     * 
     * @param reqUrl
     * @return
     * @throws IOException
     */
    public String getRequest(String reqUrl, Map<String, String> headers) throws IOException {

        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildGetRequest();
        request.url = url;

        HttpResponse response = request.execute();
        String r = readResponse(response);
        System.out.println("url: " + reqUrl + ", response: " + response.statusCode + " - " + r.length() + " characters");
        return r;
    }

    public String postAuthorizedRequest(String reqUrl, String postData, String un, String pwd, Map<String, String> headers) throws IOException {
        // String authString = un + ":" + pwd;
        // System.out.println("auth string: " + authString);
        // byte[] authEncBytes = Base64.encode(authString.getBytes());
        // String authStringEnc = new String(authEncBytes);
        // System.out.println("Base64 encoded auth string: " + authStringEnc);
        //
        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildPostRequest();
        request.url = url;
        request.headers.setBasicAuthentication(un, pwd);
        HttpResponse response = request.execute();
        String r = readResponse(response);
        return r;
    }

    public String postRequest(String reqUrl, String postData, Map<String, String> headers) throws IOException {
        GenericUrl url = new GenericUrl(reqUrl);
        HttpRequest request = transport.buildPostRequest();
        request.url = url;

        HttpResponse response = request.execute();
        String r = readResponse(response);
        return r;
    }
}
