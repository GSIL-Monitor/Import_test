package com.jd.rec.nl.connector.storm.spout;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.jd.rec.nl.service.base.quartet.WindowCollector;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.jd.rec.nl.connector.storm.Const.TIMESTAMP;
import static com.jd.rec.nl.connector.storm.Const.VALUE_FIELD_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/9/5
 */
public class ScheduleSpout extends BaseRichSpout {

    private static final Logger LOGGER = getLogger(ScheduleSpout.class);

    int packageId = 0;

    private SpoutOutputCollector spoutOutputCollector;

    private ScheduledExecutorService executorService;

    private Object _lock;

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this.spoutOutputCollector = spoutOutputCollector;
        _lock = new Object();
        packageId = 0;
        executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(() -> {
                    try {
                        long wakeupTime = System.currentTimeMillis();
                        Values values = new Values(packageId++, wakeupTime);
                        LOGGER.error("emit schedule:{}", values);
                        this.spoutOutputCollector.emit(values);
                        if (packageId >= 10000000) {
                            packageId = 1;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, WindowCollector.defaultInterval.getSeconds(),
                WindowCollector.defaultInterval.getSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void nextTuple() {
        try {
            Thread.sleep(100L);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        Fields sigFields = new Fields(VALUE_FIELD_NAME, TIMESTAMP);
        outputFieldsDeclarer.declare(sigFields);
    }
}
