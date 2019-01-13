package com.jd.rec.nl.core.infrastructure.domain;

import java.time.Duration;

/**
 * @author linmx
 * @date 2018/7/19
 */
public class ThriftParameter {

    /**
     * 廊坊连接
     */
    String connectionLF;
    /**
     * 马驹桥连接
     */
    String connectionMJQ;
    /**
     * 汇天连接
     */
    String connectionHT;
    /**
     * zk命名空间
     */
    String jdns;
    /**
     * 超时时间
     */
    Duration timeout;
    /**
     * 最小idle
     */
    int minIdle;
    /**
     * 最大idle
     */
    int maxIdle;
    /**
     * 最大连接数
     */
    int maxActive;
    /**
     * 最大等待时间
     */
    Duration maxWaitTime;
    /**
     *
     */
    String exhaustedAction;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("connect:[\n").append(connectionLF).append("\n").append(connectionMJQ).append("\n").append(connectionHT)
                .append("\n]").append("jdns:").append(jdns);
        return sb.toString();
    }

    public String getConnection(CLUSTER cluster) {
        if (cluster == CLUSTER.LF) {
            return getConnectionLF();
        } else if (cluster == CLUSTER.MJQ) {
            return getConnectionMJQ();
        } else {
            return getConnectionHT();
        }
    }

    public String getConnectionLF() {
        return connectionLF;
    }

    public void setConnectionLF(String connectionLF) {
        this.connectionLF = connectionLF;
    }

    public String getConnectionMJQ() {
        return connectionMJQ;
    }

    public void setConnectionMJQ(String connectionMJQ) {
        this.connectionMJQ = connectionMJQ;
    }

    public String getConnectionHT() {
        return connectionHT;
    }

    public void setConnectionHT(String connectionHT) {
        this.connectionHT = connectionHT;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public Duration getMaxWaitTime() {
        return maxWaitTime;
    }

    public void setMaxWaitTime(Duration maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    public String getExhaustedAction() {
        return exhaustedAction;
    }

    public void setExhaustedAction(String exhaustedAction) {
        this.exhaustedAction = exhaustedAction;
    }

    public String getJdns() {
        return jdns;
    }

    public void setJdns(String jdns) {
        this.jdns = jdns;
    }
}
