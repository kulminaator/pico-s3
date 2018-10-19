package com.github.kulminaator.s3;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface Client {
    S3Object getObject(String bucket, String object);

    List<S3Object> listObjects(String bucket) throws IOException;
    List<S3Object> listObjects(String bucket, String prefix) throws IOException;

    InputStream getObjectDataAsInputStream(String bucket, String object) throws IOException;

    /**
     * Assumens object is text and encoded as utf-8. Returns the contents as a String.
     * @param bucket The bucket name.
     * @param object The object path.
     * @return Contents as a String
     * @throws IOException In case things blow up.
     */
    String getObjectDataAsString(String bucket, String object) throws IOException;
}
