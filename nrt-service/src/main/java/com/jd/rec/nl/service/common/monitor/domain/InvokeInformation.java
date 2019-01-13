package com.jd.rec.nl.service.common.monitor.domain;

import org.apache.commons.lang3.time.DateFormatUtils;

/**
 * @author linmx
 * @date 2018/8/30
 */
public class InvokeInformation {
    long start;

    long end;

    String startTimestamp;

    String endTimestamp;

    String executorName;

    String threadName;

    long duration;

    String e;

    public InvokeInformation(String executorName) {
        this.executorName = executorName;
        this.start = System.currentTimeMillis();
        this.startTimestamp = DateFormatUtils.format(start, "yyyy-MM-dd HH:mm:ss.SSS");
        this.threadName = Thread.currentThread().getName();
    }

    public void exception(String e) {
        this.e = e;
    }

    @Override
    public String toString() {
        return "InvokeInformation{" +
                "start=" + start +
                ", end=" + end +
                ", startTimestamp='" + startTimestamp + '\'' +
                ", endTimestamp='" + endTimestamp + '\'' +
                ", executorName='" + executorName + '\'' +
                ", threadName='" + threadName + '\'' +
                ", duration=" + duration +
                ", e='" + e + '\'' +
                '}';
    }

    public void end() {
        this.end = System.currentTimeMillis();
        this.endTimestamp = DateFormatUtils.format(end, "yyyy-MM-dd HH:mm:ss.SSS");
        duration = end - start;
    }
}
