package com.jd.rec.nl.service.base.quartet;

import com.jd.rec.nl.core.domain.KeyValue;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;

import java.io.Serializable;
import java.time.Duration;

/**
 * 标识性接口，用于标识处理转换分片维度后的数据
 */
public interface KeyedUpdater<K, V> extends Operator {

    /**
     * 更新操作,支持输出多个处理结果
     *
     * @param keyValue         分片好的数据
     * @param resultCollection 保存更新结果的数据结构
     *                         对于实时处理的输出,调用
     *                         {@link ResultCollection#addOutput(String, String, int, String, byte[], Duration)} 输出处理结果<br/>
     *                         对于窗口处理,调用 {@link ResultCollection#addMapResult(String, Serializable, Serializable)} 输出结果
     * @throws Exception
     */
    void update(KeyValue<K, V> keyValue, ResultCollection resultCollection);

    /**
     * 标识顺序，从1开始，同一个app如果需要多次分片转换，要求按顺序递增order
     *
     * @return
     */
    int order();
}
