package com.github.kulminaator.s3;

import com.github.kulminaator.s3.http.HttpClient;
import com.github.kulminaator.s3.http.HttpResponse;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        when(this.httpClient.makeGetRequest(any(), any())).thenReturn(this.buildResponseOf("object data here"));

        //when
        String result = client.getObjectDataAsString("my-bucket", "my-object-folder/my-object");

        //then
        String expectedPath = "https://s3-elbonia-central-1.amazonaws.com/my-bucket/my-object-folder/my-object";
        Map<String, String> expectedParams = new HashMap<>();
        verify(this.httpClient, times(1)).makeGetRequest(expectedPath, expectedParams);

        assertEquals(result, "object data here");
    }



    @Test
    public void fetches_objects_listing() throws IOException {
        // given
        Client client = this.buildClient();
        when(this.httpClient.makeGetRequest(any(), any())).thenReturn(
                this.buildResponseOfResource("s3_response_content.xml"));

        //when
        List<S3Object> objectList = client.listObjects("my-bucket");

        //then
        String expectedPath = "https://s3-elbonia-central-1.amazonaws.com/my-bucket?list-type=2";
        Map<String, String> expectedParams = new HashMap<>();
        verify(this.httpClient, times(1)).makeGetRequest(expectedPath, expectedParams);

        assertTrue(objectList.size() > 0);
        assertTrue(objectList.stream().anyMatch(o -> o.getKey().equals("text_data_demo.txt")));
        assertTrue(objectList.stream().anyMatch(o -> o.getKey().equals("binary_data_demo.png")));

        S3Object pngObject = objectList.stream().filter(o -> o.getKey().equals("binary_data_demo.png"))
                .findFirst().get();

        assertEquals(pngObject.getETag(), "\"4e4d609b8d37347fcff94f20543e1d0e\"");
        assertEquals(pngObject.getSize(), Long.valueOf(14463L));
        assertEquals(pngObject.getLastModified(), "2018-09-23T10:34:17.000Z");
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