package com.jd.rec.nl.service.common.quartet.operator.impl;

import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.service.base.quartet.OperatorFilter;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;

/**
 * @author linmx
 * @date 2018/9/30
 */
public interface UidTestFilter extends OperatorFilter<MapperContext> {

    @Override
    default boolean test(MapperContext mapperContext) {
        String uid = mapperContext.getUid();
        if (ConfigBase.getSystemConfig().hasPath("trace.uid") && ConfigBase.getSystemConfig().getStringList("trace.uid")
                .contains(uid)) {
            return true;
        } else {
            return false;
        }
    }
}
