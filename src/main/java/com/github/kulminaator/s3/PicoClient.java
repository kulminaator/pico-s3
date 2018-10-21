package com.github.kulminaator.s3;

import com.github.kulminaator.s3.auth.CredentialsProvider;
import com.github.kulminaator.s3.auth.PicoSignatureCalculator;
import com.github.kulminaator.s3.http.HttpClient;
import com.github.kulminaator.s3.http.HttpRequest;
import com.github.kulminaator.s3.http.HttpResponse;
import com.github.kulminaator.s3.http.PicoHttpClient;
import com.github.kulminaator.s3.xml.S3XmlParser;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PicoClient implements Client {

    private boolean https;
    private final String region;
    private HttpClient httpClient;
    private CredentialsProvider credentialsProvider;

    private PicoClient(String region) {
        this.region = region;
    }

    @Override
    public S3Object getObject(String bucket, String object) {
        /* just request the object over https://s3-eu-west-1.amazonaws.com/bucket/object*/
        return null;
    }

    @Override
    public List<S3Object> listObjects(String bucket) throws IOException {
        return this.listObjects(bucket, null);
    }

    @Override
    public List<S3Object> listObjects(String bucket, String prefix) throws IOException {
        /*make a url request to  https://s3-eu-west-1.amazonaws.com/bucket/?list-type=2&start-after=prefix */
        final Map<String,List<String>> headers = new HashMap<>();
        final String params = "list-type=2";

        boolean hasMorePages = true;
        final List<S3Object> finalList = new ArrayList<>();
        String continuation = null;

        while (hasMorePages) {

            final StringBuilder paramsBuilder = new StringBuilder();
            paramsBuilder.append(params);

            if (continuation != null) {
                paramsBuilder.append("&continuation-token=");
                paramsBuilder.append(uriEncode(continuation));
            }
            if (prefix != null) {
                paramsBuilder.append("&prefix=");
                paramsBuilder.append(uriEncode(prefix, false));
            }

            HttpRequest request = new HttpRequest();
            request.setHeaders(headers);
            request.setProtocol(this.getS3HttpProtocol());
            request.setHost(this.getS3Host());
            request.setPath(this.getS3Path(bucket, null));
            request.setParams(paramsBuilder.toString());
            request.setRegion(this.region);

            this.secureRequest(request);

            final HttpResponse response = this.httpClient.makeRequest(request);

            final Document s3ListingDocument = S3XmlParser.parseS3Xml(new String(response.getBody(),
                    StandardCharsets.UTF_8));

            final List<S3Object> collectedList = S3XmlParser.parseObjectsFromXml(s3ListingDocument);
            finalList.addAll(collectedList);

            continuation = S3XmlParser.getNextContinuationToken(s3ListingDocument);
            hasMorePages = continuation != null;
        }

        return finalList;
    }

    @Override
    public InputStream getObjectDataAsInputStream(String bucket, String object) throws IOException {
        return new ByteArrayInputStream(this.getObjectData(bucket, object));
    }

    @Override
    public String getObjectDataAsString(String bucket, String object) throws IOException {
        return new String(this.getObjectData(bucket, object), StandardCharsets.UTF_8);
    }

    @Override
    public void putObject(String bucket, String object, byte[] data, String contentType) throws IOException {
        final Map<String,List<String>> headers = new HashMap<>();

        HttpRequest request = new HttpRequest();
        request.setMethod("PUT");
        request.setHeaders(headers);
        request.setProtocol(this.getS3HttpProtocol());
        request.setHost(this.getS3Host());
        request.setPath(this.getS3Path(bucket, object));
        request.setRegion(this.region);
        request.setBody(data);

        headers.put("Content-Type", Collections.singletonList(contentType));
        headers.put("Content-Length", Collections.singletonList( String.valueOf(data.length)));

        this.secureRequest(request);

        this.httpClient.makeRequest(request);
    }

    private byte[] getObjectData(String bucket, String object) throws IOException {
        final Map<String,List<String>> headers = new HashMap<>();
        HttpRequest request = new HttpRequest();
        request.setHeaders(headers);
        request.setProtocol(this.getS3HttpProtocol());
        request.setHost(this.getS3Host());
        request.setPath(this.getS3Path(bucket, object));
        request.setRegion(this.region);

        this.secureRequest(request);

        final HttpResponse response = this.httpClient.makeRequest(request);
        return response.getBody();
    }

    private void secureRequest(HttpRequest request) {
        final PicoSignatureCalculator calculator = new PicoSignatureCalculator();
        calculator.addSignatureHeaderForRequest(request, this.credentialsProvider);
    }

    private String getS3Host() {
        final StringBuilder builder = new StringBuilder();
        builder.append("s3-");
        builder.append(this.region);
        builder.append(".amazonaws.com");
        return builder.toString();
    }

    private String getS3HttpProtocol() {
        if (this.https) {
            return "https";
        } else {
            return "http";
        }
    }

    private String getS3Path(String bucket, String prefix) {
        if (prefix == null) {
            return "/" + bucket;
        }
        return "/" + bucket + "/" + uriEncode(prefix, false);
    }

    private void setHttps(final boolean https) {
        this.https = https;
    }

    private void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private void setCredentialsProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }


    /**
     * Encodes uri components for http safety, also encodes slashes.
     * Slightly modified code from amazon's example on their web page in authorization part.
     * @param input The input string.
     */
    protected static String uriEncode(CharSequence input) {
        return uriEncode(input, true);
    }

    /**
     * Encodes uri components for http safety.
     * Slightly modified code from amazon's example on their web page in authorization part.
     * @param input The input string.
     * @param encodeSlash Should slash be encoded or not.
     */
    protected static String uriEncode(CharSequence input, boolean encodeSlash) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '~' || ch == '.') {
                result.append(ch);
            } else if (ch == '/') {
                result.append(encodeSlash ? "%2F" : ch);
            } else {
                result.append(toUrlHexUTF8(ch));
            }
        }
        return result.toString();
    }

    /**
     * Turns the char into utf8 string, grabs it's bytes in utf8 shape and encodes them one by one with url syntax.
     * @param ch The char to encode.
     * @return The encoded data.
     */
    protected static String toUrlHexUTF8(char ch) {
        final byte[] raw = ("" + ch).getBytes(StandardCharsets.UTF_8);
        final StringBuilder hexString = new StringBuilder();
        for (final byte rawByte : raw) {
            hexString.append("%");
            hexString.append(String.format("%02X", rawByte & 0XFF));
        }
        return hexString.toString().toLowerCase();
    }

    public static class Builder {
        private String region;
        private boolean https = true;
        private HttpClient httpClient = new PicoHttpClient();
        private CredentialsProvider credentialsProvider;

        public Builder() {}

        public Builder withHttp() {
            this.https = false;
            return this;
        }

        public Builder withHttps() {
            this.https = true;
            return this;
        }

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder withCredentialsProvider(CredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        public Builder withHttpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public PicoClient build() {
            final PicoClient client = new PicoClient(this.region);
            client.setHttps(this.https);
            client.setHttpClient(this.httpClient);
            client.setCredentialsProvider(this.credentialsProvider);
            return client;
        }
    }

}
