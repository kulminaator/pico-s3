package com.github.kulminaator.s3.auth;

import com.github.kulminaator.s3.http.HttpRequest;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
    public void adds_auth_header_with_signature_without_session_token() {
        /*
            GET ?lifecycle HTTP/1.1
            Host: examplebucket.s3.amazonaws.com
            Authorization: SignatureToBeCalculated
            x-amz-date: 20130524T000000Z
            x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
         */
        // given

        Clock clock = Clock.fixed(Instant.parse("2018-09-08T01:02:03Z"), ZoneOffset.UTC);
        final PicoSignatureCalculator calculator = new PicoSignatureCalculator(clock);
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
        assertTrue(request.getHeaders().containsKey("Authorization"));

        assertEquals("20180908T010203Z", request.getHeaders().get("x-amz-date").get(0));

        String expected = "AWS4-HMAC-SHA256 Credential=this is secret/20180908/us-east-1/s3/aws4_request," +
                "SignedHeaders=host;x-amz-content-sha256;x-amz-date," +
                "Signature=37e0b9723178cac53737200f71fe889e5f6fe8e3e76fb857fa0a775fe212e563";

        assertEquals(expected, request.getHeaders().get("Authorization").get(0));
    }


    @Test
    public void adds_auth_header_with_signature_with_session_token() {
        /*
            GET ?lifecycle HTTP/1.1
            Host: examplebucket.s3.amazonaws.com
            Authorization: SignatureToBeCalculated
            x-amz-date: 20130524T000000Z
            x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
         */
        // given

        Clock clock = Clock.fixed(Instant.parse("2018-09-08T01:02:03Z"), ZoneOffset.UTC);
        final PicoSignatureCalculator calculator = new PicoSignatureCalculator(clock);
        final HttpRequest request = new HttpRequest();
        request.setHost("examplebucket.s3.amazonaws.com");
        request.setRegion("us-east-1");
        request.setPath("");
        request.setParams("?lifecycle");
        request.setMethod("GET");
        request.setProtocol("https");

        // when
        calculator.addSignatureHeaderForRequest(request, this.getSimpleCredentialsProvider(true));

        //then
        assertTrue(request.getHeaders().containsKey("x-amz-content-sha256"));
        assertTrue(request.getHeaders().containsKey("Authorization"));

        assertEquals("20180908T010203Z", request.getHeaders().get("x-amz-date").get(0));

        /*
        expected:<...nt-sha256;x-amz-date[,Signature=37e0b9723178cac53737200f71fe889e5f6fe8e3e76fb857fa0a775fe212e563]> but was:<...nt-sha256;x-amz-date[;x-amz-security-token,Signature=51256ed4de0f0dd43e432b89894ff22717ebd836ce98ee47fba9723054675ad2]>

         */

        String expected = "AWS4-HMAC-SHA256 Credential=this is secret/20180908/us-east-1/s3/aws4_request," +
                "SignedHeaders=host;x-amz-content-sha256;x-amz-date;x-amz-security-token," +
                "Signature=51256ed4de0f0dd43e432b89894ff22717ebd836ce98ee47fba9723054675ad2";

        assertEquals(expected, request.getHeaders().get("Authorization").get(0));
    }


    private CredentialsProvider getSimpleCredentialsProvider() {
        return getSimpleCredentialsProvider(false);
    }

    private CredentialsProvider getSimpleCredentialsProvider(boolean withSessionToken) {
        SimpleCredentialsProvider provider = new SimpleCredentialsProvider();
        provider.setAccessKeyId("this is secret");
        provider.setSecretAccessKey("this is secret");
        if (withSessionToken) {
            provider.setSessionToken("this a session token");
        }
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