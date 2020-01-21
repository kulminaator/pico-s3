package com.github.kulminaator.s3.auth;

import com.github.kulminaator.s3.http.HttpClient;
import com.github.kulminaator.s3.http.HttpRequest;
import com.github.kulminaator.s3.http.HttpResponse;
import com.github.kulminaator.s3.http.PicoHttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstanceCredentialsProvider implements CredentialsProvider {
    private String accessKeyId;
    private String sessionToken;
    private String secretAccessKey;
    private ZonedDateTime expiration = null;
    private boolean loaded;

    private HttpClient client = new PicoHttpClient();

    public InstanceCredentialsProvider() {}

    public InstanceCredentialsProvider(HttpClient customHttpClient) {
        this.client = customHttpClient;
    }


    @Override
    public String getAccessKeyId() {
        this.assureLoaded();
        return this.accessKeyId;
    }

    @Override
    public String getSecretAccessKey() {
        this.assureLoaded();
        return this.secretAccessKey;
    }

    @Override
    public String getSessionToken() {
        this.assureLoaded();
        return this.sessionToken;
    }

    private void assureLoaded() {
        if (!this.loaded || this.expired()) {
            this.loadFromHttp();
            loaded = true;
        }
    }

    private boolean expired() {
        if (this.expiration == null) {
            return true;
        }
        return Clock.systemDefaultZone().instant().compareTo(this.expiration.toInstant()) > 0;
    }

    /**
     * @throws IllegalStateException In case you are not really in an aws ec2 instance.
     */
    private void loadFromHttp() {
        try {
            String instanceRole = this.loadRoleFromHttp();
            HttpRequest request = new HttpRequest();
            request.setProtocol("http");
            request.setHost("169.254.169.254");
            request.setPath("/latest/meta-data/iam/security-credentials/" + uriEncode(instanceRole, true));

            HttpResponse response = this.client.makeRequest(request);

            this.parseCredentials(new String(response.getBody(), StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot access env params", exception);
        }
    }

    /**
     * Encodes uri components for http safety.
     * Slightly modified code from amazon's example on their web page in authorization part.
     * @param input The input string.
     * @param encodeSlash Should slash be encoded or not.
     */
    private static String uriEncode(CharSequence input, boolean encodeSlash) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '~' || ch == '.') {
                result.append(ch);
            } else if (ch == '/') {
                result.append(encodeSlash ? "%2F" : ch);
            } else {
                result.append(toUrlHexUTF8(ch));
            }
        }
        return result.toString();
    }

    /**
     * Turns the char into utf8 string, grabs it's bytes in utf8 shape and encodes them one by one with url syntax.
     * @param ch The char to encode.
     * @return The encoded data.
     */
    private static String toUrlHexUTF8(char ch) {
        final byte[] raw = ("" + ch).getBytes(StandardCharsets.UTF_8);
        final StringBuilder hexString = new StringBuilder();
        for (final byte rawByte : raw) {
            hexString.append("%");
            hexString.append(String.format("%02X", rawByte & 0XFF));
        }
        return hexString.toString().toUpperCase();
    }

    private String loadRoleFromHttp() {
        try {
            HttpRequest request = new HttpRequest();
            request.setProtocol("http");
            request.setHost("169.254.169.254");
            request.setPath("/latest/meta-data/iam/security-credentials/");

            HttpResponse response = this.client.makeRequest(request);

            String instanceRole = new String(response.getBody(), StandardCharsets.UTF_8);

            if (instanceRole.length() < 1) {
                throw new IllegalStateException("No role found on " +
                        "http://169.254.169.254/latest/meta-data/iam/security-credentials");
            }
            return instanceRole;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot access env params", exception);
        }
    }

    private void parseCredentials(final String responseBody) {
        this.accessKeyId = this.extractSimpleJsonValue("AccessKeyId", responseBody);
        this.secretAccessKey = this.extractSimpleJsonValue("SecretAccessKey", responseBody);
        this.sessionToken = this.extractSimpleJsonValue("Token", responseBody);
        this.expiration = this.parseDateTime(this.extractSimpleJsonValue("Expiration", responseBody));
    }

    private ZonedDateTime parseDateTime(String input) {
        if (input != null) {
            return ZonedDateTime.parse(input);
        }
        return null;
    }

    /**
     * Expects jsonData to be a simple 1 level hash map, tries to extract the data by the regular expressions.
     * @param jsonKey The json key to extract. This will be used raw in regex, don't put stupid things here.
     * @param jsonData The json data to extract data from.
     * @return The extracted data or null if not found.
     */
    private String extractSimpleJsonValue(String jsonKey, String jsonData) {
        /*
        The credentials are served in this form:
        {
            "Code" : "Success",
                "LastUpdated" : "2012-04-26T16:39:16Z",
                "Type" : "AWS-HMAC",
                "AccessKeyId" : "ASIAIOSFODNN7EXAMPLE",
                "SecretAccessKey" : "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                "Token" : "token",
                "Expiration" : "2017-05-17T15:09:54Z"
        }
        As i'm a total cowboy i will not bother to implement a complete json parser here (which jre lacks sadly),
        instead i will opt in for picking them up with regular expressions instead.
        */
        Pattern pattern = Pattern.compile(".*\"" + jsonKey + "\"\\s*:\\s*\"([^\"]*)\".*",
                Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jsonData);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
}
