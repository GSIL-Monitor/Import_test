package com.jd.rec.nl.service.common.monitor.domain;

/**
 * 垃圾回收器的信息
 *
 * @author wl
 * @since 2018/11/13
 */
public class GCInfo {
    /**
     * gc的名称
     */
    private String name;
    /**
     * 总数
     */
    private long count;
    /**
     * 总消耗时间(ms)
     */
    private long time;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "GCInfo{" +
                "name='" + name + '\'' +
                ", count=" + count +
                ", time=" + time +
                '}';
    }
}
