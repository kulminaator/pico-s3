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

        HttpRequest request = new HttpRequest();
        request.setHeaders(headers);
        request.setProtocol(this.getS3HttpProtocol());
        request.setHost(this.getS3Host());
        request.setPath(this.getS3Path(bucket, prefix));
        request.setParams(params);
        request.setRegion(this.region);

        this.secureRequest(request);

        final HttpResponse response = this.httpClient.makeRequest(request);

        final Document s3ListingDocument = S3XmlParser.parseS3Xml(new String(response.getBody(),
                StandardCharsets.UTF_8));

        final List<S3Object> collectedList = S3XmlParser.parseObjectsFromXml(s3ListingDocument);
        return collectedList;
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
        return "/" + bucket + "/" + prefix;
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
