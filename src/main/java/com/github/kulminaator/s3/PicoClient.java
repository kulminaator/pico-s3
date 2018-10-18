package com.github.kulminaator.s3;

import com.github.kulminaator.s3.http.HttpClient;
import com.github.kulminaator.s3.http.HttpResponse;
import com.github.kulminaator.s3.xml.S3XmlParser;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PicoClient implements Client {

    private boolean https;
    private final String region;
    private HttpClient httpClient;

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
        /*make a url request to  https://s3-eu-west-1.amazonaws.com/bucket/?list-type=2&start-after=top */
        final Map<String, String> headers = new HashMap<>();
        final String params = "?list-type=2";
        final HttpResponse response = httpClient.makeRequest(buildUrl(bucket, null) + params, headers);

        final Document s3ListingDocument = S3XmlParser.parseS3Xml(new String(response.getBody(),
                StandardCharsets.UTF_8));

        final List<S3Object> collectedList = S3XmlParser.parseObjectsFromXml(s3ListingDocument);
        return collectedList;
    }

    @Override
    public InputStream getObjectDataAsInputStream(String bucket, String object) throws IOException {
        final Map<String, String> headers = new HashMap<>();
        final HttpResponse response = httpClient.makeRequest(buildUrl(bucket, object), headers);
        return new ByteArrayInputStream(response.getBody());
    }

    @Override
    public String getObjectDataAsString(String bucket, String object) throws IOException {
        final Map<String, String> headers = new HashMap<>();
        final HttpResponse response = httpClient.makeRequest(buildUrl(bucket, object), headers);

        return new String(response.getBody(), StandardCharsets.UTF_8);
    }

    private String buildUrl(final String bucket, final String object) {
        final StringBuilder builder = new StringBuilder();
        if (this.https) {
            builder.append("https://");
        } else {
            builder.append("http://");
        }
        builder.append("s3-");
        builder.append(this.region);
        builder.append(".amazonaws.com/");
        builder.append(bucket);
        if (object != null) {

            builder.append("/");
            builder.append(object);
        }
        return builder.toString();
    }

    private void setHttps(final boolean https) {
        this.https = https;
    }

    private void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }


    public static class Builder {
        private String region;
        private boolean https = true;
        private HttpClient httpClient;

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

        public PicoClient build() {
            final PicoClient client = new PicoClient(this.region);
            client.setHttps(this.https);
            client.setHttpClient(httpClient);
            return client;
        }

        public Builder withHttpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }
    }

}
