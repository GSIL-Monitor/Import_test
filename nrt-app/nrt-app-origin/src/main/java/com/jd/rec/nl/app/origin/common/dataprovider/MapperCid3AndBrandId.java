package com.jd.rec.nl.app.origin.common.dataprovider;


import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.operator.RequiredMiscInfoKey;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author zec & wl
 * @date 2018/8/23
 */
public class MapperCid3AndBrandId implements RequiredMiscInfoKey {

    @Override
    public Set<String> getKeys(MapperContext mapperContext) {
        Set<String> keys = new HashSet<>();
        if (mapperContext.getSkuProfiles() != null) {
            Map<Long, ItemProfile> profiles = mapperContext.getSkuProfiles();
            for (ItemProfile itemProfile : profiles.values()) {
                int cid3 = itemProfile.getCid3();
                int brandId = itemProfile.getBrandId();
                keys.add(cid3 + ":" + brandId);
            }
        }
        return keys;
    }
}
