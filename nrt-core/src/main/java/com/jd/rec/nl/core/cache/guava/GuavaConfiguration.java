package com.jd.rec.nl.core.cache.guava;

import javax.cache.configuration.MutableConfiguration;

public class GuavaConfiguration<K, V> extends MutableConfiguration<K, V> {

    private long maximumSize = -1L;

    private long maximumWeight = -1L;

    private long statisticsInterval = -1L;

    public long getStatisticsInterval() {
        return statisticsInterval;
    }

    public void setStatisticsInterval(long statisticsInterval) {
        this.statisticsInterval = statisticsInterval;
    }

    public long getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(long maximumSize) {
        this.maximumSize = maximumSize;
    }

    public long getMaximumWeight() {
        return maximumWeight;
    }

    public void setMaximumWeight(long maximumWeight) {
        this.maximumWeight = maximumWeight;
    }
}
