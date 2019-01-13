package com.jd.rec.nl.app.origin.modules.popularshop.domain;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/10/10
 */
public class FeatureLog<T> implements Serializable {

    private T burstInfo;

    private String out;

    public FeatureLog(T burstInfo, com.google.protobuf.GeneratedMessageV3.Builder builder) {

        this.burstInfo = burstInfo;
        this.out = builder.toString();
    }

    public T getBurstInfo() {
        return burstInfo;
    }

    public void setBurstInfo(T burstInfo) {
        this.burstInfo = burstInfo;
    }

    public String getOut() {
        return out;
    }

    public void setOut(String out) {
        this.out = out;
    }
}
