package com.jd.rec.nl.app.origin.modules.cid3preference.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * 一个cid3最近7天的点击数据
 *
 * @author wl
 * @date 2018/9/3
 */
public class Cid3ClickInfo {

    Map<Integer, Integer> cid3ClickCount = new HashMap<>();

    double preScore = 0;

    public Cid3ClickInfo() {

    }

    public Cid3ClickInfo(Map<Integer, Integer> cid3ClickCount, double preScore) {
        this.cid3ClickCount = cid3ClickCount;
        this.preScore = preScore;
    }

    public Map<Integer, Integer> getCid3ClickCount() {
        return cid3ClickCount;
    }

    public void setCid3ClickCount(Map<Integer, Integer> cid3ClickCount) {
        this.cid3ClickCount = cid3ClickCount;
    }

    public double getPreScore() {
        return preScore;
    }

    public void setPreScore(double preScore) {
        this.preScore = preScore;
    }

    @Override
    public String toString() {
        return "Cid3ClickInfo{" +
                "cid3ClickCount=" + cid3ClickCount +
                ", preScore=" + preScore +
                '}';
    }
}
