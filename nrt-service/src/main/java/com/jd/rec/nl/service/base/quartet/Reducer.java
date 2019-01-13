package com.jd.rec.nl.service.base.quartet;

import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;

import java.io.Serializable;
import java.time.Duration;

/**
 * @author linmx
 * @date 2018/7/24
 */
public interface Reducer<K extends Serializable, V extends Serializable> extends Operator {

    /**
     * 收集不同分片的记录数据
     *
     * @param key
     * @param value
     */
    void collect(K key, V value);

    /**
     * 进行reduce操作
     *
     * @param resultCollection 保存更新结果的数据结构,调用
     *                         {@link ResultCollection#addOutput(String, String, int, String, byte[], Duration)} 增加处理结果
     */
    void reduce(ResultCollection resultCollection);

}
