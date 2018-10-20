package com.github.kulminaator.s3.auth;

import com.github.kulminaator.s3.http.HttpClient;
import com.github.kulminaator.s3.http.HttpRequest;
import com.github.kulminaator.s3.http.HttpResponse;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InstanceCredentialsProviderTest {

    @Test
    public void will_load_credentials() throws IOException {
        // given
        HttpClient customClient = mock(HttpClient.class);
        InstanceCredentialsProvider provider = new InstanceCredentialsProvider(customClient);

        HttpResponse response = new HttpResponse();
        response.setBody(expectedAmazonResponse().getBytes(StandardCharsets.UTF_8));
        when(customClient.makeRequest(any())).thenReturn(response);

        //when
        String accessKeyId = provider.getAccessKeyId();
        String secretKey = provider.getSecretAccessKey();
        String token = provider.getSessionToken();

        //then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(customClient, times(1)).makeRequest(captor.capture());

        assertEquals("169.254.169.254", captor.getValue().getHost());
        assertEquals("http", captor.getValue().getProtocol());
        assertEquals("/latest/meta-data/iam/security-credentials/", captor.getValue().getPath());
        assertNull(null, captor.getValue().getParams());


        assertEquals(accessKeyId, "expected-key-id");
        assertEquals(secretKey, "expected-secret-key-value-be-here");
        assertEquals(token, "expected-long-long-token-text");

    }

    @Test
    public void expiredDataIsReloaded() throws IOException {
        // given
        HttpClient customClient = mock(HttpClient.class);
        InstanceCredentialsProvider provider = new InstanceCredentialsProvider(customClient);

        HttpResponse expiredResponse = new HttpResponse();
        expiredResponse.setBody(expiredAmazonResponse().getBytes(StandardCharsets.UTF_8));
        HttpResponse goodResponse = new HttpResponse();
        goodResponse.setBody(expectedAmazonResponse().getBytes(StandardCharsets.UTF_8));

        when(customClient.makeRequest(any())).thenReturn(expiredResponse, goodResponse);

        //when
        String accessKeyId = provider.getAccessKeyId();
        String secretKey = provider.getSecretAccessKey();
        String token = provider.getSessionToken();

        //then

        verify(customClient, times(2)).makeRequest(any());

        // our first request returned expired response
        // imagine it happened in the past a long time ago,
        // so yes we expect right now this value key id to be expired
        assertEquals(accessKeyId, "expired-access-key");
        // but the rest of the data was reloaded from an up to date body
        assertEquals(secretKey, "expected-secret-key-value-be-here");
        assertEquals(token, "expected-long-long-token-text");
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