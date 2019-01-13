package com.jd.rec.nl.service.base.quartet.domain;

import java.time.Duration;

/**
 * 最终输出的处理结果
 *
 * @author linmx
 * @date 2018/7/27
 */
public class OutputResult extends MapResult<String, byte[]> {

    String type;

    int subType;

    Duration ttl;

    public OutputResult(String executorName, String type, int subType, String key, byte[] value, Duration ttl) {
        super(executorName, key, value);
        this.type = type;
        this.subType = subType;
        this.ttl = ttl;
    }

    public String getType() {
        return type;
    }

    public int getSubType() {
        return subType;
    }

    public Duration getTtl() {
        return ttl;
    }

    @Override
    public String toString() {
        return String.format("executor:%s, type:%s, subType:%d, key:%s, value:%s", this.executorName, this.type, this.subType,
                this.key, this.value);
    }

    @Override
    public boolean isFin() {
        return true;
    }
}
