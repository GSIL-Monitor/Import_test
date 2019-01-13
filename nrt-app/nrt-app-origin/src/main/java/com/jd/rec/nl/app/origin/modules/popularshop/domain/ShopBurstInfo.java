package com.jd.rec.nl.app.origin.modules.popularshop.domain;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wl
 * @date 2018/10/15
 */
public class ShopBurstInfo implements Serializable {

    String featureKey;

    protected List<Integer> shopUV1h = new ArrayList<>();
    protected int shopUV5min = 0;
    protected List<Integer> shopSkuUV1h = new ArrayList<>();
    protected int shopSkuUV5min = 0;
    /**
     * 当前窗口的hc,会根据uv变化,计算逻辑:
     * double newHistoryClick = historyClick * alpha + (1 - alpha) * uv;
     */
    double shopUV1hHc;

    double shopSkuUV1hHc;

    double shopUV5minHc;

    double shopSkuUV5minHc;

    HistoryCUV historyCUV;
    /**
     * 前一个窗口的hc值,
     */
    double preHcSku1h;

    double preHcSku5min;

    double preHcUV1h;

    double preHcUV5min;

    Double alpha;

    Double punishment;

    Double score;

    public double getShopUV1hHc() {
        return shopUV1hHc;
    }

    public ShopBurstInfo(String featureKey, Double punishment, Duration windowSize, Double alpha, HistoryCUV historyValue) {
        this.featureKey = featureKey;
        this.alpha = alpha;
        this.punishment = punishment;

        // 计算hc t-1
        long historyClickTime5min = historyValue.getTimestamp5min();
        long historyClickTime1h = historyValue.getTimestamp1h();
        long now = System.currentTimeMillis();
        long pow5min = (now - historyClickTime5min) / windowSize.toMillis() - 1;
        if (pow5min < 0) {
            pow5min = 0;
        }
        long pow1h = (now - historyClickTime1h) / windowSize.toMillis() * 12 - 1;
        this.preHcSku1h = historyValue.getShopSkuUV1hCoefficient() * Math.pow(alpha, pow1h);
        this.preHcSku5min = historyValue.getShopSkuUV5minCoefficient() * Math.pow(alpha, pow5min);
        this.preHcUV1h = historyValue.getShopUV1hCoefficient() * Math.pow(alpha, pow1h);
        this.preHcUV5min = historyValue.getShopUV5minCoefficient() * Math.pow(alpha, pow5min);

        // 计算店铺1小时和5分钟的hc
        shopUV1hHc = preHcUV1h * alpha;
        shopUV5minHc = preHcUV5min * alpha;
        //计算商详页1小时和5分钟的hc
        shopSkuUV1hHc = preHcSku1h * alpha;
        shopSkuUV5minHc = preHcSku5min * alpha;
    }

    /**
     * 计算score,目前是召回哪个就计算哪个。(目前商详页5分钟的)
     *
     * @return
     */
    public Double getScore() {
        score = (shopSkuUV5min + punishment) / (this.preHcSku5min + punishment);
        return score;
    }

    public double getPreHcSku1h() {
        return preHcSku1h;
    }

    public void setPreHcSku1h(double preHcSku1h) {
        this.preHcSku1h = preHcSku1h;
    }

    public double getPreHcSku5min() {
        return preHcSku5min;
    }

    public void setPreHcSku5min(double preHcSku5min) {
        this.preHcSku5min = preHcSku5min;
    }

    public double getPreHcUV1h() {
        return preHcUV1h;
    }

    public void setPreHcUV1h(double preHcUV1h) {
        this.preHcUV1h = preHcUV1h;
    }

    public double getPreHcUV5min() {
        return preHcUV5min;
    }

    public void setPreHcUV5min(double preHcUV5min) {
        this.preHcUV5min = preHcUV5min;
    }

    public void setShopUV1hHc(double shopUV1hHc) {
        this.shopUV1hHc = shopUV1hHc;
    }

    public double getShopSkuUV1hHc() {
        return shopSkuUV1hHc;
    }

    public void setShopSkuUV1hHc(double shopSkuUV1hHc) {
        this.shopSkuUV1hHc = shopSkuUV1hHc;
    }

    public double getShopUV5minHc() {
        return shopUV5minHc;
    }

    public void setShopUV5minHc(double shopUV5minHc) {
        this.shopUV5minHc = shopUV5minHc;
    }

    public double getShopSkuUV5minHc() {
        return shopSkuUV5minHc;
    }

    public void setShopSkuUV5minHc(double shopSkuUV5minHc) {
        this.shopSkuUV5minHc = shopSkuUV5minHc;
    }

    public int getShopUV5min() {
        return shopUV5min;
    }

    public void setShopUV5min(int shopUV5min) {
        this.shopUV5min = shopUV5min;
    }

    public List<Integer> getShopUV1h() {
        return shopUV1h;
    }

    public void addshopUV5min() {
        shopUV5min++;
        // 重新计算当前的hc
        shopUV5minHc = shopUV5minHc + (1 - alpha);
    }

    public void addshopSkuUV5min() {
        shopSkuUV5min++;
        shopSkuUV5minHc = shopSkuUV5minHc + (1 - alpha);
    }

    //最后一个做++操作
    public void addShopUV1h() {
        if (shopUV1h.size() == 0) {
            shopUV1h.add(1);
        } else {
            int num = shopUV1h.get(shopUV1h.size() - 1).intValue();
            num++;
            shopUV1h.remove(shopUV1h.size() - 1);
            shopUV1h.add(num);
        }
        shopUV1hHc = shopUV1hHc + (1 - alpha);
    }

    public void addShopSkuUV1h() {
        if (shopSkuUV1h.size() == 0) {
            shopSkuUV1h.add(1);
        } else {
            int num = shopSkuUV1h.get(shopSkuUV1h.size() - 1);
            num++;
            shopSkuUV1h.remove(shopSkuUV1h.size() - 1);
            shopSkuUV1h.add(num);
        }
        shopSkuUV1hHc = shopSkuUV1hHc + (1 - alpha);
    }

    public void setShopUV1h(List<Integer> shopUV1h) {
        this.shopUV1h = shopUV1h;
    }

    public List<Integer> getShopSkuUV1h() {
        return shopSkuUV1h;
    }

    public void setShopSkuUV1h(List<Integer> shopSkuUV1h) {
        this.shopSkuUV1h = shopSkuUV1h;
    }

    public int getShopSkuUV5min() {
        return shopSkuUV5min;
    }

    public void setShopSkuUV5min(int shopSkuUV5min) {
        this.shopSkuUV5min = shopSkuUV5min;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    public HistoryCUV getHistoryCUV() {
        return historyCUV;
    }

    public void setHistoryCUV(HistoryCUV historyCUV) {
        this.historyCUV = historyCUV;
    }

    public Double getAlpha() {
        return alpha;
    }

    public void setAlpha(Double alpha) {
        this.alpha = alpha;
    }

    public Double getPunishment() {
        return punishment;
    }

    public void setPunishment(Double punishment) {
        this.punishment = punishment;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
