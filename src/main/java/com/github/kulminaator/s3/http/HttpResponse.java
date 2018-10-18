package com.github.kulminaator.s3.http;

import java.util.List;
import java.util.Map;

public class HttpResponse {
    private byte[] body;
    private int httpCode;
    private Map<String, List<String>> headers;

    public HttpResponse() {
    }

    public void setBody(byte[] bytes) {
        this.body = bytes;
    }

    public void setHttpCode(int responseCode) {
        this.httpCode = responseCode;
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

    public int getHttpCode() {
        return httpCode;
    }
}
