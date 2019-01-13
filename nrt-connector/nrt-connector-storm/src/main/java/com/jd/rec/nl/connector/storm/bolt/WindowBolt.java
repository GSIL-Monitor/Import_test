package com.jd.rec.nl.connector.storm.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.jd.rec.nl.connector.storm.util.TupleType;
import com.jd.rec.nl.connector.storm.util.TupleType.Type;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.pubsub.ApplicationEvent;
import com.jd.rec.nl.core.pubsub.EventPublisher;
import com.jd.rec.nl.service.base.quartet.domain.MapResult;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.ApplicationLoader;
import com.jd.rec.nl.service.common.quartet.processor.WindowProcessor;
import org.slf4j.Logger;

import java.util.Map;

import static com.jd.rec.nl.connector.storm.Const.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/7/31
 */
public class WindowBolt extends TraceableBolt {

    public static final String signal = "_sig";

    private static final Logger LOGGER = getLogger(WindowBolt.class);

    ApplicationLoader loader;

    private WindowProcessor windowProcessor;

    private EventPublisher eventPublisher;

    public WindowBolt(ApplicationLoader loader) {
        this.loader = loader;
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        windowProcessor = loader.getWindowProcessor();
        eventPublisher = InjectorService.getCommonInjector().getInstance(EventPublisher.class);
        LOGGER.warn("task:{}", Thread.currentThread().getName());
        super.prepare(map, topologyContext, outputCollector);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        Fields fields = new Fields(KEY_FIELD_NAME, VALUE_FIELD_NAME, TIMESTAMP);
        outputFieldsDeclarer.declareStream(EXPORTER_STREAM_ID, fields);

        Fields reduceFields = new Fields(KEY_FIELD_NAME, VALUE_FIELD_NAME, TIMESTAMP);
        Fields sigFields = new Fields(TIMESTAMP);
        outputFieldsDeclarer.declareStream(REDUCER_STREAM_ID, reduceFields);
        outputFieldsDeclarer.declareStream(REDUCER_STREAM_ID.concat(signal), sigFields);
        super.declareOutputFields(outputFieldsDeclarer);
    }

    @Override
    public void execute(Tuple tuple) {
        if (TupleType.check(tuple) == Type.schedule) {
            scheduleTrigger(tuple);
        } else if (TupleType.check(tuple) == Type.appEvent) {
            publishEvent(tuple.getLongByField(TIMESTAMP), (ApplicationEvent) tuple.getValueByField(VALUE_FIELD_NAME));
        } else {
            LOGGER.debug("receive normal tuple:", tuple);
            try {
                final MapResult mapResult = (MapResult) tuple.getValueByField(VALUE_FIELD_NAME);
                windowProcessor.collect(mapResult);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                this.ack(tuple);
            }
        }
    }


    private void publishEvent(Long time, ApplicationEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void scheduleTrigger(Tuple tuple) {
        LOGGER.debug("receive tick tuple:{}", tuple);
        LOGGER.debug("{}:trigger window function", Thread.currentThread().getName());
        long start = System.currentTimeMillis();
        long scheduleTime = tuple.getLongByField(TIMESTAMP);
        int packageId = tuple.getIntegerByField(VALUE_FIELD_NAME);
        try {
            ResultCollection results = windowProcessor.shuffle(packageId);
            // 触发后就ack,防止影响spout的发送,处理延迟使用ump和ilog进行估算
            this.ack(tuple);
            MapResult result;
            while ((result = results.getNext()) != null) {
                if (result.isFin()) {
                    //                        LOGGER.debug("output:{}", result);
                    this.emit(EXPORTER_STREAM_ID, new Values(result.getKey(), result, scheduleTime));
                } else {
                    //                        LOGGER.debug("MapResult:{}", result);
                    this.emit(REDUCER_STREAM_ID, new Values(result.getKey(), result, scheduleTime));
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            this.emit(REDUCER_STREAM_ID.concat(signal), new Values(scheduleTime));
            long end = System.currentTimeMillis();
            LOGGER.debug("{}:window function finish, last :{}", Thread.currentThread().getName(), end - start);
        }
    }
}
