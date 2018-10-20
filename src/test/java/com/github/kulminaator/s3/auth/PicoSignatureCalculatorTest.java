package com.github.kulminaator.s3.auth;

import com.github.kulminaator.s3.http.HttpRequest;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PicoSignatureCalculatorTest {


    @Test
    public void buildsCorrectCanonicalRequest() {
        //given
        final HttpRequest request = new HttpRequest();
        request.setMethod("GET");
        request.setHost("");
        request.setParams("Action=ListUsers&Version=2010-05-08");
        request.setPath("/");
        Map<String, List<String>> headers = new TreeMap<>();
        headers.put("Host", Collections.singletonList("iam.amazonaws.com"));
        headers.put("X-Amz-Date", Collections.singletonList("20150830T123600Z"));
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded; charset=utf-8"));
        request.setHeaders(headers);
        request.setBody(new byte[]{});
        final PicoSignatureCalculator calculator = new PicoSignatureCalculator();

        //when
        final String canonicalRequest = calculator.getCanonicalRequest(request);

        //then
        final String expectedRequest = this.getExpectedRequest();

        assertEquals(canonicalRequest, expectedRequest);
    }

    @Test
    public void adds_auth_header_with_signature() {
        /*
            GET ?lifecycle HTTP/1.1
            Host: examplebucket.s3.amazonaws.com
            Authorization: SignatureToBeCalculated
            x-amz-date: 20130524T000000Z
            x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
         */
        // given

        final PicoSignatureCalculator calculator = new PicoSignatureCalculator();
        final HttpRequest request = new HttpRequest();
        request.setHost("examplebucket.s3.amazonaws.com");
        request.setRegion("us-east-1");
        request.setPath("");
        request.setParams("?lifecycle");
        request.setMethod("GET");
        request.setProtocol("https");

        // when
        calculator.addSignatureHeaderForRequest(request, this.getSimpleCredentialsProvider());

        //then
        assertTrue(request.getHeaders().containsKey("x-amz-content-sha256"));
    }

    private CredentialsProvider getSimpleCredentialsProvider() {
        SimpleCredentialsProvider provider = new SimpleCredentialsProvider();
        provider.setAccessKeyId("this is secret");
        provider.setSecretAccessKey("this is secret");
        // no token on this simple credentials set
        return provider;
    }

    private String getExpectedRequest() {
        return "GET\n" +
                "/\n" +
                "Action=ListUsers&Version=2010-05-08\n" +
                "content-type:application/x-www-form-urlencoded; charset=utf-8\n" +
                "host:iam.amazonaws.com\n" +
                "x-amz-date:20150830T123600Z\n" +
                "\n" +
                "content-type;host;x-amz-date\n" +
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    }

}