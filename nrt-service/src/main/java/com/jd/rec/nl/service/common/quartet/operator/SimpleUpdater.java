package com.jd.rec.nl.service.common.quartet.operator;

import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;

import java.time.Duration;

/**
 * @author linmx
 * @date 2018/7/31
 */
public interface SimpleUpdater<V> extends Updater {

    /**
     * 获取key
     *
     * @param context
     * @return
     */
    String getKey(MapperContext context, V ret);

    /**
     * 类型
     *
     * @return
     */
    String getType();

    /**
     * model id
     *
     * @return
     */
    int getSubType();

    /**
     * 简单map操作
     *
     * @param input
     * @return
     */
    V update(MapperContext input) throws Exception;

    /**
     * 对结果序列化
     *
     * @param result
     * @return
     */
    byte[] serialize(MapperContext input, V result);

    /**
     * 超时时间,默认为0(默认超时时间)
     *
     * @return
     */
    default Duration expireTime() {
        return null;
    }

    @Override
    default void update(MapperContext input, ResultCollection resultCollection) {
        try {
            V result = update(input);
            if (result == null) {
                return;
            }
            resultCollection.addOutput(this.getName(), this.getType(), this.getSubType(), this.getKey(input, result),
                    this.serialize(input, result), expireTime());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
