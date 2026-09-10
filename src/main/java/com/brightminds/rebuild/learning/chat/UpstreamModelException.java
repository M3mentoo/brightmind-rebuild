package com.brightminds.rebuild.learning.chat;

public class UpstreamModelException extends RuntimeException {

    public UpstreamModelException(String message) {
        super(message);
    }

    public UpstreamModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
