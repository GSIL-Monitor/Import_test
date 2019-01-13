package com.jd.rec.nl.connector.storm.trace.domain;

/**
 * @author linmx
 * @date 2018/9/26
 */
public class UserRuntimeTrace {

    String uid;
    String executor;
    String input;
    String output;
    String exception;
    String time;

    public UserRuntimeTrace(String uid, String executor, String input, String output, String exception, String time) {
        this.uid = uid;
        this.executor = executor;
        this.input = input;
        this.output = output;
        this.exception = exception;
        this.time = time;
    }

    public String getExecutor() {

        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
