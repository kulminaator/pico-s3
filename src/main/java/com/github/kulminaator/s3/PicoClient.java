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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PicoClient implements Client {

    private boolean https;
    private final String region;
    private HttpClient httpClient;
    private CredentialsProvider credentialsProvier;

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
        final String params = "?list-type=2";

        HttpRequest request = new HttpRequest();
        request.setHeaders(headers);
        request.setProtocol(this.getS3HttpProtocol());
        request.setHost(this.getS3Host());
        request.setPath(this.getS3Path(bucket, prefix));
        request.setParams(params);

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

    private byte[] getObjectData(String bucket, String object) throws IOException {
        final Map<String,List<String>> headers = new HashMap<>();
        HttpRequest request = new HttpRequest();
        request.setHeaders(headers);
        request.setProtocol(this.getS3HttpProtocol());
        request.setHost(this.getS3Host());
        request.setPath(this.getS3Path(bucket, object));

        this.secureRequest(request);

        final HttpResponse response = this.httpClient.makeRequest(request);
        return response.getBody();
    }

    private void secureRequest(HttpRequest request) {
        final PicoSignatureCalculator calculator = new PicoSignatureCalculator();
        calculator.addSignatureHeaderForRequest(request, this.credentialsProvier);
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

    private void setCredentialsProvider(CredentialsProvider credentialsProvier) {
        this.credentialsProvier = credentialsProvier;
    }

    public static class Builder {
        private String region;
        private boolean https = true;
        private HttpClient httpClient = new PicoHttpClient();
        private CredentialsProvider credentialsProvier;

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

        public Builder withCredentialsProvider(CredentialsProvider credentialsProvier) {
            this.credentialsProvier = credentialsProvier;
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
            client.setCredentialsProvider(this.credentialsProvier);
            return client;
        }
    }

}
