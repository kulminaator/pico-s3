package com.github.kulminaator.s3.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequest {
    private String method = "GET";
    private String protocol;
    private String host;
    private String path;
    private String params;
    private byte[] body = new byte[0];
    private Map<String, List<String>> headers = new HashMap<>();
    private String region;
    private int connectTimeout;
    private int readTimeout;

    public HttpRequest() {
    }

    public void setBody(byte[] bytes) {
        this.body = bytes;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        HashMap<String, List<String>> safeHeaders = new HashMap<>();
        safeHeaders.putAll(headers);
        this.headers = safeHeaders;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return this.body;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * Set a header to one exact value.
     * @param key The key.
     * @param value The value.
     */
    public void setHeader(String key, String value) {
        this.headers.put(key, Collections.singletonList(value));
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
