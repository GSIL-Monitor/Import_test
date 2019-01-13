package com.jd.rec.nl.app.origin.modules.rbcidblacklist.domain;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/10/31
 */
public class RbcidExposure implements Comparable<RbcidExposure>{
    String rbcid;
    long expireTime;
    int exposureCount = 0;

    public RbcidExposure() {
    }

    boolean isBlacklist = false;

    public RbcidExposure(String rbcid, long expireTime, int exposureCount) {
        this.rbcid = rbcid;
        this.expireTime = expireTime;
        this.exposureCount = exposureCount;
    }

    public String getRbcid() {
        return rbcid;
    }

    public void setRbcid(String cid3) {
        this.rbcid = cid3;
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
    public int compareTo(RbcidExposure o) {
        if (this.getExpireTime() > o.getExpireTime()) {
            return 1;
        } else if (this.getExpireTime() == o.getExpireTime()) {
            return this.rbcid.compareTo(o.getRbcid());
        } else {
            return -1;
        }
    }
}
