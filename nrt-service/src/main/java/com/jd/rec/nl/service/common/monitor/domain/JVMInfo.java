package com.jd.rec.nl.service.common.monitor.domain;

import java.util.List;

/**
 * jvm详细信息
 *
 * @author wanlong3
 * @date 2018/11/13
 */
public class JVMInfo {

    /**
     * jvm进程id
     */
    private String processId;
    /**
     * jvm内存上限
     */
    private long jvmMaxMemory;

    /**
     * 堆的最大上限(MB)
     */
    private Long heapMemoryMax;
    /**
     * 堆的使用大小(MB)
     */
    private Long heapMemoryUsed;
    /**
     * 非堆的最大上限(MB)
     */
    private Long noheapMemoryMax;
    /**
     * 非堆的最大使用大小(MB)
     */
    private Long noheapMemoryUsed;


    /**
     * gc的信息
     */
    private List<GCInfo> gcInfos;


    /**
     * jvm内存区的情况
     * <p>
     * private List<JvmMemoryAreaInfo> jvmMemoryAreaInfos;
     */
    public Long getHeapMemoryMax() {
        return heapMemoryMax;
    }

    public void setHeapMemoryMax(Long heapMemoryMax) {
        this.heapMemoryMax = heapMemoryMax;
    }

    public Long getHeapMemoryUsed() {
        return heapMemoryUsed;
    }

    public void setHeapMemoryUsed(Long heapMemoryUsed) {
        this.heapMemoryUsed = heapMemoryUsed;
    }

    public Long getNoheapMemoryMax() {
        return noheapMemoryMax;
    }

    public void setNoheapMemoryMax(Long noheapMemoryMax) {
        this.noheapMemoryMax = noheapMemoryMax;
    }

    public Long getNoheapMemoryUsed() {
        return noheapMemoryUsed;
    }

    public void setNoheapMemoryUsed(Long noheapMemoryUsed) {
        this.noheapMemoryUsed = noheapMemoryUsed;
    }

    public List<GCInfo> getGcInfos() {
        return gcInfos;
    }

    public void setGcInfos(List<GCInfo> gcInfos) {
        this.gcInfos = gcInfos;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public long getJvmMaxMemory() {
        return jvmMaxMemory;
    }

    public void setJvmMaxMemory(long jvmMaxMemory) {
        this.jvmMaxMemory = jvmMaxMemory;
    }

    @Override
    public String toString() {
        return "JVMInfo{" +
                "processId='" + processId + '\'' +
                ", jvmMaxMemory=" + jvmMaxMemory +
                ", heapMemoryMax=" + heapMemoryMax +
                ", heapMemoryUsed=" + heapMemoryUsed +
                ", noheapMemoryMax=" + noheapMemoryMax +
                ", noheapMemoryUsed=" + noheapMemoryUsed +
                ", gcInfos=" + gcInfos +
                '}';
    }
}
