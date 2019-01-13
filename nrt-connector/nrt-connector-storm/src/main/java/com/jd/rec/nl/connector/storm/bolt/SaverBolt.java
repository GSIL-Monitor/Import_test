package com.jd.rec.nl.connector.storm.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import com.jd.rec.nl.connector.storm.util.TupleType;
import com.jd.rec.nl.connector.storm.util.TupleType.Type;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.pubsub.ApplicationEvent;
import com.jd.rec.nl.core.pubsub.EventPublisher;
import com.jd.rec.nl.service.base.quartet.domain.OutputResult;
import com.jd.rec.nl.service.common.quartet.ApplicationLoader;
import com.jd.rec.nl.service.common.quartet.processor.ExportProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.jd.rec.nl.connector.storm.Const.TIMESTAMP;
import static com.jd.rec.nl.connector.storm.Const.VALUE_FIELD_NAME;


public class SaverBolt extends TraceableBolt {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaverBolt.class);

    private ApplicationLoader loader;

    private ExportProcessor exportProcessor;

    private EventPublisher eventPublisher;

    public SaverBolt(ApplicationLoader loader) {
        this.loader = loader;
    }


    @Override
    public void prepare(Map map, TopologyContext context, OutputCollector collector) {
        this.exportProcessor = this.loader.getExportProcessor();
        eventPublisher = InjectorService.getCommonInjector().getInstance(EventPublisher.class);
        super.prepare(map, context, collector);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        super.declareOutputFields(outputFieldsDeclarer);
    }

    @Override
    public void execute(final Tuple tuple) {
        if (TupleType.check(tuple) == Type.appEvent) {
            publishEvent(tuple.getLongByField(TIMESTAMP),
                    (ApplicationEvent) tuple.getValueByField(VALUE_FIELD_NAME));
            return;
        }

        final OutputResult result = (OutputResult) tuple.getValueByField(VALUE_FIELD_NAME);
        try {
            exportProcessor.export(result);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            this.ack(tuple);
        }
    }

    private void publishEvent(Long time, ApplicationEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void cleanup() {
    }
}