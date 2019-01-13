package com.jd.rec.nl.app.origin.modules.brandpreference.domain;

import java.io.Serializable;

public class BrandSku implements Serializable{
    /**
     * 过期时间
     */
    long expireTime;
    long sku;
    /**
     * 品牌id
     */
    String brandId;
    /**
     * 相似品牌id的权重
     */
    Float weight;


    public BrandSku(){

    }

    public BrandSku(long expireTime, long sku, String brandId, Float weight) {
        this.expireTime = expireTime;
        this.sku = sku;
        this.brandId = brandId;
        this.weight = weight;

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

    public String getBrandId() {
        return brandId;
    }

    public void setBrandId(String brandId) {
        this.brandId = brandId;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "BrandSku{" +
                "expireTime=" + expireTime +
                ", sku=" + sku +
                ", brandId='" + brandId + '\'' +
                ", weight=" + weight +
                '}';
    }
}
