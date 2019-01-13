package com.jd.rec.nl.service.base.quartet.domain;

import com.jd.rec.nl.service.base.quartet.Reducer;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.WindowCollector;
import com.jd.ump.profiler.proxy.Profiler;
import org.slf4j.Logger;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/4
 */
public class ResultCollection {

    private static final Logger LOGGER = getLogger(ResultCollection.class);

    private volatile AtomicBoolean finish = new AtomicBoolean(false);

    private Deque<MapResult> results = new ConcurrentLinkedDeque<>();

    private AtomicLong monitorCount = new AtomicLong(0);

    private Map<String, AtomicLong> outputCounts = new ConcurrentHashMap<>();

    public Deque<MapResult> getResults() {
        return results;
    }

    public void addResult(MapResult keyValuePair) {
        monitorCount.incrementAndGet();
        results.addLast(keyValuePair);
    }

    /**
     * 增加分片输出数据
     *
     * @param executorName 使用{@link Updater#getName()} or {@link WindowCollector#getName()} or {@link Reducer#getName()}获取
     * @param key          键
     * @param value        值
     * @param <K>          键类型
     * @param <V>          值类型
     */
    public <K extends Serializable, V extends Serializable> void addShardingValue(String executorName, K key, V value) {
        monitorCount.incrementAndGet();
        MapResult result = new MapResult(executorName, key, value, false);
        results.addLast(result);
    }

    /**
     * 增加输出数据
     *
     * @param executorName 使用{@link Updater#getName()} or {@link WindowCollector#getName()} or {@link Reducer#getName()}获取
     * @param key          键
     * @param value        值
     * @param <K>          键类型
     * @param <V>          值类型
     */
    public <K extends Serializable, V extends Serializable> void addMapResult(String executorName, K key, V value) {
        monitorCount.incrementAndGet();
        MapResult result = new MapResult(executorName, key, value);
        results.addLast(result);
    }

    /**
     * 增加最终输出结果
     *
     * @param executorName 使用{@link Updater#getName()} or {@link WindowCollector#getName()} or {@link Reducer#getName()}获取
     * @param serviceType  service类型,参考 {@link com.jd.feeder.pipeline.ServiceType} 的枚举值
     * @param subType      模型id
     * @param key          键,字符串类型
     * @param value        值,序列化后的byte数组
     * @param ttl          存活时间
     */
    public void addOutput(String executorName, String serviceType, int subType, String key, byte[] value,
                          Duration ttl) {
        monitorCount.incrementAndGet();
        monitor(executorName, serviceType, subType, key, value);
        OutputResult finResult = new OutputResult(executorName, serviceType, subType, key, value, ttl);
        results.addLast(finResult);
    }

    private void monitor(String executorName, String serviceType, int subType, String key, byte[] value) {
        String monitorKey = serviceType.concat("-").concat(String.valueOf(subType));
        Profiler.countAccumulate(monitorKey);
    }

    public void addAll(Collection<MapResult> results) {
        monitorCount.addAndGet(results.size());
        this.results.addAll(results);
    }

    public MapResult getNext() {
        while (true) {
            if (!this.results.isEmpty()) {
                monitorCount.decrementAndGet();
                return this.results.pollFirst();
            } else {
                if (isFinish()) {
                    if (!this.results.isEmpty()) {
                        monitorCount.decrementAndGet();
                        return this.results.pollFirst();
                    } else {
                        if (monitorCount.intValue() != 0) {
                            LOGGER.error("result collection has lost!!check sync! lost num:{}",
                                    monitorCount.intValue());
                        }
                        return null;
                    }
                }
            }
        }
    }

    public void autoFinish(ExecutorService finishExecutor, Future future, Duration timeout) {
        finishExecutor.submit(() -> {
            try {
                future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                LOGGER.error("operator timeout!!![{}]", Thread.currentThread().getName());
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                if (!future.isDone()) {
                    future.cancel(false);
                }
                this.finish();
            }
        });
    }

    public boolean isFinish() {
        return finish.get();
    }

    public void finish() {
        this.finish.set(true);
    }

    public int size() {
        return results.size();
    }

}
