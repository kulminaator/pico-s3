package com.github.kulminaator.s3.http;

import java.io.IOException;

public interface HttpClient {
    /**
     * Make a simple request (probably against S3) as pointed out in the details of HttpRequest class.
     * @param request The request to perform.
     * @return The resulting http response.
     * @throws IOException In case communication fails.
     */
    HttpResponse makeRequest(HttpRequest request) throws IOException;
}
