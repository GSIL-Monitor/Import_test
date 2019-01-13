package com.jd.rec.nl.app.origin.modules.activityburst.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/8/7
 */
public class DimensionValue implements Cloneable {

    public static final char SEG_SEP = '_';

    public static final char VALUE_SEP = ':';

    Map<String, Object> features = new HashMap<>();

    public DimensionValue() {
    }


    public Map<String, Object> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, Object> features) {
        this.features = features;
    }

    public void addFeature(String featureName, Object featureValue) {
        features.put(featureName, featureValue);
    }

    @Override
    public int hashCode() {
        return features.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DimensionValue)) {
            return false;
        }
        return features.equals(((DimensionValue) obj).getFeatures());
    }

    @Override
    protected DimensionValue clone() {
        Map<String, Object> newFeatures = new HashMap<>(features);
        DimensionValue newDimensionValue = new DimensionValue();
        newDimensionValue.setFeatures(newFeatures);
        return newDimensionValue;
    }

    /**
     * 获取对应的爆品特征的key值,只有用户维度的特征值才能生成爆品特征值
     *
     * @return
     */
    public String getFeatureKey() {
        //        BurstFeature.BurstFeatureKey.Builder builder = BurstFeature.BurstFeatureKey.newBuilder();
        //        features.forEach((featureName, featureValue) -> {
        //            Descriptors.FieldDescriptor descriptor = BurstFeature.BurstFeatureKey.getDescriptor().findFieldByName
        //                    (featureName);
        //            if (descriptor != null)
        //                builder.setField(descriptor, featureValue);
        //        });
        //        return new String(builder.build().toByteArray());
        StringBuilder keys = new StringBuilder();
        StringBuilder values = new StringBuilder();
        features.forEach((key, value) -> {
            if (!key.equals("global")) {
                keys.append(SEG_SEP).append(key.substring(0, 1));
                values.append(SEG_SEP).append(value);
            }
        });
        String key = keys.append(values).toString();
        if (key.length() == 0) {
            key = "_global";
        }
        return key;
    }

    @Override
    public String toString() {
        StringBuilder keys = new StringBuilder();
        StringBuilder values = new StringBuilder();
        features.forEach((key, value) -> {
            keys.append(key).append(SEG_SEP);
            values.append(value).append(SEG_SEP);
        });
        return keys.substring(0, keys.length() - 1).concat(String.valueOf(VALUE_SEP))
                .concat(values.substring(0, values.length() - 1));
    }
}
