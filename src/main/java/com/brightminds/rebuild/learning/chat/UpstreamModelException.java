package com.brightminds.rebuild.learning.chat;

public class UpstreamModelException extends RuntimeException {

    private final Integer statusCode;
    private final boolean retryable;

    public UpstreamModelException(int statusCode, boolean retryable) {
        super("Model provider returned HTTP " + statusCode);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public UpstreamModelException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.retryable = false;
    }

    public Integer statusCode() {
        return statusCode;
    }

    public boolean retryable() {
        return retryable;
    }
}
