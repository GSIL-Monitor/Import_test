package com.jd.rec.nl.service.base.quartet;

import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;

import java.time.Duration;

/**
 * @author linmx
 * @date 2018/9/21
 */
public interface Schedule extends Operator {

    /**
     * 默认的时间间隔,单位为秒,5分钟
     */
    Duration defaultInterval = ConfigBase.getSystemConfig().getDuration("quartet.intervalUnit");

    /**
     * 间隔大小,默认时间间隔的倍数
     * 以简化触发条件
     *
     * @return 默认时间间隔的倍数
     */
    int intervalSize();


    /**
     * 触发操作
     *
     * @param resultCollection
     */
    void trigger(ResultCollection resultCollection);
}
