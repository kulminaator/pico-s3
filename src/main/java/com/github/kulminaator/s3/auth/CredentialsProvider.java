package com.github.kulminaator.s3.auth;

public interface CredentialsProvider {

    String getAccessKeyId();
    String getSecretAccessKey();
    String getSessionToken();
}
