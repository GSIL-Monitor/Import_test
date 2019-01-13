package com.jd.rec.nl.connector.storm.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.service.common.quartet.ApplicationLoader;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.quartet.processor.ParseProcessor;
import org.slf4j.Logger;
import storm.kafka.StringScheme;

import java.util.Map;

import static com.jd.rec.nl.connector.storm.Const.*;
import static org.slf4j.LoggerFactory.getLogger;

public class PreBolt extends BaseRichBolt {

    private static final Logger LOGGER = getLogger(PreBolt.class);

    private ApplicationLoader loader;

    private ParseProcessor parseProcessor;

    private OutputCollector outputCollector;

    public PreBolt(ApplicationLoader loader) {
        this.loader = loader;
    }

    @Override
    public void prepare(Map map, TopologyContext context, OutputCollector collector) {
        outputCollector = collector;
        this.parseProcessor = loader.getParseProcessor();
    }

    @Override
    public void execute(final Tuple tuple) {

        final String source = tuple.getSourceComponent();
        final String value = tuple.getStringByField(StringScheme.STRING_SCHEME_KEY);
        Message<String> message = new Message<>(source, value);
        try {
            final ImporterContext importerContext = parseProcessor.parse(message);
            if (importerContext != null) {
                outputCollector.emit(new Values(importerContext.getKey(), importerContext.getValue(),
                        System.currentTimeMillis()));
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            outputCollector.ack(tuple);
        }
    }


    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        Fields fields = new Fields(KEY_FIELD_NAME, VALUE_FIELD_NAME, TIMESTAMP);
        outputFieldsDeclarer.declare(fields);
    }

    @Override
    public void cleanup() {
    }
}