package com.jd.rec.nl.core.exception;

import com.jd.ump.profiler.proxy.Profiler;

/**
 * @author linmx
 * @date 2018/9/18
 */
public class EnvironmentException extends RuntimeException {

    private String source;

    public EnvironmentException(String source, String message) {
        super(message);
        this.source = source;
        Profiler.countAccumulate("NRT_ENV_ERROR_".concat(source));
    }


    public EnvironmentException(String source, String message, Throwable cause) {
        super(message, cause);
        this.source = source;
        Profiler.countAccumulate("NRT_ENV_ERROR_".concat(source));
    }

    public EnvironmentException(String source, Throwable cause) {
        super(cause);
        this.source = source;
        Profiler.countAccumulate("NRT_ENV_ERROR_".concat(source));
    }

    public String getSource() {
        return source;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        return String.format("[%s]:%s", source, message);
    }
}
