package com.github.kulminaator.s3.auth;

import com.github.kulminaator.s3.http.HttpRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Auth header calculation class. See details from here:
 *
 * https://docs.aws.amazon.com/general/latest/gr/sigv4-add-signature-to-request.html
 * https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-auth-using-authorization-header.html
 *
 * Also i think amazon people are all over the place with the whole messy if-or-not-then-what-is-even-going-on-approach.
 */
public class PicoSignatureCalculator {

    private Clock clock = Clock.systemUTC();

    public PicoSignatureCalculator() {
    }

    /**
     * Constructor used in time related unit tests only.
     */
    PicoSignatureCalculator(Clock presetClock) {
        this.clock = presetClock;
    }

    public void addSignatureHeaderForRequest(HttpRequest request, CredentialsProvider credentialsProvider) {
        if (credentialsProvider == null) {
            return;
        }
        this.addSignatureHeader(request, credentialsProvider);
    }

    private void addSignatureHeader(HttpRequest request, CredentialsProvider credentialsProvider) {
        final Instant now = this.clock.instant();
        final String date = this.getFormattedDate(now);
        final String dateTime = this.getFormattedDateTime(now);

        this.addRequiredHeaders(request, dateTime, credentialsProvider);

        final String accessKey = credentialsProvider.getAccessKeyId();
        final String secretAccessKey = credentialsProvider.getSecretAccessKey();

        final String scope = date + "/" + request.getRegion() + "/s3/aws4_request";

        final String canonical = this.getCanonicalRequest(request);

        System.out.println("*** Canonical is : \n" + canonical + "//END");

        final String stringToSign = this.getStringToSign(dateTime, scope, canonical);

        System.out.println("*** <> *** String to sign : \n" + stringToSign + "//END");

        final byte[] dateKey = hmacSha256(date, "AWS4"+secretAccessKey);
        final byte[] dateRegionKey = hmacSha256(request.getRegion(), dateKey);
        final byte[] dateRegionServiceKey = hmacSha256("s3", dateRegionKey);
        final byte[] signingKey = hmacSha256("aws4_request", dateRegionServiceKey);

        final byte[] signature = hmacSha256(stringToSign.getBytes(StandardCharsets.UTF_8), signingKey);

        final String hexSignature = this.hex(signature);

        final StringBuilder authHeaderContent = new StringBuilder();
        final String signedHeaders = this.getSignedHeaders(request);
        authHeaderContent.append("AWS4-HMAC-SHA256 ")
                .append("Credential=").append(accessKey)
                .append("/").append(date).append("/").append(request.getRegion())
                .append("/s3/aws4_request").append(",")
                .append("SignedHeaders=").append(signedHeaders)
                .append(",").append("Signature=").append(hexSignature);

        request.setHeader("Authorization", authHeaderContent.toString());
    }

    private void addRequiredHeaders(HttpRequest request, String dateTime, CredentialsProvider credentialsProvider) {
        request.setHeader("Host", request.getHost());
        request.setHeader("x-amz-date", dateTime);
        request.setHeader("x-amz-content-sha256", this.sha256(request.getBody()));
        if (credentialsProvider.getSessionToken() != null) {
            request.setHeader("x-amz-security-token", credentialsProvider.getSessionToken());
        }
    }

    private String getSignedHeaders(HttpRequest request) {
        final Map<String, String> canonicalHeaders = this.getCanonicalHeaders(request);
        return String.join(";", canonicalHeaders.keySet());
    }

    private String getStringToSign(String timestampString, String scope, String canonical) {
        return "AWS4-HMAC-SHA256" + "\n" +
                timestampString + "\n" +
                scope + "\n" +
                this.sha256(canonical.getBytes(StandardCharsets.UTF_8));
    }

    protected String getCanonicalRequest(HttpRequest request) {
        final StringBuilder builder = new StringBuilder();
        builder.append(request.getMethod()).append("\n")
            .append(request.getPath()).append("\n");
        if (request.getParams() != null) {
            builder.append(request.getParams()).append("\n");
        } else {
            builder.append("\n");
        }

        final Map<String, String> canonicalHeaders = this.getCanonicalHeaders(request);
        for (final Map.Entry<String, String> cHeader : canonicalHeaders.entrySet()) {
            builder.append(cHeader.getKey());
            builder.append(":");
            builder.append(cHeader.getValue());
            builder.append("\n");
        }
        builder.append("\n")
            .append(String.join(";", canonicalHeaders.keySet())).append("\n")
            .append(sha256(request.getBody()));

        return builder.toString();
    }


    private byte[] hmacSha256(String data, byte[] key) {
        return this.hmacSha256(data.getBytes(StandardCharsets.UTF_8), key);
    }


    private byte[] hmacSha256(String data, String key) {
        return this.hmacSha256(data.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] hmacSha256(byte[] data, byte[] key) {
        try {
            final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            final SecretKeySpec secret_key = new SecretKeySpec(key, "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return sha256_HMAC.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Platform not sane, missing sha256 or failing to construct key", e);
        }
    }

    private byte[] rawSha256(byte[] body) {
        final MessageDigest digest = getSha256Digest();
        byte[] digested = digest.digest(body);
        return digested;
    }

    private String sha256(byte[] body) {
        byte[] digested = this.rawSha256(body);
        return this.hex(digested);
    }

    private String hex(byte[] data) {
        final StringBuilder hexString = new StringBuilder();
        for (final byte rawByte : data) {
            hexString.append(String.format("%02x", rawByte & 0XFF));
        }
        return hexString.toString().toLowerCase();
    }

    private MessageDigest getSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Platform doesn't support SHA-256", e);
        }
    }

    private Map<String, String> getCanonicalHeaders(HttpRequest request) {
        final TreeMap<String, String> map = new TreeMap<>();
        for (Map.Entry<String, List<String>> e : request.getHeaders().entrySet()) {
            final String value = String.join(";", e.getValue()).replaceAll(" +", " ");
            map.put(e.getKey().toLowerCase().trim(), value);
        }
        return map;
    }

    private String getFormattedDate(Instant instant) {
        return instant.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private String getFormattedDateTime(Instant instant) {
        return instant.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    }
}
