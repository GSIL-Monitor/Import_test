package com.jd.rec.nl.connector.storm.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.jd.rec.nl.connector.storm.util.TupleType;
import com.jd.rec.nl.connector.storm.util.TupleType.Type;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.pubsub.ApplicationEvent;
import com.jd.rec.nl.core.pubsub.EventPublisher;
import com.jd.rec.nl.service.base.quartet.domain.MapResult;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.ApplicationLoader;
import com.jd.rec.nl.service.common.quartet.processor.KeyedUpdateProcessor;
import org.slf4j.Logger;

import java.util.Map;

import static com.jd.rec.nl.connector.storm.Const.*;
import static org.slf4j.LoggerFactory.getLogger;

public class KeyedStreamAppBolt extends SynchronizedTupleCollectorBolt {

    private static final Logger LOGGER = getLogger(AppBolt.class);

    private int order;

    private ApplicationLoader loader;

    private KeyedUpdateProcessor updateProcessor;

    private EventPublisher eventPublisher;

    public KeyedStreamAppBolt(ApplicationLoader loader, int order) {
        this.loader = loader;
        this.order = order;
    }

    @Override
    public void prepare(Map map, TopologyContext context, OutputCollector collector) {
        this.updateProcessor = loader.getKeyedUpdateProcessor(order);
        eventPublisher = InjectorService.getCommonInjector().getInstance(EventPublisher.class);
        super.prepare(map, context, collector);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        Fields fields = new Fields(KEY_FIELD_NAME, VALUE_FIELD_NAME, TIMESTAMP);
        outputFieldsDeclarer.declareStream(EXPORTER_STREAM_ID, fields);
        outputFieldsDeclarer.declareStream(KEYED_STREAM_ID, fields);
        if (this.loader.hasWindow()) {
            Fields windowFields = new Fields(KEY_FIELD_NAME, VALUE_FIELD_NAME, TIMESTAMP);
            outputFieldsDeclarer.declareStream(WINDOW_STREAM_ID, windowFields);
        }
    }

    @Override
    public void execute(final Tuple tuple) {

        LOGGER.debug("receive tuple:{}", tuple);

        if (TupleType.check(tuple) == Type.schedule) {
            scheduleTrigger(tuple);
        } else if (TupleType.check(tuple) == Type.appEvent) {
            publishEvent(tuple.getLongByField(TIMESTAMP), (ApplicationEvent) tuple.getValueByField(VALUE_FIELD_NAME));
        } else {
            try {
                final MapResult mapResult = (MapResult) tuple.getValueByField(VALUE_FIELD_NAME);
                long sessionTime = tuple.getLongByField(TIMESTAMP);
                final ResultCollection resultCollection = updateProcessor.update(mapResult);
                MapResult result;
                while ((result = resultCollection.getNext()) != null) {
                    if (result.isFin()) {
                        this.emit(EXPORTER_STREAM_ID, new Values(result.getKey(), result, sessionTime));
                    } else {
                        if (result.isForWindow()) {
                            this.emit(WINDOW_STREAM_ID, new Values(result.getKey(), result, sessionTime));
                        } else {
                            this.emit(KEYED_STREAM_ID, new Values(result.getKey(), result, sessionTime));
                        }
                    }
                }
            } catch (InvalidDataException e) {
                LOGGER.debug(e.getMessage(), e);
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
        if (!this.updateProcessor.hasSchedule()) {
            this.ack(tuple);
            return;
        }
        LOGGER.debug("receive tick tuple:{}", tuple);
        LOGGER.debug("{}:trigger schedule function", Thread.currentThread().getName());
        long start = System.currentTimeMillis();
        long scheduleTime = tuple.getLongByField(TIMESTAMP);
        int packageId = tuple.getIntegerByField(VALUE_FIELD_NAME);
        try {
            ResultCollection results = this.updateProcessor.scheduleTrigger(packageId);
            // 触发后就ack,防止影响spout的发送,处理延迟使用ump和ilog进行估算
            this.ack(tuple);
            MapResult result;
            while ((result = results.getNext()) != null) {
                if (result.isFin()) {
                    //                        LOGGER.debug("output:{}", result);
                    this.emit(EXPORTER_STREAM_ID, new Values(result.getKey(), result, scheduleTime));
                } else {
                    //                        LOGGER.debug("MapResult:{}", result);
                    this.emit(WINDOW_STREAM_ID, new Values(result.getKey(), result, scheduleTime));
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            long end = System.currentTimeMillis();
            LOGGER.debug("{}:schedule function finish, last :{}", Thread.currentThread().getName(), end - start);
        }
    }

}
