package com.github.kulminaator.s3.http;

import com.github.kulminaator.s3.http.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Implementing the bare minimum to create a http client good enough to access aws s3.
 */
public class PicoHttpClient implements HttpClient {

    private static final int BLOCK_SIZE = 16 * 1024;
    private final boolean debug;

    public PicoHttpClient() {this(false);}

    public PicoHttpClient(boolean debug) {
        this.debug = true;
    }

    @Override
    public HttpResponse makeGetRequest(final String urlString, final Map<String, String> headers) throws IOException {
        // request is prepared
        final URL url = new URL(urlString);

        this.debug("Request to " + url.toExternalForm());

        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        if (headers != null) {
            for(final String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.get(key));
            }
        }

        // request is made
        final int responseCode = connection.getResponseCode();

        if (responseCode < 200 || responseCode > 299) {
            throw new IllegalStateException("Unexpected http code " + responseCode);
        }

        final HttpResponse response = new HttpResponse();
        response.setHttpCode(connection.getResponseCode());
        response.setHeaders(connection.getHeaderFields());

        final byte[] bytes = this.readDataToBytes(connection.getInputStream());
        response.setBody(bytes);

        this.debug("Response to " + new String(bytes));

        connection.disconnect();

        return response;
    }

    private void debug(String s) {
        if (this.debug) {
            System.out.println(DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()) + " [PICO_HTTP] " + s);
        }
    }

    private byte[] readDataToBytes(InputStream input) throws IOException {
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
        return bytes;
    }
}
