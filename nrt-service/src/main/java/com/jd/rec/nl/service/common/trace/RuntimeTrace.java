package com.jd.rec.nl.service.common.trace;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.rec.nl.service.infrastructure.ILogKafka;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/8/28
 */
@Singleton
public class RuntimeTrace {

    public static final int batchSize = 300;

    private static final Logger LOGGER = getLogger(RuntimeTrace.class);

    private static Gson gson = new Gson();

    private static String host;

    private static String ip;

    static {
        host = MonitorUtils.getHost();
        ip = MonitorUtils.getHostIP();
    }

    @Inject
    private ILogKafka iLogKafka;

    //    KafkaProducer<byte[], byte[]> producer;

    //    private AtomicBoolean isReady = new AtomicBoolean(false);

    //    public RuntimeTrace() {
    //
    //        LOGGER.error("init iLog");
    //        String clientId = "nrt-platform";
    //        String brokerHost = "172.28.117.120:9092,172.28.117.116:9092,172.28.117.118:9092";
    //        String serializerClass = "org.apache.kafka.common.serialization.ByteArraySerializer";
    //        Properties jdqProp = JDQUtil.getProperties("null", "null", brokerHost, clientId, serializerClass);
    //        jdqProp.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 6 * 1024 * 1024L);
    //        producer = new KafkaProducer(jdqProp);
    //        try {
    //            producer.send(new ProducerRecord(TOPIC, String.format("start at:%s", DateFormatUtils.format(new Date()
    //                    , "yyyy-MM-dd HH:mm:ss.SSS")).getBytes()));
    //        } catch (Exception e) {
    //            LOGGER.error(e.getMessage(), e);
    //        } finally {
    //            LOGGER.error("init iLog succeed");
    //            isReady.set(true);
    //        }
    //    }

    private LinkedBlockingDeque<byte[]> messages = new LinkedBlockingDeque<>();

    public <T> void info(String source, T message) {
        doTrace(source, message, Level.INFO);
    }

    public <T> void error(String source, T message) {
        doTrace(source, message, Level.ERROR);
    }

    public <T> void warn(String source, T message) {
        doTrace(source, message, Level.WARN);
    }

    private <T> void doTrace(String source, T message, Level level) {
        //        LOGGER.debug("add trace log:{},{}", source, message);
        try {
            JsonLoggerInfo json = new JsonLoggerInfo(host, ip, source, message, level.name(), System.currentTimeMillis());
            String data = json.toString();
            iLogKafka.trace(Collections.singletonList(data.getBytes()));
            //            messages.addLast(data.getBytes());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
        }
    }

    /**
     * 获取trace数据
     *
     * @return
     */
    public List<byte[]> getTraces() {
        try {
            List<byte[]> waitToEmit = new ArrayList<>();

            while (waitToEmit.size() < batchSize) {
                byte[] value = messages.pollFirst();
                if (value == null) {
                    break;
                }
                waitToEmit.add(value);
            }
            if (waitToEmit.isEmpty()) {
                byte[] value = messages.takeFirst();
                waitToEmit.add(value);
                while (waitToEmit.size() < batchSize) {
                    value = messages.pollFirst();
                    if (value == null) {
                        break;
                    }
                    waitToEmit.add(value);
                }
            }
            return waitToEmit;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }


    /**
     * 输出的日志内容
     */
    public static class JsonLoggerInfo<T> {

        private String source;

        private String hostName;

        private String ip;

        /**
         * 项目名
         */
        private String type = "nrt";

        /**
         * 日志信息
         */
        private String message;

        /**
         * 日志级别
         */
        private String level;

        /**
         * 日志时间
         */
        @SerializedName("@timestamp")
        private String UTCTime;

        private String time;

        public JsonLoggerInfo(String hostName, String hostIP, String source, T message, String level, long timeMillis) {
            this.source = source;
            Map<String, Object> messageObject = new HashMap<>();
            if (message instanceof String) {
                messageObject.put(source, new CommonMsg((String) message));
            } else {
                messageObject.put(source, message);
            }
            this.message = gson.toJson(messageObject);
            this.level = level;
            this.hostName = hostName;
            this.ip = hostIP;
            this.UTCTime = DateFormatUtils.formatUTC(timeMillis, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            this.time = DateFormatUtils.format(timeMillis, "yyyy-MM-dd HH:mm:ss.SSS");
        }

        public static JsonLoggerInfo fromJson(byte[] value) {
            return gson.fromJson(new String(value), JsonLoggerInfo.class);
        }

        public String getSource() {
            return source;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public String getLevel() {
            return level;
        }

        public String getUTCTime() {
            return UTCTime;
        }

        @Override
        public String toString() {
            return gson.toJson(this);
            //            return super.toString();
        }

        class CommonMsg {

            String value;

            public CommonMsg(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }
    }
}
