package com.jd.rec.nl.app.origin.modules.rbcidblacklist.domain;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/11/24
 */
public class RbcidOrder {

    String rbcid;

    long timeStamp;

    long period;

    public RbcidOrder(String rbcid, long timeStamp, long period) {
        this.rbcid = rbcid;
        this.timeStamp = timeStamp;
        this.period = period;
    }

    public RbcidOrder() {
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public String getRbcid() {
        return rbcid;
    }

    public void setRbcid(String rbcid) {
        this.rbcid = rbcid;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
