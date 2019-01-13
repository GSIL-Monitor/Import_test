package com.jd.rec.nl.app.origin.modules.popularshop;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.popularshop.domain.*;
import com.jd.rec.nl.service.base.quartet.WindowCollector;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.trace.RuntimeTrace;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recsys.prediction_service.UnifiedItemBurstFeatureOuterClass;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

/**
 * 累加当前窗口的shop uv,并在窗口结束时计算shop爆品得分以及收集每个segmentation的topN shop召回
 * 具体做法为:
 * 1.在窗口累计期,按用户维度的组合feature累加各shop的UV,并记录对应用户feature类型的segmentationConfig以及shop对应的shop维度feature值
 * 2.在窗口结束时,按用户维度计算每个shop的爆品得分,并根据记录的shop feature值和对应segmentationConfig组合用户维度值生成完整的segmentation,
 * 获取每个segmentation下的爆品得分取top20
 *
 * @author wl
 * @date 2018/9/19
 */
public class AccumulateShopUVAndScore implements WindowCollector<Long, EventFeature> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccumulateShopUVAndScore.class);

    private static final int featurePartitionNum = 1000;

    String name = "popularshop";

    @Inject
    @Named("alpha")
    private double alpha;

    @Inject
    @Named("punishment")
    private double punishment;

    @Inject
    @Named("featureServiceType")
    private String featureServiceType;


    @Inject
    @Named("featureSubType")
    private int featureSubType;

    @Inject
    @Named("HC_Tll")
    private Duration hcTimeToLive;

    @Inject
    private ShopBurstCoefficientService shopBurstCoefficientService;
    @Inject(optional = true)
    @Named("windowSize")
    private Duration windowSize = Duration.ofMinutes(5);

    @Inject
    private RuntimeTrace runtimeTrace;

    /**
     * 当前窗口用户维度的shop uv量统计
     */
    private Map<DimensionValue, HashMap<Long, ShopBurstInfo>> userDimensionBurstCache = new HashMap<>();

    /**
     * 当前窗口下用户维度对应的所属segmentation
     */
    private Map<DimensionValue, Set<SegmentationConfig>> userDimensionToSeg = new HashMap<>();

    private Map<Long, Map<String, Object>> shopFeatures = new HashMap<>();

    private List<SegmentationConfig> segmentationConfigs;

    /**
     * 做一个判定是否已经到1h的标识。
     */
    private int count = 0;

    public AccumulateShopUVAndScore() {
    }

    @Inject
    public AccumulateShopUVAndScore(@Named("segmentation") List<SegmentationConfig> segmentationConfigs) {
        this.segmentationConfigs = segmentationConfigs;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }


    @Override
    public int windowSize() {
        return Long.valueOf(windowSize.getSeconds() / defaultInterval.getSeconds()).intValue();
    }

    /**
     * 处理原始数据为需要的key-value  pair进行暂存
     * <p>
     * 这里是以shopId做为key，时间特征做为value(uid,shopId,用户特征)
     *
     * @param shopId
     * @param eventFeature
     */
    @Override
    public void collect(Long shopId, EventFeature eventFeature) {
        if (shopFeatures.isEmpty()) {
            LOGGER.debug("the first message in this window,uv cache:{}, segmentation cache:{}", userDimensionBurstCache.size(),
                    userDimensionToSeg.size());
        }
        Set<DimensionValue> userDimensionValues = new HashSet<>();
        segmentationConfigs.stream().forEach(segmentationConfig -> {
            // 用户维度具体值
            Set<DimensionValue> dimensionValues = segmentationConfig.getUserDimensions(eventFeature);

            dimensionValues.forEach(userDimensionValue -> {
                if (userDimensionValue.getFeatures() == null || userDimensionValue.getFeatures().size() == 0) {
                    LOGGER.error("config:{} lead a null !! user features: {}", segmentationConfig.getUserDimension(),
                            eventFeature.getUserFeature());
                    return;
                }
                userDimensionValues.add(userDimensionValue);
                // 将用户维度的具体值关联到segmentationConfig
                if (this.userDimensionToSeg.containsKey(userDimensionValue)) {
                    this.userDimensionToSeg.get(userDimensionValue).add(segmentationConfig);
                } else {
                    Set<SegmentationConfig> configs = new HashSet<>();
                    configs.add(segmentationConfig);
                    this.userDimensionToSeg.put(userDimensionValue, configs);
                }
            });
        });

        userDimensionValues.forEach(userDimensionValue -> {
            //保存及累加店铺pv数据
            HashMap<Long, ShopBurstInfo> shopBurstInfo;
            if (userDimensionBurstCache.containsKey(userDimensionValue)) {
                shopBurstInfo = userDimensionBurstCache.get(userDimensionValue);
            } else {
                shopBurstInfo = new HashMap<>();
                userDimensionBurstCache.put(userDimensionValue, shopBurstInfo);
            }

            if (shopBurstInfo.containsKey(shopId)) {
                ShopBurstInfo burstInfo = shopBurstInfo.get(shopId);
                accumulateShopId(eventFeature, burstInfo);
            } else {
                HistoryCUV lastHistory = shopBurstCoefficientService.getHistoryCUV(userDimensionValue, shopId, System.currentTimeMillis());
                String featureKeyPost = userDimensionValue.getFeatureKey();
                String featureKey = String.valueOf(shopId).concat(featureKeyPost);
                ShopBurstInfo burstInfo = new ShopBurstInfo(featureKey, punishment, windowSize, alpha, lastHistory);
                accumulateShopId(eventFeature, burstInfo);
                burstInfo.setHistoryCUV(lastHistory);
                shopBurstInfo.put(shopId, burstInfo);
            }
        });

        // 记录shop维度的属性
        if (!shopFeatures.containsKey(shopId)) {
            shopFeatures.put(shopId, eventFeature.getShopFeature());
        }
    }


    /**
     * 分派，当一个时间窗口结束后将存储的数据按key分派
     *
     * @param resultCollection
     */
    @Override
    public void shuffle(ResultCollection resultCollection) {
        count++;
        try {
            if (userDimensionBurstCache.size() == 0) {
                return;
            } else {
                LOGGER.debug("user dimension size:{}, \n {}", userDimensionBurstCache.size(), userDimensionBurstCache.keySet());
                userDimensionBurstCache.entrySet().parallelStream().forEach(dimensionEntry -> {
                    DimensionValue userDimensionValue = dimensionEntry.getKey();
                    HashMap<Long, ShopBurstInfo> shopBurstInfo = dimensionEntry.getValue();

                    LOGGER.debug("computer the user dimension:{}, shop size:{}", userDimensionValue.getFeatures(),
                            shopBurstInfo.size());

                    // 当前用户维度下的segmentation定义
                    Set<SegmentationConfig> recallSegmentation = userDimensionToSeg.get(userDimensionValue);

                    // 当前用户维度下各segmentation的topN召回商详页1小时
                    Map<SegmentationInfo, Queue<ShopWithScore>> recallTopNSku1h = new HashMap<>();
                    //商详页5分钟
                    Map<SegmentationInfo, Queue<ShopWithScore>> recallTopNSku5min = new HashMap<>();

                    shopBurstInfo.forEach((shopId, burstInfo) -> {
                        //商详页5分钟
                        double scoreSku5min = burstInfo.getScore();
                        LOGGER.debug("shopId:" + shopId + ",preHCSku5min:" + burstInfo.getPreHcSku5min() + ",SkuUV5minHc:" + burstInfo.getShopSkuUV5minHc() + ",score:" + scoreSku5min);

                        //店铺
                        Integer shopUV1h = burstInfo.getShopUV1h().stream().mapToInt(Integer::intValue).sum();
                        //商详页
                        Integer shopSkuUV1h = burstInfo.getShopSkuUV1h().stream().mapToInt(Integer::intValue).sum();

                        /**
                         *  保存新的hc 同时存储到dbService和内存中去,一定保证这个history和数据库中查询出来的是一样的。
                         *  5分钟到了只是更新5分钟的。1h到了，更新1h的。不能混为一谈
                         */
                        burstInfo.getHistoryCUV().setTimestamp5min(System.currentTimeMillis() - 5 * 60 * 1000);
                        burstInfo.getHistoryCUV().setShopUV5minCoefficient(burstInfo.getShopUV5minHc());
                        burstInfo.getHistoryCUV().setShopSkuUV5minCoefficient(burstInfo.getShopSkuUV5minHc());
                        if (count == 12) {
                            burstInfo.getHistoryCUV().setTimestamp1h(System.currentTimeMillis() - 60 * 60 * 1000);
                            burstInfo.getHistoryCUV().setShopSkuUV1hCoefficient(burstInfo.getShopSkuUV1hHc());
                            burstInfo.getHistoryCUV().setShopUV1hCoefficient(burstInfo.getShopUV1hHc());
                        }
                        shopBurstCoefficientService.saveNewHistoryCUV(userDimensionValue, shopId, burstInfo.getHistoryCUV(), hcTimeToLive);

                        if (burstInfo.getShopUV5min() + burstInfo.getShopSkuUV5min() > 0) {

                            UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.Builder builder =
                                    UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.newBuilder();
                            builder.setItemId(shopId);
                            builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("shopUV1h")
                                    .setValue(shopUV1h.doubleValue()))
                                    .addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper
                                            ("shopSkuUV1h").setValue(shopSkuUV1h.doubleValue()))
                                    .addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper
                                            ("shopSkuUV5min").setValue(burstInfo.getShopSkuUV5min()))
                                    .addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("shopUV5min")
                                            .setValue(burstInfo.getShopUV5min()))
                                    .addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("time1h")
                                            .setValue(burstInfo.getHistoryCUV().getTimestamp1h() / 1000))
                                    .addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("time5min")
                                            .setValue(burstInfo.getHistoryCUV().getTimestamp5min() / 1000));
                            // 增加窗口期的hc作为feature输出
                            builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("shopSkuUV1hHc").setValue
                                    (BigDecimal.valueOf(burstInfo.getPreHcSku1h()).multiply(BigDecimal.valueOf(10000))
                                            .setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue()));
                            builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("shopSkuUV5minHc").setValue
                                    (BigDecimal.valueOf(burstInfo.getPreHcSku5min()).multiply(BigDecimal.valueOf(10000))
                                            .setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue()));
                            builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("shopUV1hHc").setValue
                                    (BigDecimal.valueOf(burstInfo.getPreHcUV1h()).multiply(BigDecimal.valueOf(10000))
                                            .setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue()));
                            builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("shopUV5minHc").setValue
                                    (BigDecimal.valueOf(burstInfo.getPreHcUV5min()).multiply(BigDecimal.valueOf(10000))
                                            .setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue()));
                            resultCollection.addOutput(this.getName(), featureServiceType, featureSubType,
                                    burstInfo.getFeatureKey(), builder.build().toByteArray(), Duration.ofHours(1));

                            // 日志记录
                            runtimeTrace.info("shopBurstFeature", new FeatureLog(burstInfo, builder));
                        }
                        //商详页的1小时uv进行召回
                        ShopWithScore skuScore1h = new ShopWithScore(shopId, shopSkuUV1h.doubleValue());

                        //商详页的5分钟score进行召回
                        ShopWithScore skuScore5min = new ShopWithScore(shopId, scoreSku5min);

                        // 将shopId增加到对应segmentation的topN中进行分数比对
                        Map<String, Object> feature = this.shopFeatures.get(shopId);
                        if (feature != null && feature.size() > 0) {
                            recallSegmentation.stream().forEach(segmentationConfig -> {
                                Set<SegmentationInfo> segmentationsInfo = segmentationConfig.getSegmentation
                                        (userDimensionValue, feature);
                                segmentationsInfo.forEach(segmentationInfo -> {
                                    SegmentationInfo segmentationInfo1 = segmentationInfo.clone();
                                    segmentationInfo1.setVersion("v1");
                                    Queue<ShopWithScore> topNSku1h;
                                    if (recallTopNSku1h.containsKey(segmentationInfo1)) {
                                        topNSku1h = recallTopNSku1h.get(segmentationInfo1);
                                    } else {
                                        topNSku1h = new PriorityQueue<>(new TopNComparator());
                                        recallTopNSku1h.put(segmentationInfo1, topNSku1h);
                                    }
                                    if (topNSku1h.size() < segmentationConfig.getTopSize()) {
                                        topNSku1h.add(skuScore1h);
                                    } else {
                                        if (topNSku1h.peek().getScore() < skuScore1h.getScore()) {
                                            topNSku1h.poll();
                                            topNSku1h.add(skuScore1h);
                                        }
                                    }
                                    SegmentationInfo segmentationInfo2 = segmentationInfo.clone();
                                    segmentationInfo2.setVersion("v2");
                                    Queue<ShopWithScore> topNSku5min;
                                    if (recallTopNSku5min.containsKey(segmentationInfo2)) {
                                        topNSku5min = recallTopNSku5min.get(segmentationInfo2);
                                    } else {
                                        topNSku5min = new PriorityQueue<>(new TopNComparator());
                                        recallTopNSku5min.put(segmentationInfo2, topNSku5min);
                                    }
                                    if (topNSku5min.size() < segmentationConfig.getTopSize()) {
                                        topNSku5min.add(skuScore5min);
                                    } else {
                                        if (topNSku5min.peek().getScore() < skuScore5min.getScore()) {
                                            topNSku5min.poll();
                                            topNSku5min.add(skuScore5min);
                                        }
                                    }

                                });
                            });
                        }
                    });
                    // 生成 pb recall  商详页1小时的
                    recallTopNSku1h.forEach(((segmentationInfo, shopWithScores) ->
                            resultCollection.addMapResult(this.getName(), segmentationInfo,
                                    new ArrayList<>(shopWithScores))
                    ));
                    //商详页5分钟的
                    recallTopNSku5min.forEach(((segmentationInfo, shopWithScores) ->
                            resultCollection.addMapResult(this.getName(), segmentationInfo,
                                    new ArrayList<>(shopWithScores))
                    ));
                });
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            // 清空缓存
            LOGGER.debug("finish shuffle,clear cache.");
            try {
                for (DimensionValue dimensionValue : new HashSet<>(this.userDimensionBurstCache.keySet())) {
                    HashMap<Long, ShopBurstInfo> shopBurstInfoHashMap = this.userDimensionBurstCache.get(dimensionValue);
                    for (Long shopId : new HashSet<>(shopBurstInfoHashMap.keySet())) {
                        ShopBurstInfo shopBurstInfo = shopBurstInfoHashMap.get(shopId);
                        shopBurstInfo.setShopUV5min(0);
                        shopBurstInfo.setShopSkuUV5min(0);
                        //将1小时 过时的删除
                        if (shopBurstInfo.getShopSkuUV1h().size() == 12) {
                            shopBurstInfo.getShopSkuUV1h().remove(0);
                        }
                        if (shopBurstInfo.getShopUV1h().size() == 12) {
                            shopBurstInfo.getShopUV1h().remove(0);
                        }
                        //给最后一个赋0
                        shopBurstInfo.getShopSkuUV1h().add(0);
                        shopBurstInfo.getShopUV1h().add(0);

                        //如果这个1小时的为0，那么就删除这个key为shopId的数据
                        List<Integer> listAll = new ArrayList<>();
                        listAll.addAll(shopBurstInfo.getShopSkuUV1h());
                        listAll.addAll(shopBurstInfo.getShopUV1h());
                        int num = 0;
                        for (int i = 0; i < listAll.size(); i++) {
                            num = num + listAll.get(i);
                            if (num > 0) {
                                break;
                            }
                        }
                        if (num == 0) {
                            shopBurstInfoHashMap.remove(shopId);
                            shopFeatures.remove(shopId);
                        } else {
                            if (count == 12) {
                                //代表到1h时候，给1h的做一个衰减
                                shopBurstInfo.setPreHcSku1h(shopBurstInfo.getShopSkuUV1hHc());
                                shopBurstInfo.setShopSkuUV1hHc(shopBurstInfo.getShopSkuUV1hHc() * alpha);
                                shopBurstInfo.setPreHcUV1h(shopBurstInfo.getShopUV1hHc());
                                shopBurstInfo.setShopUV1hHc(shopBurstInfo.getShopUV1hHc() * alpha);
                                count = 0;
                            }
                            shopBurstInfo.setPreHcSku5min(shopBurstInfo.getShopSkuUV5minHc());
                            shopBurstInfo.setShopSkuUV5minHc(shopBurstInfo.getShopSkuUV5minHc() * alpha);
                            shopBurstInfo.setPreHcUV5min(shopBurstInfo.getShopUV5minHc());
                            shopBurstInfo.setShopUV5minHc(shopBurstInfo.getShopUV5minHc() * alpha);
                        }
                    }
                    //userDimensionBurstCache  移除这个为空的 ,同一个维度
                    if (shopBurstInfoHashMap.size() == 0) {
                        userDimensionBurstCache.remove(dimensionValue);
                        userDimensionToSeg.remove(dimensionValue);
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

        }
    }

    /**
     * 累加shopId
     *
     * @param eventFeature
     * @param shopBurstInfo
     */
    public void accumulateShopId(EventFeature eventFeature, ShopBurstInfo shopBurstInfo) {
        if (eventFeature.getType().equals(BehaviorField.CLICK.toString())) {
            // 5min
            if (eventFeature.isShop5m()) {
                shopBurstInfo.addshopSkuUV5min();
            }
            // 1h
            if (eventFeature.isShop1h()) {
                shopBurstInfo.addShopSkuUV1h();
            }
        }
        if (eventFeature.getType().equals(BehaviorField.EXPOSURE.toString())) {
            if (eventFeature.isShop5m()) {
                shopBurstInfo.addshopUV5min();
            }
            if (eventFeature.isShop1h()) {
                shopBurstInfo.addShopUV1h();
            }
        }
    }

    public static class TopNComparator implements Comparator<ShopWithScore> {
        @Override
        public int compare(ShopWithScore o1, ShopWithScore o2) {
            if (o1.getScore() == o2.getScore()) {
                return 0;
            } else if (o1.getScore() > o2.getScore()) {
                return 1;
            } else {
                return -1;
            }
        }
    }

}
