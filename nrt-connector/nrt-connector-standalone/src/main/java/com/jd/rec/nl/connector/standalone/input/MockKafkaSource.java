package com.jd.rec.nl.connector.standalone.input;

import com.jd.rec.nl.connector.standalone.util.ThreadPoolRunner;
import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.input.Source;
import com.jd.rec.nl.core.input.domain.SourceConfig;
import com.jd.rec.nl.service.base.quartet.domain.KafkaConfig;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.Callable;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @ClassName MockKafkaSource
 * @Description TODO
 * @Author rocky
 * @Date 2018/11/27 下午4:35
 * @Version 1.0
 */
public class MockKafkaSource implements Source<Message<String>> {

    private static final Logger LOGGER = getLogger(MockKafkaSource.class);
    private String name = "mock-kafka-source";
    private Queue<MockContent> waitForPop = new ArrayDeque<>();
    private Long duration;
    private Long tick;
    private Integer limit;
    private Set<SourceConfig> configs;

    public MockKafkaSource(Set<SourceConfig> configs, Integer limit, Long duration) {
        LOGGER.debug("kafka init");
        this.duration = duration;
        this.limit = limit;
        this.configs = configs;
        this.tick = System.currentTimeMillis();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Message<String> get() {
        if (waitForPop.size() != 0) {
            MockContent content = waitForPop.poll();
            Message<String> message = new Message<>(content.getName(), content.getValue());
            return message;
        } else if (duration >= System.currentTimeMillis() - tick) {
            Set<Callable<Queue<MockContent>>> futureList = new HashSet<>();
            ThreadPoolRunner<Queue<MockContent>> pool = new ThreadPoolRunner<>();
            configs.forEach(config -> futureList.add(() -> {
                KafkaConfig kafkaConfig = (KafkaConfig) config;
                if (kafkaConfig.getEnable()) {
                    LOGGER.debug("kafka topic: " + kafkaConfig.getName());
                    MockKafkaConsumer2Queue mockKafkaConsumer2Queue = new MockKafkaConsumer2Queue(kafkaConfig, limit);
                    return mockKafkaConsumer2Queue.fetch();
                }
                return null;
            }));
            List<Queue<MockContent>> resQueue = pool.run(20, futureList, duration);
            resQueue.stream().filter(Objects::nonNull).forEach(q -> waitForPop.addAll(q));
            MockContent content = waitForPop.poll();
            Message<String> message = new Message<>(content.getName(), content.getValue());
            return message;
        }
        return null;
    }
}
