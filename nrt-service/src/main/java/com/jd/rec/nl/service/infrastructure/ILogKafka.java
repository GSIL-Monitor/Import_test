package com.jd.rec.nl.service.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.rec.nl.service.common.trace.RuntimeTrace;
import com.jd.unifiedfeed.feedclient.util.JDQUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/9/30
 */
@Singleton
public class ILogKafka implements BaseInfrastructure {

    private static final Logger LOGGER = getLogger(ILogKafka.class);

    private final static String TOPIC = "nrt";

    private static KafkaProducer<byte[], byte[]> producer;

    @Inject(optional = true)
    @ENV("iLog.traceFlag")
    private boolean traceFlag = true;

    private String host;

    private String ip;

//    protected ILogKafka(boolean flag) {
//    }
//
//    public ILogKafka() {
//        host = MonitorUtils.getHost();
//        ip = MonitorUtils.getHostIP();
//        String clientId = "nrt-platform";
//        String brokerHost = "172.28.117.120:9092,172.28.117.116:9092,172.28.117.118:9092";
//        String serializerClass = "org.apache.kafka.common.serialization.ByteArraySerializer";
//        Properties jdqProp = JDQUtil.getProperties("null", "null", brokerHost, clientId, serializerClass);
//        jdqProp.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 128 * 1024 * 1024L);
//        producer = new KafkaProducer(jdqProp);
//        try {
//            LOGGER.warn("init iLog!");
//            RuntimeTrace.JsonLoggerInfo json = new RuntimeTrace.JsonLoggerInfo(host, ip, "traceRestart",
//                    String.format("start at:%s", DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS")),
//                    Level.INFO.name(), System.currentTimeMillis());
//            String data = json.toString();
//            producer.send(new ProducerRecord(TOPIC, data.getBytes()));
//            LOGGER.warn("init iLog succeed!");
//        } catch (Exception e) {
//            LOGGER.error(e.getMessage(), e);
//        }
//    }

    @Inject
    public void initProducer(@ENV("iLog.clientId") String clientId, @ENV("iLog.brokerHost") String brokerHost) {
        host = MonitorUtils.getHost();
        ip = MonitorUtils.getHostIP();
        String serializerClass = "org.apache.kafka.common.serialization.ByteArraySerializer";
        Properties jdqProp = JDQUtil.getProperties("null", "null", brokerHost, clientId, serializerClass);
        // 以下均为了降低吞吐量
        jdqProp.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 64 * 1024 * 1024L);
        //        jdqProp.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 2);
        jdqProp.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        producer = new KafkaProducer(jdqProp);
        try {
            LOGGER.warn("init iLog!");
            RuntimeTrace.JsonLoggerInfo json = new RuntimeTrace.JsonLoggerInfo(host, ip, "traceRestart",
                    String.format("start at:%s", DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS")),
                    Level.INFO.name(), System.currentTimeMillis());
            String data = json.toString();
            producer.send(new ProducerRecord(TOPIC, data.getBytes()));
            LOGGER.warn("init iLog succeed!");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void trace(List<byte[]> result) {
        if (this.traceFlag) {
            result.stream().forEach(trace -> {
                RuntimeTrace.JsonLoggerInfo jsonLoggerInfo = RuntimeTrace.JsonLoggerInfo.fromJson(trace);
                if (jsonLoggerInfo.getLevel().equals(Level.WARN.name())) {
                    LOGGER.warn("{}: {}", jsonLoggerInfo.getSource(), new String(trace));
                }
                producer.send(new ProducerRecord<>(TOPIC, trace));
            });
        }
    }

}
