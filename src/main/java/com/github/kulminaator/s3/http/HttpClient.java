package com.github.kulminaator.s3.http;

import java.io.IOException;

public interface HttpClient {
    HttpResponse makeRequest(HttpRequest request) throws IOException;
}
