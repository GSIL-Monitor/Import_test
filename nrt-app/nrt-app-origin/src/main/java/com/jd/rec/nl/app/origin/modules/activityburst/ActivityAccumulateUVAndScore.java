package com.jd.rec.nl.app.origin.modules.activityburst;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.activityburst.domain.*;
import com.jd.rec.nl.service.base.quartet.WindowCollector;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.trace.RuntimeTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recsys.prediction_service.UnifiedItemBurstFeatureOuterClass;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ForkJoinTask;

/**
 * 累加当前窗口的activityId uv,并在窗口结束时计算activityId爆品得分以及收集每个segmentation的topN activityId召回
 * 具体做法为:
 * 1.在窗口累计期,按用户维度的组合feature累加各activityId的UV,并记录对应用户feature类型的segmentationConfig以及activityId对应的activityId维度feature值
 * 2.在窗口结束时,按用户维度计算每个activityId的爆品得分,并根据记录的activityId feature值和对应segmentationConfig组合用户维度值生成完整的segmentation,
 * 获取每个segmentation下的爆品得分取top100
 *
 * @author linmx
 * @date 2018/7/24
 */
public class ActivityAccumulateUVAndScore implements WindowCollector<Long, EventFeature> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityAccumulateUVAndScore.class);

    private static final int featurePartitionNum = 1000;

    String name = "activityBurst";

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
    private ActivityBurstCoefficientService burstCoefficientService;

    @Inject
    @Named("HC_Tll")
    private Duration hcTimeToLive;

    @Inject(optional = true)
    @Named("windowSize")
    //  private Duration windowSize = Duration.ofSeconds(10);
    private Duration windowSize = Duration.ofMinutes(5);
    @Inject
    private RuntimeTrace runtimeTrace;

    /**
     * 当前窗口用户维度的activityId uv量统计
     */
    private Map<DimensionValue, HashMap<Long, BurstInfo>> userDimensionBurstCache = new HashMap<>();

    /**
     * 当前窗口下用户维度对应的所属segmentation
     */
    private Map<DimensionValue, Set<SegmentationConfig>> userDimensionToSeg = new HashMap<>();

    private Map<Long, Map<String, Object>> activityFeatures = new HashMap<>();

    private List<SegmentationConfig> segmentationConfigs;

    public ActivityAccumulateUVAndScore() {
    }

    @Inject
    public ActivityAccumulateUVAndScore(@Named("segmentation") List<SegmentationConfig> segmentationConfigs) {
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

    @Override
    public void collect(Long activityId, EventFeature eventFeature) {
        if (activityFeatures.isEmpty()) {
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
                // 讲用户维度的具体值关联到segmentationConfig
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
            // 累加各用户维度下的activityId uv
            HashMap<Long, BurstInfo> activityIdBurstInfo;
            if (userDimensionBurstCache.containsKey(userDimensionValue)) {
                activityIdBurstInfo = userDimensionBurstCache.get(userDimensionValue);
            } else {
                activityIdBurstInfo = new HashMap<>();
                userDimensionBurstCache.put(userDimensionValue, activityIdBurstInfo);
            }
            if (activityIdBurstInfo.containsKey(activityId)) {
                activityIdBurstInfo.get(activityId).addUV();
            } else {
                HistoryCUV lastHistory = burstCoefficientService.getHistoryCUV(userDimensionValue, activityId,
                        System.currentTimeMillis());
                String featureKeyPost = userDimensionValue.getFeatureKey();
                String featureKey = String.valueOf(activityId).concat(featureKeyPost);
                BurstInfo burstInfo = new BurstInfo(featureKey, alpha, punishment, windowSize, lastHistory);
                activityIdBurstInfo.put(activityId, burstInfo);
            }
        });

        // 记录activityId维度的属性
        if (!activityFeatures.containsKey(activityId)) {
            activityFeatures.put(activityId, eventFeature.getItemFeature());
        }
    }

    @Override
    public void shuffle(ResultCollection resultCollection) {
        ForkJoinTask task = null;
        try {
            long current = System.currentTimeMillis();
            if (userDimensionBurstCache.size() == 0) {
                return;
            } else {
                LOGGER.debug("user dimension size:{}, \n {}", userDimensionBurstCache.size(), userDimensionBurstCache.keySet());
                userDimensionBurstCache.entrySet().stream().forEach(dimensionEntry -> {
                    DimensionValue userDimensionValue = dimensionEntry.getKey();
                    HashMap<Long, BurstInfo> activityIdBurstInfo = dimensionEntry.getValue();

                    LOGGER.debug("computer the user dimension:{}, activityId size:{}", userDimensionValue.getFeatures(),
                            activityIdBurstInfo.size());

                    // 当前用户维度下的segmentation定义
                    Set<SegmentationConfig> recallSegmentation = userDimensionToSeg.get(userDimensionValue);
                    // 当前用户维度下各segmentation的topN召回
                    Map<SegmentationInfo, Queue<ActivityIdWithScore>> recallTopN = new HashMap<>();

                    // 计算此维度下activityId的得分addUV
                    // BurstFeature.BurstFeatureList.Builder listBuilder = BurstFeature.BurstFeatureList.newBuilder();

                    activityIdBurstInfo.forEach((activityId, burstInfo) -> {

                        double scoreDouble = burstInfo.getScore();
                        LOGGER.debug("{}:{}--> preHC:{}, HC:{}, score:{}", userDimensionValue.toString(), activityId,
                                burstInfo
                                        .getPreHc(), burstInfo.getHc(), scoreDouble);

                        // 保存新的hc
                        HistoryCUV newHC = new HistoryCUV();
                        newHC.setTimestamp(burstInfo.getTime());
                        newHC.setCoefficient(burstInfo.getHc());
                        burstCoefficientService.saveNewHistoryCUV(userDimensionValue, activityId, newHC, hcTimeToLive);

                        UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.Builder builder =
                                UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.newBuilder();
                        builder.setItemId(activityId);
                        builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("time")
                                .setValue(current / 1000));
                        builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("hc")
                                .setValue(burstInfo.getPreHc() * 10000));
                        builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("uv")
                                .setValue(burstInfo.getUv()));

                        //                                BurstFeature.BurstFeatureInfo.Builder builder = BurstFeature
                        // .BurstFeatureInfo.newBuilder();
                        //                                builder.setTime(current / 1000);
                        //                                builder.setHc(new Double(burstInfo.getPreHc() * 10000)
                        // .intValue());
                        //                                builder.setItemId(activityId);
                        //                                builder.setUv(burstInfo.getUv());
                        resultCollection.addOutput(this.getName(), featureServiceType, featureSubType,
                                burstInfo.getFeatureKey(), builder.build().toByteArray(), hcTimeToLive);

                        // 日志记录
                        runtimeTrace.info("actBurstFeature", burstInfo);

                        // 计算分数
                        //                                ActivityIdWithScore score = new ActivityIdWithScore
                        // (activityId, scoreDouble);
                        //                                // 将activityId增加到对应segmentation的topN中进行分数比对
                        //                                Map<String, Object> feature = this.activityFeatures.get
                        // (activityId);
                        //   if (feature != null && feature.size() > 0) {
                        //                                if (feature != null) {
                        //                                    recallSegmentation.stream().forEach(segmentationConfig -> {
                        //                                        Set<SegmentationInfo> segmentationsInfo =
                        // segmentationConfig.getSegmentation
                        //                                                (userDimensionValue, feature);
                        //                                        segmentationsInfo.forEach(segmentationInfo -> {
                        //                                            Queue<ActivityIdWithScore> topN;
                        //                                            if (recallTopN.containsKey(segmentationInfo)) {
                        //                                                topN = recallTopN.get(segmentationInfo);
                        //                                            } else {
                        //                                                topN = new PriorityQueue<>(new TopNComparator
                        // ());
                        //                                                recallTopN.put(segmentationInfo, topN);
                        //                                            }
                        //                                            if (topN.size() < segmentationConfig.getTopSize()) {
                        //                                                topN.add(score);
                        //                                            } else {
                        //                                                if (topN.peek().getScore() < score.getScore()) {
                        //                                                    topN.poll();
                        //                                                    topN.add(score);
                        //                                                }
                        //                                            }
                        //                                        });
                        //                                    });
                        //                                }
                    });

                    //                            // 生成 pb recall
                    //                            recallTopN.forEach(((segmentationInfo, activityIdWithScores) ->
                    //                                    resultCollection.addMapResult(this.getName(), segmentationInfo,
                    //                                            new ArrayList<>(activityIdWithScores))
                    //                            ));
                });
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            // 清空缓存
            LOGGER.debug("finish shuffle,clear cache.");
            //            runtimeTrace.info("test", "clear");
            this.userDimensionBurstCache.clear();
            this.userDimensionToSeg.clear();
            this.activityFeatures.clear();
        }
    }

    private ActivityIdWithScore getScore(long activityId, int uv, HistoryCUV historyCUV) {
        double score = (uv + punishment) / (historyCUV.getCoefficient() + punishment);
        return new ActivityIdWithScore(activityId, score);
    }

    public static class TopNComparator implements Comparator<ActivityIdWithScore> {

        @Override
        public int compare(ActivityIdWithScore o1, ActivityIdWithScore o2) {
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
