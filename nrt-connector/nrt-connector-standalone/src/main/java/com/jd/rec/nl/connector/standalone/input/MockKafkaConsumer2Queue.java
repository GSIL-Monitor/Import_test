package com.jd.rec.nl.connector.standalone.input;

import com.jd.rec.nl.service.base.quartet.domain.KafkaConfig;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import org.slf4j.Logger;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @ClassName MockKafkaConsumer2Queue
 * @Description TODO
 * @Author rocky
 * @Date 2018/11/22 下午4:10
 * @Version 1.0
 */
public class MockKafkaConsumer2Queue {


    private static final Logger LOGGER = getLogger(MockKafkaConsumer2Queue.class);

    Map<String, List<KafkaStream<byte[], byte[]>>> topicStreamsMap;
    private String topic;
    private Integer limit;
    private ConsumerConnector consumerConn;

    public MockKafkaConsumer2Queue(KafkaConfig kafkaConfig, Integer limit) {
        Properties props = new Properties();
        props.put("group.id", "nrt-standalone" + UUID.randomUUID());
        props.put("zookeeper.connect", kafkaConfig.getKafkaZk());
        props.put("zookeeper.session.timeout.ms", "5000");
        props.put("zookeeper.connection.timeout.ms", "10000");
        props.put("rebalance.backoff.ms", "2000");
        props.put("rebalance.max.retries", "10");
        props.put("auto.offset.reset", "largest");
        props.put("auto.commit.interval.ms", "1000");
        props.put("partition.assignment.strategy", "roundrobin");
        ConsumerConfig consumerConfig = new ConsumerConfig(props);
        String topic = kafkaConfig.getTopic();
        consumerConn = Consumer.createJavaConsumerConnector(consumerConfig);
        Map<String, Integer> topicCountMap = new HashMap<>();
        topicCountMap.put(topic, 1);
        this.topicStreamsMap = consumerConn.createMessageStreams(topicCountMap);
        this.topic = topic;
        this.limit = limit;
    }

    public Queue<MockContent> fetch() {
        Queue<MockContent> waitForPop = new ArrayDeque<>();
        KafkaStream<byte[], byte[]> stream = topicStreamsMap.get(topic).get(0);
        LOGGER.debug("client id :" + stream.clientId());
        ConsumerIterator<byte[], byte[]> it = stream.iterator();
        while (it.hasNext()) {
            MessageAndMetadata<byte[], byte[]> data = it.next();
            String msg = new String(data.message());
            try {
                MockContent contents = new MockContent();
                contents.setName(topic);
                contents.setValue(msg);
                LOGGER.debug(topic + " msg : " + msg);
                waitForPop.add(contents);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (Objects.nonNull(limit) && waitForPop.size() >= limit) {
                break;
            }
        }
        consumerConn.shutdown();
        return waitForPop;
    }
}
