package com.jd.rec.nl.app.origin.modules.activityburst.domain;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/7/11
 */
public class ActivityIdWithScore implements Serializable {
    long activityId;
    double score;

    public ActivityIdWithScore(long activityId, double score) {
        this.activityId = activityId;
        this.score = score;
    }

    public ActivityIdWithScore() {
    }

    public long getActivityId() {
        return activityId;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return String.format("activityId:%d, score:%f", activityId, score);
    }
}
