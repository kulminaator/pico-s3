package com.github.kulminaator.s3;

import com.github.kulminaator.s3.exception.S3AccessException;
import com.github.kulminaator.s3.options.PutObjectOptions;

import java.io.InputStream;
import java.util.List;

public interface Client {
    /**
     * Fetches object data (size, last modified at etc.)
     * @param bucket Name of the bucket where the object is.
     * @param object Path to the object inside the bucket.
     * @return The data describing the object (but not the contents of the object).
     * @throws S3AccessException In case there's a communication issue with s3.
     */
    S3Object getObject(String bucket, String object) throws S3AccessException;

    /**
     * List objects in the bucket.
     * @param bucket Name of the bucket
     * @return List of s3 objects.
     * @throws S3AccessException In case there's a communication issue with s3.
     */
    List<S3Object> listObjects(String bucket) throws S3AccessException;

    /**
     * List objects in bucket under the prefix.
     *
     * @param bucket Name of the bucket.
     * @param prefix The prefix of all objects.
     * @return List of s3 objects.
     * @throws S3AccessException In case there's a communication issue with s3.
     */
    List<S3Object> listObjects(String bucket, String prefix) throws S3AccessException;

    /**
     * Fetches the object from S3, buffers it into a byte array and provides input stream to the byte array.
     * Obviously not ideal for huge transfers (support for these will come later).
     * @param bucket Name of the bucket.
     * @param object Name of the object.
     * @return InputStream to the raw data in bytes.
     * @throws S3AccessException In case there's a communication issue with s3.
     */
    InputStream getObjectDataAsInputStream(String bucket, String object) throws S3AccessException;

    /**
     * Assumes that object is text data and encoded as utf-8. Returns the contents as a String. Comfortable to use
     * in case you keep your config or template files as text based (json/yaml/ini/xml) files in S3.
     * @param bucket The bucket name.
     * @param object The object path.
     * @return Contents as a String
     * @throws S3AccessException In case there's a communication issue with s3.
     */
    String getObjectDataAsString(String bucket, String object) throws S3AccessException;

    /**
     * Creates the named file in the S3 with the specified Content-Type. Designed for smaller files that easily fit
     * into your computer's memory. As data is a byte array here it cannot contain more than 2GB of data (int length
     * would overflow).
     *
     * @param bucket Bucket name.
     * @param object Object path in bucket.
     * @param data Binary data of the file.
     * @param contentType Content type of the file.
     * @throws S3AccessException In case there's a communication issue with s3.
     */
    void putObject(String bucket, String object, byte[] data, String contentType) throws S3AccessException;

    /**
     * Creates the named file in the S3 with the specified Content-Type. Designed for smaller files that easily fit
     * into your computer's memory. As data is a byte array here it cannot contain more than 2GB of data (int length
     * would overflow).
     *
     * @param bucket Bucket name.
     * @param object Object path in bucket.
     * @param data Binary data of the file.
     * @param options Specific s3 object options.
     * @throws S3AccessException In case there's a communication issue with s3.
     */
    void putObject(String bucket, String object, byte[] data, PutObjectOptions options) throws S3AccessException;
}
