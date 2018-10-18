package com.github.kulminaator.s3;

public class S3Object {
    private String key;
    private String ETag;
    private Long size;
    private String lastModified;

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public void setETag(String eTag) {
        this.ETag = eTag;
    }

    public String getETag() {
        return this.ETag;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getSize() {
        return this.size;
    }

    public String getLastModified() {
        return this.lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
}
