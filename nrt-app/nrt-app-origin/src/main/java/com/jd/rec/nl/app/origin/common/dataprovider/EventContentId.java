package com.jd.rec.nl.app.origin.common.dataprovider;

import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.operator.RequiredMiscInfoKey;

import java.util.HashSet;
import java.util.Set;

/**
 * @author zec & wl
 * @date 2018/8/23
 */
public class EventContentId implements RequiredMiscInfoKey {

    @Override
    public Set<String> getKeys(MapperContext mapperContext) {
        Set<String> keys = new HashSet<>();
        Set<String> contentIds = mapperContext.getEventContent().getContentIds();
        if (contentIds != null && !contentIds.isEmpty()) {
            keys.addAll(contentIds);
        }
        return keys;
    }
}
