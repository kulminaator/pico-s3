package com.github.kulminaator.s3.http;

import java.io.IOException;
import java.util.Map;

public interface HttpClient {
    HttpResponse makeRequest(HttpRequest request) throws IOException;
}
