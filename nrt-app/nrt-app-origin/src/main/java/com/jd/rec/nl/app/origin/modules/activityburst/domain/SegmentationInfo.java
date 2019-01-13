package com.jd.rec.nl.app.origin.modules.activityburst.domain;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * @author linmx
 * @date 2018/7/24
 */
public class SegmentationInfo implements Serializable {

    int topSize;

    Map<String, Object> userDimensionValue = new LinkedHashMap<>();

    Map<String, Object> itemDimensionValue = new LinkedHashMap<>();

    public SegmentationInfo(int topSize, List<String> userDimensionDefine, DimensionValue userDimensionValue,
                            List<String> itemDimensionDefine, DimensionValue itemDimensionValue) {
        this.topSize = topSize;
        if (userDimensionDefine == null) {
            this.userDimensionValue.putAll(userDimensionValue.getFeatures());
        } else {
            userDimensionDefine.forEach(key -> this.userDimensionValue.put(key, userDimensionValue.getFeatures().get(key)));
        }
        if (itemDimensionDefine == null) {
            this.itemDimensionValue.putAll(itemDimensionValue.getFeatures());
        } else {
            itemDimensionDefine.forEach(key -> this.itemDimensionValue.put(key, itemDimensionValue.getFeatures().get(key)));
        }
    }

    public int getTopSize() {
        return topSize;
    }

    public void setTopSize(int topSize) {
        this.topSize = topSize;
    }

    public Map<String, Object> getUserDimensionValue() {
        return userDimensionValue;
    }

    public Map<String, Object> getItemDimensionValue() {
        return itemDimensionValue;
    }

    @Override
    public int hashCode() {
        int hashCode = topSize;
        hashCode = 31 * hashCode + userDimensionValue.hashCode();
        hashCode = 31 * hashCode + itemDimensionValue.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != SegmentationInfo.class) {
            return false;
        }
        SegmentationInfo info = (SegmentationInfo) obj;
        return userDimensionValue.equals(info.getUserDimensionValue()) && itemDimensionValue.equals(info.getItemDimensionValue())
                && topSize == info.getTopSize();
    }

    @Override
    public String toString() {
        StringBuilder keys = new StringBuilder();
        StringBuilder values = new StringBuilder();
        userDimensionValue.forEach((key, value) -> {
            if (!key.equals("global")) {
                keys.append(key).append(DimensionValue.SEG_SEP);
                values.append(value).append(DimensionValue.SEG_SEP);
            }
        });
        itemDimensionValue.forEach((key, value) -> {
            if (!key.equals("global")) {
                keys.append(key).append(DimensionValue.SEG_SEP);
                values.append(value).append(DimensionValue.SEG_SEP);
            }
        });
        if (keys.length() == 0) {
            return "global";
        } else {
            return keys.substring(0, keys.length() - 1).concat(String.valueOf(DimensionValue.VALUE_SEP))
                    .concat(values.substring(0, values.length() - 1));
        }
    }
}
