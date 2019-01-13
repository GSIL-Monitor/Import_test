package com.jd.rec.nl.app.origin.modules.cid3blacklist.domain;

/**
 * @author linmx
 * @date 2018/6/28
 */
public class Cid3Exposure implements Comparable<Cid3Exposure> {
    int cid3;
    long expireTime;
    int exposureCount = 0;

    public Cid3Exposure() {
    }

    boolean isBlacklist = false;

    public Cid3Exposure(int cid3, long expireTime, int exposureCount) {
        this.cid3 = cid3;
        this.expireTime = expireTime;
        this.exposureCount = exposureCount;
    }

    public int getCid3() {
        return cid3;
    }

    public void setCid3(int cid3) {
        this.cid3 = cid3;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public int getExposureCount() {
        return exposureCount;
    }

    public void setExposureCount(int exposureCount) {
        this.exposureCount = exposureCount;
    }

    public boolean isBlacklist() {
        return isBlacklist;
    }

    public void setBlacklist(boolean blacklist) {
        isBlacklist = blacklist;
    }

    @Override
    public int compareTo(Cid3Exposure o) {
        if (this.getExpireTime() > o.getExpireTime()) {
            return 1;
        } else if (this.getExpireTime() == o.getExpireTime()) {
            return 0;
        } else {
            return -1;
        }
    }
}
