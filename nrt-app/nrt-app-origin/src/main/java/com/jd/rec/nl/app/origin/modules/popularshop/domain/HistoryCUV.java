package com.jd.rec.nl.app.origin.modules.popularshop.domain;

import java.io.Serializable;

/**
 * @author wl
 * @date 2018/10/15
 */
public class HistoryCUV implements Serializable {

    private double shopSkuUV1hCoefficient = 0;
    private double shopSkuUV5minCoefficient = 0;
    private double shopUV1hCoefficient = 0;
    private double shopUV5minCoefficient = 0;
    private long timestamp5min;
    private long timestamp1h;

    public long getTimestamp5min() {
        return timestamp5min;
    }

    public void setTimestamp5min(long timestamp5min) {
        this.timestamp5min = timestamp5min;
    }

    public long getTimestamp1h() {
        return timestamp1h;
    }

    public void setTimestamp1h(long timestamp1h) {
        this.timestamp1h = timestamp1h;
    }

    public double getShopSkuUV1hCoefficient() {
        return shopSkuUV1hCoefficient;
    }

    public void setShopSkuUV1hCoefficient(double shopSkuUV1hCoefficient) {
        this.shopSkuUV1hCoefficient = shopSkuUV1hCoefficient;
    }

    public double getShopSkuUV5minCoefficient() {
        return shopSkuUV5minCoefficient;
    }

    public void setShopSkuUV5minCoefficient(double shopSkuUV5minCoefficient) {
        this.shopSkuUV5minCoefficient = shopSkuUV5minCoefficient;
    }

    public double getShopUV1hCoefficient() {
        return shopUV1hCoefficient;
    }

    public void setShopUV1hCoefficient(double shopUV1hCoefficient) {
        this.shopUV1hCoefficient = shopUV1hCoefficient;
    }

    public double getShopUV5minCoefficient() {
        return shopUV5minCoefficient;
    }

    public void setShopUV5minCoefficient(double shopUV5minCoefficient) {
        this.shopUV5minCoefficient = shopUV5minCoefficient;
    }
}
