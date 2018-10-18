package com.github.kulminaator.s3.auth;

public class EnvironmentCredentialsProvider implements CredentialsProvider {
    @Override
    public String getAccessKeyId() {
        return this.getFromEnv("AWS_ACCESS_KEY_ID");
    }

    @Override
    public String getSecretAccessKey() {
        return this.getFromEnv("AWS_SECRET_ACCESS_KEY");
    }

    @Override
    public String getSessionToken() {
        return this.getFromEnv("AWS_SESSION_TOKEN");
    }

    private String getFromEnv(final String param) {
        return System.getenv(param);
    }
}
