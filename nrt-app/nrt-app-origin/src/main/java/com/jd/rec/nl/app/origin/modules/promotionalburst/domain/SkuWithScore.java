package com.jd.rec.nl.app.origin.modules.promotionalburst.domain;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/7/11
 */
public class SkuWithScore implements Serializable {
    long sku;
    double score;

    public SkuWithScore(long sku, double score) {
        this.sku = sku;
        this.score = score;
    }

    public SkuWithScore() {
    }

    public long getSku() {
        return sku;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return String.format("sku:%d, score:%f", sku, score);
    }
}
