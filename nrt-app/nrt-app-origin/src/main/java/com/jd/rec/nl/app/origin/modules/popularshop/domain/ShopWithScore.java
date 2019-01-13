package com.jd.rec.nl.app.origin.modules.popularshop.domain;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/7/11
 */
public class ShopWithScore implements Serializable {
    long shopId;
    double score;

    public ShopWithScore(long shopId, double score) {
        this.shopId = shopId;
        this.score = score;
    }

    public ShopWithScore() {
    }

    public long getShopId() {
        return shopId;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return String.format("sku:%d, score:%f", shopId, score);
    }
}
