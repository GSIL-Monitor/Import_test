package com.jd.rec.nl.connector.storm.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import com.jd.rec.nl.service.infrastructure.ILogKafka;
import com.jd.rec.nl.core.guice.InjectorService;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import static com.jd.rec.nl.connector.storm.Const.VALUE_FIELD_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/8/29
 */
public class TraceBolt extends BaseRichBolt {

    private static final Logger LOGGER = getLogger(TraceBolt.class);

    private ILogKafka iLogKafka;

    private OutputCollector outputCollector;

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.outputCollector = outputCollector;
        iLogKafka = InjectorService.getCommonInjector().getInstance(ILogKafka.class);
    }

    @Override
    public void execute(Tuple tuple) {
        try {
            final List<byte[]> result = (List<byte[]>) tuple.getValueByField(VALUE_FIELD_NAME);
            iLogKafka.trace(result);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            this.outputCollector.ack(tuple);
        }
    }


    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

    }
}
