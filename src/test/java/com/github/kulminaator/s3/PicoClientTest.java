package com.github.kulminaator.s3;

import com.github.kulminaator.s3.http.HttpClient;
import com.github.kulminaator.s3.http.HttpRequest;
import com.github.kulminaator.s3.http.HttpResponse;
import com.github.kulminaator.s3.options.PutObjectOptions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PicoClientTest {


    private HttpClient httpClient;

    @Test
    public void fetches_object_data() throws IOException {
        // given
        Client client = this.buildClient();
        when(this.httpClient.makeRequest(any())).thenReturn(this.buildResponseOf("object data here"));

        //when
        String result = client.getObjectDataAsString("my-bucket", "my-object-folder/my-object");

        //then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(this.httpClient, times(1)).makeRequest(captor.capture());

        assertEquals("my-bucket.s3-elbonia-central-1.amazonaws.com", captor.getValue().getHost());
        assertEquals("https", captor.getValue().getProtocol());
        assertEquals("/my-object-folder/my-object", captor.getValue().getPath());
        assertNull(null, captor.getValue().getParams());


        assertEquals("object data here", result);
    }

    @Test
    public void fetches_object_data_as_stream() throws IOException {
        final int connectTimeout = 25;
        final int readTimeout = 55;

        // given
        Client client = this.buildClient(connectTimeout, readTimeout);
        when(this.httpClient.makeRequest(any())).thenReturn(this.buildResponseOf("streamed object data here"));

        //when
        InputStream resultStream = client.getObjectDataAsInputStream("my-bucket", "my-object-folder/my-object");
        ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();
        int read = 0;
        while (read >= 0) {
            final byte[] buffer = new byte[2048];
            read = resultStream.read(buffer);
            if (read > -1) {
                dataBuffer.write(buffer, 0, read);
            }
        }
        String stringResult = new String(dataBuffer.toByteArray(), StandardCharsets.UTF_8);

        //then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(this.httpClient, times(1)).makeRequest(captor.capture());

        assertEquals("my-bucket.s3-elbonia-central-1.amazonaws.com", captor.getValue().getHost());
        assertEquals("https", captor.getValue().getProtocol());
        assertEquals("/my-object-folder/my-object", captor.getValue().getPath());
        assertNull(null, captor.getValue().getParams());

        assertEquals(connectTimeout, captor.getValue().getConnectTimeout());
        assertEquals(readTimeout, captor.getValue().getReadTimeout());

        assertEquals("streamed object data here", stringResult);
    }



    @Test
    public void fetches_objects_listing() throws IOException {
        // given
        Client client = this.buildClient();
        when(this.httpClient.makeRequest(any())).thenReturn(
                this.buildResponseOfResource("s3_response_content.xml"));

        //when
        List<S3Object> objectList = client.listObjects("my-bucket");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(this.httpClient, times(1)).makeRequest(captor.capture());

        assertEquals("my-bucket.s3-elbonia-central-1.amazonaws.com", captor.getValue().getHost());
        assertEquals("https", captor.getValue().getProtocol());
        assertEquals("/", captor.getValue().getPath());
        assertEquals("list-type=2", captor.getValue().getParams());
        assertEquals("GET", captor.getValue().getMethod());
        assertEquals(PicoClient.DEFAULT_CONNECT_TIMEOUT, captor.getValue().getConnectTimeout());
        assertEquals(PicoClient.DEFAULT_READ_TIMEOUT, captor.getValue().getReadTimeout());

        assertTrue(objectList.size() > 0);
        assertTrue(objectList.stream().anyMatch(o -> o.getKey().equals("text_data_demo.txt")));
        assertTrue(objectList.stream().anyMatch(o -> o.getKey().equals("binary_data_demo.png")));

        S3Object pngObject = objectList.stream().filter(o -> o.getKey().equals("binary_data_demo.png"))
                .findFirst().orElseThrow(() -> new IllegalStateException("expected record not found"));

        assertEquals(pngObject.getETag(), "\"4e4d609b8d37347fcff94f20543e1d0e\"");
        assertEquals(pngObject.getSize(), Long.valueOf(14463L));
        assertEquals(pngObject.getLastModified(), "2018-09-23T10:34:17.000Z");
    }

    @Test
    public void can_upload_files() throws Exception {
        // given
        Client client = this.buildClient();
        when(this.httpClient.makeRequest(any())).thenReturn(
                this.buildResponseOf("ok"));

        //when
        client.putObject("my-bucket", "my-object", "test-data".getBytes(StandardCharsets.UTF_8), "text/plain");

        // then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(this.httpClient, times(1)).makeRequest(captor.capture());


        assertEquals("my-bucket.s3-elbonia-central-1.amazonaws.com", captor.getValue().getHost());
        assertEquals("https", captor.getValue().getProtocol());
        assertEquals("/my-object", captor.getValue().getPath());
        assertEquals("PUT", captor.getValue().getMethod());
        assertEquals("text/plain", captor.getValue().getHeaders().get("Content-Type").get(0));
        assertEquals("9", captor.getValue().getHeaders().get("Content-Length").get(0));
        assertArrayEquals("test-data".getBytes(), captor.getValue().getBody());
    }

    @Test
    public void can_upload_sse_encrypted_files() throws Exception {
        // given
        Client client = this.buildClient();
        when(this.httpClient.makeRequest(any())).thenReturn(
                this.buildResponseOf("ok"));

        //when
        PutObjectOptions options = new PutObjectOptions.Builder()
                .withContentType("text/html")
                .withServerSideEncryption(PutObjectOptions.Builder.SERVER_SIDE_ENCRYPTION_S3)
                .build();
        client.putObject("my-bucket", "my-object", "test-data".getBytes(StandardCharsets.UTF_8), options);

        // then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(this.httpClient, times(1)).makeRequest(captor.capture());


        assertEquals("my-bucket.s3-elbonia-central-1.amazonaws.com", captor.getValue().getHost());
        assertEquals("https", captor.getValue().getProtocol());
        assertEquals("/my-object", captor.getValue().getPath());
        assertEquals("PUT", captor.getValue().getMethod());
        assertEquals("text/html", captor.getValue().getHeaders().get("Content-Type").get(0));
        assertEquals("9", captor.getValue().getHeaders().get("Content-Length").get(0));


        assertEquals("AES256", captor.getValue().getHeaders().get("x-amz-server-side-encryption").get(0));

        assertArrayEquals("test-data".getBytes(), captor.getValue().getBody());
    }

    @Test
    public void can_upload_sse_kms_encrypted_files() throws Exception {
        // given
        Client client = this.buildClient();
        when(this.httpClient.makeRequest(any())).thenReturn(
                this.buildResponseOf("ok"));

        //when
        PutObjectOptions options = new PutObjectOptions.Builder()
                .withContentType("text/html")
                .withServerSideEncryption(PutObjectOptions.Builder.SERVER_SIDE_ENCRYPTION_KMS)
                .withServerSideEncryptionKeyId("abc-123")
                .build();
        client.putObject("my-bucket", "my-object", "test-data".getBytes(StandardCharsets.UTF_8), options);

        // then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(this.httpClient, times(1)).makeRequest(captor.capture());


        assertEquals("my-bucket.s3-elbonia-central-1.amazonaws.com", captor.getValue().getHost());
        assertEquals("https", captor.getValue().getProtocol());
        assertEquals("/my-object", captor.getValue().getPath());
        assertEquals("PUT", captor.getValue().getMethod());
        assertEquals("text/html", captor.getValue().getHeaders().get("Content-Type").get(0));
        assertEquals("9", captor.getValue().getHeaders().get("Content-Length").get(0));

        assertEquals("aws:kms", captor.getValue().getHeaders().get("x-amz-server-side-encryption").get(0));
        assertEquals("abc-123", captor.getValue().getHeaders().get("x-amz-server-side-encryption-aws-kms-key-id").get(0));

        assertArrayEquals("test-data".getBytes(), captor.getValue().getBody());
    }


    @Test
    public void can_handle_unicode_objects_for_get() throws IOException {
        // given
        Client client = this.buildClient();
        when(this.httpClient.makeRequest(any())).thenReturn(this.buildResponseOf("unicode-object-content"));

        //when
        String result = client.getObjectDataAsString("my-bucket", "my-Öbject-folder/käsehauš");

        //then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(this.httpClient, times(1)).makeRequest(captor.capture());

        assertEquals("GET", captor.getValue().getMethod());
        assertEquals("my-bucket.s3-elbonia-central-1.amazonaws.com", captor.getValue().getHost());
        assertEquals("https", captor.getValue().getProtocol());
        assertEquals("/my-%C3%96bject-folder/k%C3%A4sehau%C5%A1", captor.getValue().getPath());
        assertNull(null, captor.getValue().getParams());

        assertEquals("unicode-object-content", result);
    }

    @Test
    public void can_handle_paginated_object_lists() throws IOException {
        // given
        Client client = this.buildClient();

        when(this.httpClient.makeRequest(any())).thenReturn(
                this.buildResponseOfResource("pagination_s3_response_content_truncated.xml"),
                this.buildResponseOfResource("pagination_s3_response_content_truncated.xml"),
                this.buildResponseOfResource("pagination_s3_response_content_final.xml")
        );

        //when
        List<S3Object> result = client.listObjects("my-bucket", "my-object-folder/s€cret-subfolder");

        //then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(this.httpClient, times(3)).makeRequest(captor.capture());

        List<HttpRequest> requestsMade = captor.getAllValues();
        assertEquals("list-type=2&prefix=my-object-folder%2Fs%E2%82%ACcret-subfolder", requestsMade.get(0).getParams());
        assertEquals("" +
                "continuation-token=14A3Bj7%2F8L49hvCZhqecpzT5OMIu7FwVz483Lmh3zo2HCC0JjlHwTWYZIoYV4%2BAo1&list-type=2" +
                "&prefix=my-object-folder%2Fs%E2%82%ACcret-subfolder", requestsMade.get(1).getParams());
        assertEquals("" +
                "continuation-token=14A3Bj7%2F8L49hvCZhqecpzT5OMIu7FwVz483Lmh3zo2HCC0JjlHwTWYZIoYV4%2BAo1&list-type=2" +
                "&prefix=my-object-folder%2Fs%E2%82%ACcret-subfolder", requestsMade.get(2).getParams());

        assertEquals(result.size(), 5);

    }

    @Test
    public void can_fetch_object_information() throws Exception {
        // given
        Client client = this.buildClient();
        HttpResponse objectInfo = new HttpResponse();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Length", Collections.singletonList("1025"));
        headers.put("Content-Type", Collections.singletonList("application/xml"));
        headers.put("ETag", Collections.singletonList("\"i-dont-know-why-this-is-quoted\""));
        headers.put("Last-Modified", Collections.singletonList("Sun, 21 Oct 2018 07:39:00 GMT"));

        objectInfo.setHeaders(headers);
        objectInfo.setHttpCode(200);
        when(this.httpClient.makeRequest(any())).thenReturn(objectInfo);

        //when
        S3Object result = client.getObject("my-bucket", "my-object");

        //then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(this.httpClient, times(1)).makeRequest(captor.capture());

        HttpRequest request = captor.getValue();
        assertEquals("HEAD", request.getMethod());
        assertEquals("my-bucket.s3-elbonia-central-1.amazonaws.com", request.getHost());
        assertEquals("https", request.getProtocol());
        assertEquals("/my-object", request.getPath());
        assertNull(null, captor.getValue().getParams());

        assertEquals(Long.valueOf(1025L), result.getSize());
        assertEquals("application/xml", result.getContentType());
        assertEquals("my-object", result.getKey());
        assertEquals("\"i-dont-know-why-this-is-quoted\"", result.getETag());

    }

    private Client buildClient() {
        this.httpClient = mock(HttpClient.class);

        final Client picoClient = new PicoClient.Builder()
                .withHttps()
                .withRegion("elbonia-central-1")
                .withHttpClient(this.httpClient)
                .build();
        return picoClient;
    }


    private Client buildClient(int connectTimeout, int readTimeout) {
        this.httpClient = mock(HttpClient.class);

        final Client picoClient = new PicoClient.Builder()
                .withHttps()
                .withRegion("elbonia-central-1")
                .withHttpClient(this.httpClient)
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout)
                .build();
        return picoClient;
    }

    private HttpResponse buildResponseOf(String objectData) {
        HttpResponse response = new HttpResponse();
        response.setBody(objectData.getBytes());
        return response;
    }

    private HttpResponse buildResponseOfResource(String resourceName) throws IOException {
        final Path path = Paths.get(this.getClass().getClassLoader().getResource(resourceName).getPath());
        final String data = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return buildResponseOf(data);
    }
}
