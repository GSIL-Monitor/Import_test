package com.jd.rec.nl.service.base.quartet;

import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/7/24
 */
public interface WindowCollector<K extends Serializable, V extends Serializable> extends Schedule {

    /**
     * 处理原始数据为需要的key-value pair进行暂存
     *
     * @param key
     * @param value
     */
    void collect(K key, V value);

    /**
     * 窗口大小
     *
     * @return
     */
    int windowSize();

    @Override
    default int intervalSize() {
        return windowSize();
    }

    /**
     * 分派,当一个时间窗口结束后将存储的数据按key分派
     *
     * @param resultCollection
     * @return key-value pair
     */
    void shuffle(ResultCollection resultCollection);

    @Override
    default void trigger(ResultCollection resultCollection) {
        shuffle(resultCollection);
    }
}
