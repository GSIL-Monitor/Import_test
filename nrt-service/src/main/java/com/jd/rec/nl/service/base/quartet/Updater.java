package com.jd.rec.nl.service.base.quartet;

import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;

import java.io.Serializable;
import java.time.Duration;

/**
 * @author linmx
 * @date 2018/6/1
 */
public interface Updater extends Operator {

    /**
     * 更新操作,支持输出多个处理结果
     *
     * @param mapperContext    Mapper数据查询的结果
     * @param resultCollection 保存更新结果的数据结构
     *                         对于实时处理的输出,调用
     *                         {@link ResultCollection#addOutput(String, String, int, String, byte[], Duration)} 输出处理结果<br/>
     *                         对于窗口处理,调用 {@link ResultCollection#addMapResult(String, Serializable, Serializable)} 输出结果
     * @throws Exception
     */
    void update(MapperContext mapperContext, ResultCollection resultCollection) throws Exception;

}
