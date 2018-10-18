package com.github.kulminaator.s3.auth;

public class SimpleCredentialsProvider implements CredentialsProvider {
    private String accesKeyId;
    private String sessionToken;
    private String secretAccessKey;

    @Override
    public String getAccessKeyId() {
        return this.accesKeyId;
    }

    @Override
    public String getSecretAccessKey() {
        return this.secretAccessKey;
    }

    @Override
    public String getSessionToken() {
        return this.sessionToken;
    }

    public void setAccesKeyId(String accesKeyId) {
        this.accesKeyId = accesKeyId;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }
}
