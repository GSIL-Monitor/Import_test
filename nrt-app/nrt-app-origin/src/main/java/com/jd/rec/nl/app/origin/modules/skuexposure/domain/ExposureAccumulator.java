package com.jd.rec.nl.app.origin.modules.skuexposure.domain;

/**
 * @author linmx
 * @date 2018/6/15
 */
public class ExposureAccumulator {
    long sku;
    int count;
    long expireTime;

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

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }
}
