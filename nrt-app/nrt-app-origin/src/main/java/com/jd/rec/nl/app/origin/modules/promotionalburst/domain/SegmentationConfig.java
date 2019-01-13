package com.jd.rec.nl.app.origin.modules.promotionalburst.domain;

import com.typesafe.config.Optional;

import java.util.*;

/**
 * @author linmx
 * @date 2018/7/16
 */
public class SegmentationConfig {

    /**
     * 输出的top N
     */
    protected int topSize;

    /**
     * 用户端维度
     */
    @Optional
    private List<String> userDimension;

    /**
     * sku端维度
     */
    @Optional
    private List<String> itemDimension;

    /**
     * key的后缀,主要用于实验版本的区分
     */
    @Optional
    private String keyPostfix;

    public String getKeyPostfix() {
        return keyPostfix;
    }

    public void setKeyPostfix(String keyPostfix) {
        this.keyPostfix = keyPostfix;
    }

    public int getTopSize() {
        return topSize;
    }

    public void setTopSize(int topSize) {
        this.topSize = topSize;
    }

    public List<String> getUserDimension() {
        return userDimension;
    }

    public void setUserDimension(List<String> userDimension) {
        this.userDimension = userDimension;
    }

    public List<String> getItemDimension() {
        return itemDimension;
    }

    public void setItemDimension(List<String> itemDimension) {
        this.itemDimension = itemDimension;
    }

    /**
     * 生成用户维度列表
     *
     * @param eventFeature
     * @return
     */
    public Set<DimensionValue> getUserDimensions(EventFeature eventFeature) {
        return getDimensionValues(eventFeature.getUserFeature(), this.userDimension);
    }

    /**
     * 根据具体的用户维度和sku特征列表生成segmentation列表
     *
     * @param userDimensionValue
     * @param itemFeatures
     * @return
     */
    public Set<SegmentationInfo> getSegmentation(DimensionValue userDimensionValue, Map<String, Object> itemFeatures) {
        Set<DimensionValue> itemDimensionsValue = getDimensionValues(itemFeatures, this.itemDimension);
        Set<SegmentationInfo> segmentationInfos = new HashSet<>();
        itemDimensionsValue.forEach(itemDimensionValue ->
                segmentationInfos.add(new SegmentationInfo(this.topSize, userDimension, userDimensionValue, itemDimension,
                        itemDimensionValue, this.keyPostfix)));
        return segmentationInfos;
    }

    /**
     * 根据维度定义和特征数据,生成符合条件的维度列表
     * 当某个特征数据为集合时,进行笛卡尔积处理
     *
     * @param features
     * @param dimensions
     * @return
     */
    private Set<DimensionValue> getDimensionValues(Map<String, Object> features, List<String> dimensions) {
        Set<DimensionValue> dimensionsValue = new HashSet<>();
        if (dimensions != null && dimensions.size() > 0) {
            if (features == null || features.size() == 0) {
                return dimensionsValue;
            }
            Map<String, Object> multiFeatures = new HashMap<>();

            DimensionValue dimensionValue = new DimensionValue(this.keyPostfix);
            for (String featureName : dimensions) {
                Object featureValue = features.get(featureName);
                if (featureValue == null) {
                    // 当前sku没有这个segmentation要求的属性,直接返回空list
                    return dimensionsValue;
                }
                if (featureValue instanceof Collection) {
                    multiFeatures.put(featureName, featureValue);
                } else {
                    dimensionValue.addFeature(featureName, featureValue);
                }
            }

            if (dimensionValue.getFeatures().size() > 0 || multiFeatures.size() > 0)
                dimensionsValue.add(dimensionValue);

            multiFeatures.forEach((featureName, featureValueCollect) -> {
                List<DimensionValue> temp = new ArrayList<>();
                ((Collection) featureValueCollect).stream().forEach(featureValue -> {
                    dimensionsValue.forEach(rawDimensionValue -> {
                        DimensionValue newValue = rawDimensionValue.clone();
                        newValue.addFeature(featureName, featureValue);
                        temp.add(newValue);
                    });
                });
                dimensionsValue.clear();
                dimensionsValue.addAll(temp);
            });
        } else {
            DimensionValue dimensionValue = new DimensionValue(this.keyPostfix);
            dimensionValue.addFeature("global", "global");
            dimensionsValue.add(dimensionValue);
        }
        return dimensionsValue;
    }

}
