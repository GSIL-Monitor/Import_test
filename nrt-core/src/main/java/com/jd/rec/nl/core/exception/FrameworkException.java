package com.jd.rec.nl.core.exception;

/**
 * Created by linmx on 2018/5/15.
 */
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public FrameworkException(Throwable cause) {

        super(cause);
    }

    public FrameworkException(String message) {
        super(message);
    }
}
