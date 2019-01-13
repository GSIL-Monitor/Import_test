package com.jd.rec.nl.connector.storm.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import java.util.Map;

/**
 * 同步的emit(原计划)，目前看会比较大影响效率，而同步问题并不明显，因此目前还是并行处理
 *
 * @author linmx
 * @date 2018/10/10
 */
public abstract class SynchronizedTupleCollectorBolt extends BaseRichBolt {

    protected OutputCollector outputCollector;

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.outputCollector = outputCollector;
    }

    protected void ack(Tuple tuple) {
        //        synchronized (this.outputCollector) {
        this.outputCollector.ack(tuple);
        //        }
    }

    protected void emit(String streamId, Values tuple) {
        //        synchronized (this.outputCollector) {
        this.outputCollector.emit(streamId, tuple);
        //        }
    }

    protected void emit(Values tuple) {
        //        synchronized (this.outputCollector) {
        this.outputCollector.emit(tuple);
        //        }
    }
}
