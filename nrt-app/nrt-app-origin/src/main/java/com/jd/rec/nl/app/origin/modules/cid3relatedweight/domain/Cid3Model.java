package com.jd.rec.nl.app.origin.modules.cid3relatedweight.domain;

/**
 * @author wl
 * @date 2018/8/25
 */
public class Cid3Model {
    long expireTime;
    long sku;
    int cid3;


    public Cid3Model(){

    }
    public Cid3Model(long expireTime, long sku, int cid3) {
        this.expireTime = expireTime;
        this.sku = sku;
        this.cid3 = cid3;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public long getSku() {
        return sku;
    }

    public void setSku(long sku) {
        this.sku = sku;
    }

    public int getCid3() {
        return cid3;
    }

    public void setCid3(int cid3) {
        this.cid3 = cid3;
    }

    @Override
    public String toString() {
        return "Cid3Model{" +
                "expireTime=" + expireTime +
                ", sku=" + sku +
                ", cid3='" + cid3 + '\'' +
                '}';
    }
}
