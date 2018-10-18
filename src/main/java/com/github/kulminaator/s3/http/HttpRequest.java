package com.github.kulminaator.s3.http;

import java.util.List;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String protocol;
    private String host;
    private String path;
    private String params;
    private byte[] body;
    private Map<String, List<String>> headers;

    public HttpRequest() {
    }

    public void setBody(byte[] bytes) {
        this.body = bytes;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
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
}
