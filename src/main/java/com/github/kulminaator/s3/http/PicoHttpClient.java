package com.github.kulminaator.s3.http;

import com.github.kulminaator.s3.http.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Implementing the bare minimum to create a http client good enough to access aws s3.
 */
public class PicoHttpClient implements HttpClient {

    private static final int BLOCK_SIZE = 16 * 1024;

    public PicoHttpClient() {}

    @Override
    public HttpResponse makeGetRequest(final String urlString, final Map<String, String> headers) throws IOException {
        final URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int responseCode = connection.getResponseCode();
        connection.setRequestMethod("GET");

        if (headers != null) {
            for(String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.get(key));
            }
        }

        HttpResponse response = new HttpResponse();
        response.setHttpCode(connection.getResponseCode());
        response.setHeaders(connection.getHeaderFields());

        InputStream input = connection.getInputStream();
        int read = 0;
        byte[] buffer = new byte[BLOCK_SIZE];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            read = input.read(buffer);
            if (read < 0) {
                break;
            }
            baos.write(buffer, 0 , read);
        }
        byte[] bytes = baos.toByteArray();

        response.setBody(bytes);

        connection.disconnect();

        return response;
    }
}
