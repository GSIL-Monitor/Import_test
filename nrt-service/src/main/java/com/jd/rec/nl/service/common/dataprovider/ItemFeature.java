package com.jd.rec.nl.service.common.dataprovider;

import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.domain.MapperFieldReflector;
import com.typesafe.config.Optional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author linmx
 * @date 2018/8/2
 */
public class ItemFeature {

    String fieldName;

    @Optional
    boolean predictFlag = false;

    /**
     * 别名
     */
    private String name;

    public boolean isPredictFlag() {
        return predictFlag;
    }

    public void setPredictFlag(boolean predictFlag) {
        this.predictFlag = predictFlag;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public List getValue(MapperContext context, long sku) {
        List ret = new ArrayList<>();
        if (isPredictFlag() && context.getMisc() != null && context.getMisc().containsKey(fieldName)
                && context.getMisc().get(fieldName).containsKey(String.valueOf(sku))) {
            ret.addAll(context.getMisc().get(fieldName).get(String.valueOf(sku)).keySet());
        } else {
            String fieldReg = "item:".concat(this.getFieldName());
            try {
                MapperFieldReflector fieldReflector = MapperFieldReflector.create(fieldReg);
                ret.add(fieldReflector.get(context, sku));
            } catch (Exception e) {
                throw new RuntimeException(String.format("get item profile error, sku=%d, fieldReg=%s, sku profile :%s",
                        sku, fieldReg, context.getSkuProfiles() != null ? context.getSkuProfiles().get(sku) : null), e);
            }
        }
        return ret;
    }
}
