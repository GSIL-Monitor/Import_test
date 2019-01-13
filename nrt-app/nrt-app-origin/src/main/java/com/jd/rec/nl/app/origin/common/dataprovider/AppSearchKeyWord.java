package com.jd.rec.nl.app.origin.common.dataprovider;

import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.operator.RequiredMiscInfoKey;

import java.util.HashSet;
import java.util.Set;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/11/19
 */
public class AppSearchKeyWord implements RequiredMiscInfoKey {
    @Override
    public Set<String> getKeys(MapperContext mapperContext) {
        Set<String> result=new HashSet<>();
        String key= mapperContext.getEventContent().getKeyWord();
        if (key != null ) {
            result.add("pw-".concat(key));
        }
        return result;
    }
}
