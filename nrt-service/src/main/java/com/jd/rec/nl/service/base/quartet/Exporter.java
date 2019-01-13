package com.jd.rec.nl.service.base.quartet;

import com.jd.rec.nl.service.base.quartet.domain.OutputResult;

/**
 * @author linmx
 * @date 2018/6/1
 */
public interface Exporter extends Operator {

    /**
     * 输出操作
     *
     * @param result
     */
    void export(OutputResult result);

}
