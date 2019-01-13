package com.jd.rec.nl.app.origin.common.dataprovider;

import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.operator.RequiredMiscInfoKey;

import java.util.HashSet;
import java.util.Set;

/**
 * @author zec & wl
 * @date 2018/8/23
 */
public class EventSku implements RequiredMiscInfoKey {

    @Override
    public Set<String> getKeys(MapperContext mapperContext) {
        Set<String> keys = new HashSet<>();
        Set<Long> importSku = mapperContext.getEventContent().getSkus();
        if (importSku != null && !importSku.isEmpty()) {
            importSku.stream().forEach(sku -> keys.add(String.valueOf(sku)));
            return keys;
        } else {
            return null;
        }
    }
}
