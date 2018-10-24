package com.github.kulminaator.s3.exception;

/**
 * Wrapper from exceptions that are thrown from below to save people from adding IoException handling all over their
 * code. If these are thrown you still need to handle them, but perhaps you want to do it a layer or a few away
 * and not cover your code with checked exceptions all over the place.
 */
public class S3AccessException extends RuntimeException {
    public S3AccessException(final Exception cause) {
        super(cause);
    }
    public S3AccessException(final String message, final Exception cause) {
        super(message, cause);
    }
    public S3AccessException(final String message) {
        super(message);
    }
}
