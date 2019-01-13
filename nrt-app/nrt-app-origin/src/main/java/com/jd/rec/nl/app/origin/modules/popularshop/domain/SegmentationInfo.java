package com.jd.rec.nl.app.origin.modules.popularshop.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wl
 * @date 2018/9/20
 */
public class SegmentationInfo implements Serializable,Cloneable {

    int topSize;

    Map<String, Object> userDimensionValue = new LinkedHashMap<>();

    Map<String, Object> shopDimensionValue = new LinkedHashMap<>();
    /**
     * 版本号
     */
    String version;

    public SegmentationInfo(int topSize, List<String> userDimensionDefine, DimensionValue userDimensionValue,
                            List<String> shopDimensionDefine, DimensionValue shopDimensionValue) {
        this.topSize = topSize;
        if (userDimensionDefine == null) {
            this.userDimensionValue.putAll(userDimensionValue.getFeatures());
        } else {
            userDimensionDefine.forEach(key -> this.userDimensionValue.put(key, userDimensionValue.getFeatures().get(key)));
        }
        if (shopDimensionDefine == null) {
            this.shopDimensionValue.putAll(shopDimensionValue.getFeatures());
        } else {
            shopDimensionDefine.forEach(key -> this.shopDimensionValue.put(key, shopDimensionValue.getFeatures().get(key)));
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

    public Map<String, Object> getShopDimensionValue() {
        return shopDimensionValue;
    }

    @Override
    public int hashCode() {
        int hashCode = topSize;
        hashCode = 31 * hashCode + userDimensionValue.hashCode();
        hashCode = 31 * hashCode + shopDimensionValue.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != SegmentationInfo.class) {
            return false;
        }
        SegmentationInfo info = (SegmentationInfo) obj;
        return userDimensionValue.equals(info.getUserDimensionValue()) && shopDimensionValue.equals(info.getShopDimensionValue())
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
        shopDimensionValue.forEach((key, value) -> {
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

    private SegmentationInfo(int topSize, Map<String, Object> userDimensionValue, Map<String, Object> shopDimensionValue) {
        this.topSize = topSize;
        this.userDimensionValue = userDimensionValue;
        this.shopDimensionValue = shopDimensionValue;
    }

    @Override
    public SegmentationInfo clone() {
        SegmentationInfo newSegmentationInfo = new SegmentationInfo(this.topSize,this.userDimensionValue,this.shopDimensionValue);
        return newSegmentationInfo;
    }
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
