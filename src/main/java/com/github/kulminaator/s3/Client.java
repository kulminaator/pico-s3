package com.github.kulminaator.s3;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface Client {
    /**
     * Fetches object data (size, last modified at etc.)
     * @param bucket Name of the bucket where the object is.
     * @param object Path to the object inside the bucket.
     * @return The data describing the object (but not the contents of the object).
     */
    S3Object getObject(String bucket, String object);

    /**
     * List objects in the bucket.
     * @param bucket Name of the bucket
     * @return List of s3 objects.
     * @throws IOException In case there's a communication issue with s3.
     */
    List<S3Object> listObjects(String bucket) throws IOException;

    /**
     * List objects in bucket under the prefix.
     *
     * @param bucket Name of the bucket.
     * @param prefix The prefix of all objects.
     * @return List of s3 objects.
     * @throws IOException In case there's a communication issue with s3.
     */
    List<S3Object> listObjects(String bucket, String prefix) throws IOException;

    /**
     * Fetches the object from S3, buffers it into a byte array and provides input stream to the byte array.
     * Obviously not ideal for huge transfers (support for these will come later).
     * @param bucket Name of the bucket.
     * @param object Name of the object.
     * @return InputStream to the raw data in bytes.
     * @throws IOException In case there's a communication issue with s3.
     */
    InputStream getObjectDataAsInputStream(String bucket, String object) throws IOException;

    /**
     * Assumes that object is text data and encoded as utf-8. Returns the contents as a String.
     * @param bucket The bucket name.
     * @param object The object path.
     * @return Contents as a String
     * @throws IOException In case there's a communication issue with s3.
     */
    String getObjectDataAsString(String bucket, String object) throws IOException;
}
