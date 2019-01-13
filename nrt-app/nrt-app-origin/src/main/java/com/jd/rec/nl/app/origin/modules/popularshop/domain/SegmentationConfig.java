package com.jd.rec.nl.app.origin.modules.popularshop.domain;

import com.typesafe.config.Optional;

import java.util.*;

/**
 * @author wl
 * @date 2018/9/20
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
     * 店铺端维度
     */
    @Optional
    private List<String> shopDimension;

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

    public List<String> getShopDimension() {
        return shopDimension;
    }

    public void setShopDimension(List<String> shopDimension) {
        this.shopDimension = shopDimension;
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
     * @param shopFeatures
     * @return
     */
    public Set<SegmentationInfo> getSegmentation(DimensionValue userDimensionValue, Map<String, Object> shopFeatures) {
        Set<DimensionValue> shopDimensionsValue = getDimensionValues(shopFeatures, this.shopDimension);
        Set<SegmentationInfo> segmentationInfos = new HashSet<>();
        shopDimensionsValue.forEach(shopDimensionValue ->
                segmentationInfos.add(new SegmentationInfo(this.topSize, userDimension, userDimensionValue, shopDimension,
                        shopDimensionValue)));
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

            DimensionValue dimensionValue = new DimensionValue();
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
            DimensionValue dimensionValue = new DimensionValue();
            dimensionValue.addFeature("global", "global");
            dimensionsValue.add(dimensionValue);
        }
        return dimensionsValue;
    }

}
