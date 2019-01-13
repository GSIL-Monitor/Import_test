package com.jd.rec.nl.service.base.quartet.domain;

import com.jd.rec.nl.core.input.domain.SourceConfig;
import com.typesafe.config.Config;

import java.util.List;
import java.util.Set;

/**
 * @author linmx
 * @date 2018/5/29
 */
public class KafkaConfig implements SourceConfig {


    /**
     * 存放offset的zk
     */
    private List<String> offsetZk;

    /**
     * offset zk的端口
     */
    private int offsetPort;

    /**
     * kafka使用的zk
     */
    private String kafkaZk;

    /**
     * 订阅的topic
     */
    private String topic;

    /**
     * 并行度
     */
    private int parallelism;

    /**
     * debug下 是否可用
     */
    private Boolean enable;

    public KafkaConfig(List<String> offsetZk, int offsetPort, String kafkaZk,
                       String topic, int parallelism, Boolean enable) {
        this.offsetZk = offsetZk;
        this.offsetPort = offsetPort;
        this.kafkaZk = kafkaZk;
        this.topic = topic;
        this.parallelism = parallelism;
        this.enable = enable;
    }

    public KafkaConfig() {
    }

    public static KafkaConfig parseConfig(Config config, Boolean enable) {
        Set<String> keys = config.root().keySet();
        String name = keys.iterator().next();
        List<String> offsetZk = config.getStringList(name + ".offset_zk");
        int offsetPort = config.getInt(name + ".offset_port");
        String kafkaZk = config.getString(name + ".kafka_zk");
        String topic = config.getString(name + ".topic");
        int parallelism = config.getInt(name + ".parallelism");
        KafkaConfig kafkaConfig = new KafkaConfig(offsetZk, offsetPort, kafkaZk, topic, parallelism, enable);
        return kafkaConfig;
    }

    public int getParallelism() {
        return parallelism;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public int getOffsetPort() {
        return offsetPort;
    }

    public List<String> getOffsetZk() {

        return offsetZk;
    }

    public void setOffsetZk(List<String> offsetZk) {
        this.offsetZk = offsetZk;
    }

    public String getKafkaZk() {
        return kafkaZk;
    }

    public void setKafkaZk(String kafkaZk) {
        this.kafkaZk = kafkaZk;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == KafkaConfig.class && ((KafkaConfig) obj).getKafkaZk().equals(this.getKafkaZk())
                && ((KafkaConfig) obj).getOffsetZk().equals(this.getOffsetZk())
                && ((KafkaConfig) obj).getTopic().equals(this.getTopic());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("kafka:");
        sb.append("topic->").append(this.topic).append(", zk->").append(this.kafkaZk);
        return sb.toString();
    }

    @Override
    public String getName() {
        return topic;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("not supported");
    }
}
