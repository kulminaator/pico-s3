package com.github.kulminaator.s3.auth;

import com.github.kulminaator.s3.http.HttpClient;
import com.github.kulminaator.s3.http.HttpRequest;
import com.github.kulminaator.s3.http.HttpResponse;
import com.github.kulminaator.s3.http.PicoHttpClient;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class InstanceCredentialsProviderTest {

    @Test
    public void will_load_credentials() throws IOException {
        // given
        HttpClient customClient = mock(HttpClient.class);
        InstanceCredentialsProvider provider = new InstanceCredentialsProvider(customClient);


        HttpResponse roleData = new HttpResponse();
        roleData.setBody("my-role-name".getBytes(StandardCharsets.UTF_8));
        HttpResponse response = new HttpResponse();
        response.setBody(expectedAmazonResponse().getBytes(StandardCharsets.UTF_8));
        when(customClient.makeRequest(any())).thenReturn(roleData, response);

        //when
        String accessKeyId = provider.getAccessKeyId();
        String secretKey = provider.getSecretAccessKey();
        String token = provider.getSessionToken();

        //then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(customClient, times(2)).makeRequest(captor.capture());

        List<HttpRequest> allRequests = captor.getAllValues();
        HttpRequest firstRequest = allRequests.get(0);
        HttpRequest secondRequest = allRequests.get(1);

        assertEquals("169.254.169.254", firstRequest.getHost());
        assertEquals("http", firstRequest.getProtocol());
        assertEquals("/latest/meta-data/iam/security-credentials/", firstRequest.getPath());
        assertNull(null, firstRequest.getParams());

        assertEquals("169.254.169.254", secondRequest.getHost());
        assertEquals("http", secondRequest.getProtocol());
        assertEquals("/latest/meta-data/iam/security-credentials/my-role-name", secondRequest.getPath());
        assertNull(null, secondRequest.getParams());

        assertEquals(accessKeyId, "expected-key-id");
        assertEquals(secretKey, "expected-secret-key-value-be-here");
        assertEquals(token, "expected-long-long-token-text");

    }

    @Test
    public void expiredDataIsReloaded() throws IOException {
        // given
        HttpClient customClient = mock(HttpClient.class);
        InstanceCredentialsProvider provider = new InstanceCredentialsProvider(customClient);

        HttpResponse roleData = new HttpResponse();
        roleData.setBody("my-role-name".getBytes(StandardCharsets.UTF_8));
        HttpResponse expiredResponse = new HttpResponse();
        expiredResponse.setBody(expiredAmazonResponse().getBytes(StandardCharsets.UTF_8));
        HttpResponse goodResponse = new HttpResponse();
        goodResponse.setBody(expectedAmazonResponse().getBytes(StandardCharsets.UTF_8));

        when(customClient.makeRequest(any())).thenReturn(roleData, expiredResponse, goodResponse);

        //when
        String accessKeyId = provider.getAccessKeyId();
        String secretKey = provider.getSecretAccessKey();
        String token = provider.getSessionToken();

        //then

        verify(customClient, times(4)).makeRequest(any());

        // our first request returned expired response
        // imagine it happened in the past a long time ago,
        // so yes we expect right now this value key id to be expired
        assertEquals(accessKeyId, "expired-access-key");
        // but the rest of the data was reloaded from an up to date body
        assertEquals(secretKey, "expected-secret-key-value-be-here");
        assertEquals(token, "expected-long-long-token-text");
    }

    /**
     * Only run this test in an ec2 instance :)
     */
    @Test
    public void realEc2Test() throws Exception {
        HttpClient realClient = new PicoHttpClient();

        if (this.noEnv(realClient)) {
            System.out.println("Real ec2 instance profile test skipped");
            assertTrue("Skipped", true);
            return;
        }

        InstanceCredentialsProvider provider = new InstanceCredentialsProvider(realClient);

        assertNotNull(provider.getAccessKeyId());
        assertNotNull(provider.getSecretAccessKey());
        assertNotNull(provider.getSessionToken());

        System.out.println("Real ec2 instance profile test passed");
    }

    private boolean noEnv(HttpClient realClient) throws Exception {
        try {
            HttpRequest request = new HttpRequest();
            request.setProtocol("http");
            request.setHost("169.254.169.254");
            request.setPath("/latest/");
            request.setConnectTimeout(1000);
            request.setReadTimeout(1000);

            return realClient.makeRequest(request).getBody().length < 1;
        } catch (Exception e) {
            // not running in an ec2 instance, no point to validate the test
            return true;
        }
    }

    private void showBeginning(final String data) {
        System.out.println(data.substring(1, 8) + "....");
    }

    private String expectedAmazonResponse() {
        return "{\n" +
                "  \"Code\" : \"Success\",\n" +
                "  \"LastUpdated\" : \"2012-04-26T16:39:16Z\",\n" +
                "  \"Type\" : \"AWS-HMAC\",\n" +
                "  \"AccessKeyId\" : \"expected-key-id\",\n" +
                "  \"SecretAccessKey\" : \"expected-secret-key-value-be-here\",\n" +
                "  \"Token\" : \"expected-long-long-token-text\",\n" +
                "  \"Expiration\" : \"2037-05-17T15:09:54Z\"\n" +
                "}";
    }



    private String expiredAmazonResponse() {
        return "{\n" +
                "  \"Code\" : \"Success\",\n" +
                "  \"LastUpdated\" : \"2012-04-26T16:39:16Z\",\n" +
                "  \"Type\" : \"AWS-HMAC\",\n" +
                "  \"AccessKeyId\" : \"expired-access-key\",\n" +
                "  \"SecretAccessKey\" : \"expired-secret-key\",\n" +
                "  \"Token\" : \"expired-token\",\n" +
                "  \"Expiration\" : \"2013-05-17T15:09:54Z\"\n" +
                "}";
    }
}