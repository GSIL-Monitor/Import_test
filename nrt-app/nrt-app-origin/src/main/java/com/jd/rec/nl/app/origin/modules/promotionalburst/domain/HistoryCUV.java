package com.jd.rec.nl.app.origin.modules.promotionalburst.domain;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/7/16
 */
public class HistoryCUV implements Serializable {

    private double coefficient = 0;

    private long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }
}
