package com.jd.rec.nl.app.origin.modules.promotionalburst.domain;

import java.io.Serializable;
import java.time.Duration;

/**
 * @author linmx
 * @date 2018/8/24
 */
public class BurstInfo implements Serializable {

    String featureKey;

    int uv;

    /**
     * 当前窗口的hc,会根据uv变化,计算逻辑:
     * double newHistoryClick = historyClick * alpha + (1 - alpha) * uv;
     */
    double hc;

    /**
     * 前一个窗口的hc值,
     */
    double preHc;

    /**
     * 时间戳
     */
    long time;

    Double alpha;

    Double punishment;

    Double score;

    public BurstInfo(String featureKey, Double alpha, Double punishment, Duration windowSize, HistoryCUV historyValue) {
        this.featureKey = featureKey;
        this.alpha = alpha;
        this.punishment = punishment;
        uv = 1;
        // 计算hc t-1
        long historyClickTime = historyValue.getTimestamp();
        time = System.currentTimeMillis();
        long pow = (time - historyClickTime) / windowSize.toMillis() - 1;
        if (pow < 0)
            pow = 0;
        this.preHc = historyValue.getCoefficient() * Math.pow(alpha, pow);
        // 计算hc
        hc = preHc * alpha + (1 - alpha) * uv;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public double getScore() {
        score = (uv + punishment) / (this.preHc + punishment);
        return score;
    }

    public void addUV() {
        uv++;
        // 重新计算当前的hc
        hc = hc + (1 - alpha);
    }

    public int getUv() {
        return uv;
    }

    public void setUv(int uv) {
        this.uv = uv;
    }

    public double getHc() {
        return hc;
    }

    public void setHc(double hc) {
        this.hc = hc;
    }

    public double getPreHc() {
        return preHc;
    }

    public void setPreHc(double preHc) {
        this.preHc = preHc;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
