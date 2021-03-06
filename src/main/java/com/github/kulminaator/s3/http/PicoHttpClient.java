package com.github.kulminaator.s3.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementing the bare minimum to create a http client good enough to access aws s3.
 */
public class PicoHttpClient implements HttpClient {

    private static final int BLOCK_SIZE = 16 * 1024;
    private final boolean debug;

    public PicoHttpClient() {this(false);}

    public PicoHttpClient(boolean debug) {
        this.debug = debug;
    }

    @Override
    public HttpResponse makeRequest(HttpRequest request) throws IOException {
        final String urlString = this.buildUrlString(request);
        final Map <String, String> headers = this.remapHeaders(request.getHeaders());

        // request is prepared
        final URL url = new URL(urlString);

        this.debug(() -> "Request to " + url.toExternalForm());

        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(request.getConnectTimeout());
        connection.setReadTimeout(request.getReadTimeout());
        connection.setRequestMethod(request.getMethod());
        this.debug(() -> "Sending headers" + headers);

        if (headers != null) {
            for(final String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.get(key));
                this.debug(() -> "Header " + key + ": " + headers.get(key));
            }
        }

        final HttpResponse response = new HttpResponse();
        // request is being made now
        if (request.getBody().length > 0) {
            connection.setDoOutput(true);
            this.writeBytesToStream(request.getBody(), connection.getOutputStream());
        }

        try {
            final int responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode > 299) {
                final byte[] bytes = this.readDataToBytes(connection.getInputStream());
                this.debug(() -> String.format("Response: '%s'", new String(bytes, StandardCharsets.UTF_8)));
                throw new IllegalStateException("Unexpected http code " + responseCode);
            }

            response.setHttpCode(responseCode);
            response.setHeaders(connection.getHeaderFields());

            final byte[] bytes = this.readDataToBytes(connection.getInputStream());
            response.setBody(bytes);
            this.debug(() -> "Response to " + new String(bytes));
        } catch (IOException exception) {
            final byte[] bytes = this.readDataToBytes(connection.getErrorStream());
            this.debug(() -> String.format("Response: '%s'", new String(bytes, StandardCharsets.UTF_8)));
            throw new IllegalStateException("Unexpected http result (" +
                    exception.getMessage() + ") with response body '" + new String(bytes) + "'", exception);
        } finally {
            connection.disconnect();
        }


        return response;
    }

    private void writeBytesToStream(byte[] body, OutputStream outputStream) throws IOException {
        try {
            outputStream.write(body);
        } finally {
            outputStream.close();
        }
    }

    private Map<String, String> remapHeaders(Map<String, List<String>> headers) {
        final Map<String, String> map = new HashMap<>();
        for (final Map.Entry<String, List<String>> header : headers.entrySet()) {
            map.put(header.getKey(), String.join(";", header.getValue()));
        }
        return map;
    }

    private String buildUrlString(HttpRequest request) {
        final StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(request.getProtocol())
                .append("://")
                .append(request.getHost())
                .append(request.getPath());
        if (request.getParams() != null) {
                pathBuilder.append("?");
                pathBuilder.append(request.getParams());
        }
        return pathBuilder.toString();
    }

    private void debug(java.util.function.Supplier<String> debugMessageProvider) {
        if (this.debug) {
            System.out.println(DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())
                    + " [PICO_HTTP] " + debugMessageProvider.get());
        }
    }

    private byte[] readDataToBytes(InputStream input) throws IOException {
        int read = 0;
        byte[] buffer = new byte[BLOCK_SIZE];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (input != null) {
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
