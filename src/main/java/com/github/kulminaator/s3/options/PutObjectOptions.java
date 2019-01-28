package com.github.kulminaator.s3.options;

/**
 * Put Object request options. Use the builder to create an instance.
 */
public class PutObjectOptions {

    private String contentType;
    private String serverSideEncryption;
    private String serverSideEncryptionKeyId;

    private void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return this.contentType;
    }

    private void setServerSideEncryption(String serverSideEncryption) {
        this.serverSideEncryption = serverSideEncryption;
    }

    public String getServerSideEncryption() {
        return serverSideEncryption;
    }

    private void setServerSideEncryptionKeyId(String serverSideEncryptionKeyId) {
        this.serverSideEncryptionKeyId = serverSideEncryptionKeyId;
    }

    public String getServerSideEncryptionKeyId() {
        return serverSideEncryptionKeyId;
    }

    /**
     * Helps to build PutObjectOptions objects.
     */
    public static class Builder {

        public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
        /**
         * Indicates servers side encryption with s3 master key.
         */
        public static final String SERVER_SIDE_ENCRYPTION_S3 = "AES256";
        public static final String SERVER_SIDE_ENCRYPTION_KMS = "aws:kms";

        private String contentType = DEFAULT_CONTENT_TYPE;
        private String serverSideEncryption;
        private String serverSideEncryptionKeyId;

        public Builder(){}

        /**
         * Sets the content type for the request.
         * @param contentType The content type.
         * @return Builder.
         */
        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Sets the encryption type used on the server side for the object.
         * @param encryptionType The encryption type, either SERVER_SIDE_ENCRYPTION_S3 or SERVER_SIDE_ENCRYPTION_KMS.
         * @return Builder.
         */
        public Builder withServerSideEncryption(String encryptionType) {
            this.serverSideEncryption = encryptionType;
            return this;
        }

        /**
         * Specifies the kms key id to use for encryption. Only makes sense with sse type KMS.
         * @param keyId The key id to use.
         * @return Builder.
         */
        public Builder withServerSideEncryptionKeyId(String keyId) {
            this.serverSideEncryptionKeyId = keyId;
            return this;
        }

        public PutObjectOptions build() {
            final PutObjectOptions putObjectOptions = new PutObjectOptions();
            putObjectOptions.setContentType(this.contentType);

            if (this.serverSideEncryption != null) {
                putObjectOptions.setServerSideEncryption(serverSideEncryption);
                if (this.serverSideEncryptionKeyId != null) {
                    putObjectOptions.setServerSideEncryptionKeyId(this.serverSideEncryptionKeyId);
                }
            }
            return putObjectOptions;
        }
    }

}
