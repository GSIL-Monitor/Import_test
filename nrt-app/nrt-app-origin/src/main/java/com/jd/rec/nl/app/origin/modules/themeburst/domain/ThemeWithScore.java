package com.jd.rec.nl.app.origin.modules.themeburst.domain;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/9/29
 */
public class ThemeWithScore implements Serializable{

    long themeId;
    double score;

    public ThemeWithScore(long themeId, double score) {
        this.themeId = themeId;
        this.score = score;
    }

    public long getThemeId() {
        return themeId;
    }

    public void setThemeId(long themeId) {
        this.themeId = themeId;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "ThemeWithScore{" +
                "themeId=" + themeId +
                ", score=" + score +
                '}';
    }
}
