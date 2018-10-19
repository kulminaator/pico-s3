package com.github.kulminaator.s3.auth;

import com.github.kulminaator.s3.http.HttpRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
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
    };

    public void addSignatureHeaderForRequest(HttpRequest request, CredentialsProvider credentialsProvider) {
        if (credentialsProvider == null) {
            return;
        }
        this.addSignatureHeader(request, credentialsProvider);
    }

    private String addSignatureHeader(HttpRequest request, CredentialsProvider credentialsProvider) {
        final String accessKey = credentialsProvider.getAccessKeyId();
        final String secretAccessKey = credentialsProvider.getSecretAccessKey();
        final String date = this.getFormattedDate();
        final String region = "region";
        final String service = "s3";
        final String v4Request = "/aws4_request";

        return null;
    }

    protected String getCanonicalRequest(HttpRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getMethod()).append("\n")
            .append(request.getPath()).append("\n")
            .append(request.getParams()).append("\n");
        Map<String, String> canonicalHeaders = this.getCanonicalHeaders(request);
        for (Map.Entry<String, String> cHeader : canonicalHeaders.entrySet()) {
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

    private String sha256(byte[] body) {
        final MessageDigest digest = getSha256Digest();
        byte[] digested = digest.digest(body);
        final StringBuilder hexString = new StringBuilder();
        for (byte rawByte : digested) {
            String hex = Integer.toHexString(rawByte & 0xFF);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
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
        TreeMap<String, String> map = new TreeMap<>();
        for (Map.Entry<String, List<String>> e : request.getHeaders().entrySet()) {
            final String value = String.join(";", e.getValue()).replaceAll(" +", " ");
            map.put(e.getKey().toLowerCase().trim(), value);
        }
        return map;
    }

    private String getFormattedDate() {
        return this.clock.instant().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
