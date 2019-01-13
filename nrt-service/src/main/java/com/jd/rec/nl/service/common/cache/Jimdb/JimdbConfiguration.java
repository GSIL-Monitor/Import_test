package com.jd.rec.nl.service.common.cache.Jimdb;

import javax.cache.configuration.MutableConfiguration;

public class JimdbConfiguration<K, V> extends MutableConfiguration<K, V> {

    private long statisticsInterval = -1L;

    public long getStatisticsInterval() {
        return statisticsInterval;
    }

    public void setStatisticsInterval(long statisticsInterval) {
        this.statisticsInterval = statisticsInterval;
    }

}
