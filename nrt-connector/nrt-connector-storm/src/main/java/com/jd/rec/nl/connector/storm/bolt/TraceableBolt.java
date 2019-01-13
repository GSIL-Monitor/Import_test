package com.jd.rec.nl.connector.storm.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.jd.rec.nl.connector.storm.Const.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/8/29
 */
public abstract class TraceableBolt extends SynchronizedTupleCollectorBolt {

    private static final Logger LOGGER = getLogger(TraceableBolt.class);

    private ExecutorService executorService;

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        super.prepare(map, topologyContext, outputCollector);
        //        executorService = Executors.newSingleThreadExecutor();
        //        executorService.submit(() -> {
        //            RuntimeTrace runtimeTrace = InjectorService.getCommonInjector().getInstance(RuntimeTrace.class);
        //            LOGGER.debug("trace:{}", runtimeTrace.toString());
        //            while (true) {
        //                try {
        //                    List<byte[]> trace = runtimeTrace.getTraces();
        //                    if (trace != null && !trace.isEmpty()) {
        //                        this.emit(TRACE_STREAM_ID, new Values(trace, System.currentTimeMillis()));
        //                    }
        //                } catch (Exception e) {
        //                    LOGGER.error(e.getMessage(), e);
        //                }
        //            }
        //        });
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        Fields traceFields = new Fields(VALUE_FIELD_NAME, TIMESTAMP);
        outputFieldsDeclarer.declareStream(TRACE_STREAM_ID, traceFields);
    }
}
