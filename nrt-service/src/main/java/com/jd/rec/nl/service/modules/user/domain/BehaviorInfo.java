package com.jd.rec.nl.service.modules.user.domain;

import java.io.Serializable;

public class BehaviorInfo implements Serializable {
    private long sku;
    private long timestamp;
    private int count;

    public BehaviorInfo(final long sku, final long timestamp, final int count) {
        this.sku = sku;
        this.timestamp = timestamp;
        this.count = count;
    }

    public BehaviorInfo() {
    }

    public long getSku() {
        return sku;
    }

    public void setSku(long sku) {
        this.sku = sku;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{sku:");
        sb.append(sku).append(",count:").append(count).append(",time:").append(timestamp).append("}");
        return sb.toString();
    }
}
