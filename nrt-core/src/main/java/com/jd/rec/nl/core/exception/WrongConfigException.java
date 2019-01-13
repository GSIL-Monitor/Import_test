package com.jd.rec.nl.core.exception;

/**
 * @author linmx
 * @date 2018/5/30
 */
public class WrongConfigException extends RuntimeException {

    public WrongConfigException(String message) {
        super(message);
    }

    public WrongConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongConfigException(Throwable cause) {
        super(cause);
    }
}
