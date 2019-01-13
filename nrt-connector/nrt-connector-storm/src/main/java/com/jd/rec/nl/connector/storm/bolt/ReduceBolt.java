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
import com.jd.rec.nl.service.common.quartet.processor.ReduceProcessor;
import com.jd.ump.profiler.proxy.Profiler;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static com.jd.rec.nl.connector.storm.Const.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/7/11
 */
public class ReduceBolt extends TraceableBolt {

    private static final int batchSize = 10;

    private static final Logger LOGGER = getLogger(ReduceBolt.class);

    private ApplicationLoader loader;

    private ReduceProcessor reduceProcessor;

    private EventPublisher eventPublisher;

    /**
     * 来源的并行度,用于判断这一周期中是否所有的数据都收到了
     */
    private int sourceParallelism;

    /**
     * 当前周期收到的signal数量
     */
    private int signalNum = 0;

    /**
     * 当前周期的各个source的时间戳,当触发reduce后做清理
     * 同一个周期同一个task送过来的时间戳是一样的,当存在不一样的情况时,说明有tuple丢失
     * 当新送达的时间戳大于当前保存的,则直接触发整体reduce,并记录新的数据
     * 当新时间戳小于当前保存的,说明是旧数据,直接丢弃(一般不存在这种情况)
     */
    private Map<String, Long> sourceTimestamp = new HashMap<>();

    public ReduceBolt(ApplicationLoader loader, int windowParallelism) {
        this.loader = loader;
        this.sourceParallelism = windowParallelism;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        reduceProcessor = loader.getReduceProcessor();
        eventPublisher = InjectorService.getCommonInjector().getInstance(EventPublisher.class);
        super.prepare(stormConf, context, collector);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        Fields fields = new Fields(KEY_FIELD_NAME, VALUE_FIELD_NAME, TIMESTAMP);
        declarer.declare(fields);
        super.declareOutputFields(declarer);
    }

    @Override
    public void execute(Tuple input) {
        try {
            if (TupleType.check(input) == Type.appEvent) {
                publishEvent(input.getLongByField(TIMESTAMP),
                        (ApplicationEvent) input.getValueByField(VALUE_FIELD_NAME));
                return;
            }

            LOGGER.debug("{}:receive tuple:{}-{}", Thread.currentThread().getName(), input.getSourceComponent(),
                    input.getSourceTask());
            // 判断是否收到重复的来源,防止由于环境问题导致tuple丢失后处理的不确定性
            String source = input.getSourceComponent().concat("-").concat(String.valueOf(input.getSourceTask()));
            long timestamp = input.getLongByField(TIMESTAMP);
            if (this.sourceTimestamp.containsKey(source)) {
                long storedTime = this.sourceTimestamp.get(source);
                if (storedTime > timestamp) {
                    // 说明是旧数据,丢弃
                    LOGGER.warn("tuple is expired:{}", input);
                    return;
                } else if (storedTime < timestamp) {
                    // 说明有数据丢失,上一周期的reduce未触发,直接触发reduce
                    LOGGER.warn("the last cycle reduce has missed:{}, receive num:{}\n{}", storedTime,
                            this.sourceTimestamp
                                    .size(),
                            this.sourceTimestamp);
                    Profiler.businessAlarm("nrt_origin_reducerBolt",
                            "the last cycle reduce has missed! Receive num:" + this
                                    .sourceTimestamp.size());
                    triggerReduce();
                    this.signalNum = 0;
                    this.sourceTimestamp.clear();
                }
            }

            this.sourceTimestamp.put(source, timestamp);

            if (isSignal(input)) {
                this.signalNum++;
                if (this.signalNum == sourceParallelism) {
                    LOGGER.debug("trigger this cycle reduce! source:{}", this.sourceTimestamp);
                    triggerReduce();
                    this.signalNum = 0;
                    this.sourceTimestamp.clear();
                }
            } else {
                MapResult value = (MapResult) input.getValueByField(VALUE_FIELD_NAME);
                reduceProcessor.gatherSharding(value);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            this.ack(input);
        }
    }

    private void publishEvent(Long time, ApplicationEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 进行reduce操作
     */
    private void triggerReduce() {
        ResultCollection operateResult = reduceProcessor.reduce();
        long current = System.currentTimeMillis();
        MapResult result;
        while ((result = operateResult.getNext()) != null) {
            if (result.isFin()) {
                this.emit(new Values(result.getKey(), result, current));
            } else {
                LOGGER.error("reducer return error result:{}", result.toString());
            }
        }
    }

    private boolean isSignal(Tuple input) {
        return input.getSourceStreamId().endsWith(WindowBolt.signal);
    }
}
