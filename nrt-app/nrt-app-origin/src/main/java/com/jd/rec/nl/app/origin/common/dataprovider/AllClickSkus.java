package com.jd.rec.nl.app.origin.common.dataprovider;

import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.operator.RequiredMiscInfoKey;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author linmx
 * @date 2018/9/7
 */
public class AllClickSkus implements RequiredMiscInfoKey {
    @Override
    public Set<String> getKeys(MapperContext mapperContext) {
        Set<String> skus = mapperContext.getEventContent().getSkus().stream().map(sku -> String.valueOf(sku)).collect
                (Collectors.toSet());
        if (mapperContext.getBehaviors() != null) {
            mapperContext.getBehaviors().entrySet().stream()
                    .filter(behaviorFieldSetEntry -> behaviorFieldSetEntry.getKey() == BehaviorField.CLICK)
                    .flatMap(behaviorFieldSetEntry -> behaviorFieldSetEntry
                            .getValue().stream()).map(behaviorInfo -> String.valueOf(behaviorInfo.getSku()))
                    .forEach(sku -> skus.add(sku));
        }
        return skus;
    }
}
