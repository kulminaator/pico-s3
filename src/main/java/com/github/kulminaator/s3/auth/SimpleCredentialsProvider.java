package com.github.kulminaator.s3.auth;

public class SimpleCredentialsProvider implements CredentialsProvider {
    private String accessKeyId;
    private String sessionToken;
    private String secretAccessKey;

    @Override
    public String getAccessKeyId() {
        return this.accessKeyId;
    }

    @Override
    public String getSecretAccessKey() {
        return this.secretAccessKey;
    }

    @Override
    public String getSessionToken() {
        return this.sessionToken;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }
}
