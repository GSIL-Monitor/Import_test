package com.jd.rec.nl.app.origin.modules.promotionalburst.domain;

/**
 * @author linmx
 * @date 2018/9/29
 */
public class ClickFeature extends EventFeature {

    String uid;

    long sku;

    public ClickFeature(String uid, long sku) {
        this.uid = uid;
        this.sku = sku;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getSku() {
        return sku;
    }

    public void setSku(long sku) {
        this.sku = sku;
    }
}
