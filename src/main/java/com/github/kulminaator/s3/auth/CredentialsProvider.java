package com.github.kulminaator.s3.auth;

public interface CredentialsProvider {

    public String getAccessKeyId();
    public String getSecretAccessKey();
    public String getSessionToken();

}
