package com.jd.rec.nl.app.origin.common.dataprovider;

import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.operator.RequiredMiscInfoKey;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author wl
 * @date 2018/9/20
 */
public class ShopUVId implements RequiredMiscInfoKey {
    @Override
    public Set<String> getKeys(MapperContext mapperContext) {
        Set<String> keys = new HashSet<>();
        if (mapperContext.getEventContent().getBehaviorField() == BehaviorField.CLICK) {
            Set<Long> importSku = mapperContext.getEventContent().getSkus();
            Map<Long, ItemProfile> itemProfileMap = mapperContext.getSkuProfiles();
            if (importSku != null && !importSku.isEmpty()) {
                if (itemProfileMap != null && !itemProfileMap.isEmpty()) {
                    for (long sku : importSku) {
                        ItemProfile itemProfile = itemProfileMap.get(sku);
                        if (itemProfile == null) {
                            continue;
                        }
                        Long shopId = itemProfile.getShopId();
                        if (shopId != null && shopId != 0) {
                            keys.add(String.valueOf(shopId));
                        }
                    }
                }
            }
        } else {
            Set<Long> importShopIds = mapperContext.getEventContent().getShopIds();
            if (importShopIds != null && !importShopIds.isEmpty()) {
                importShopIds.stream().forEach(shopId -> keys.add(String.valueOf(shopId)));
            }
        }
        return keys;
    }
}
