package com.jd.rec.nl.app.origin.modules.themeburst;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.popularshop.domain.FeatureLog;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.DimensionValue;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.SegmentationConfig;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.SegmentationInfo;
import com.jd.rec.nl.app.origin.modules.themeburst.domain.ThemeEventFeature;
import com.jd.rec.nl.app.origin.modules.themeburst.domain.ThemeScoreComputer;
import com.jd.rec.nl.app.origin.modules.themeburst.domain.ThemeViewInfo;
import com.jd.rec.nl.app.origin.modules.themeburst.domain.ThemeWithScore;
import com.jd.rec.nl.service.base.quartet.WindowCollector;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.trace.RuntimeTrace;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;
import recsys.prediction_service.UnifiedItemBurstFeatureOuterClass;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * test
 *
 * @author linmx
 * @date 2018/9/29
 */
public class AccumulateThemeUV implements WindowCollector<Long, ThemeEventFeature> {

    private static final Logger LOGGER = getLogger(AccumulateThemeUV.class);

    private String name = "themeBurst";

    private Duration windowSize = Duration.ofMinutes(10);

    @Inject
    @Named("featureServiceType")
    private String featureServiceType;

    @Inject
    @Named("featureSubType")
    private int featureSubType;

    @Inject
    private RuntimeTrace runtimeTrace;

    @Inject
    @Named("alpha")
    private double alpha;

    @Inject
    @Named("HC_Tll")
    private Duration hcTimeToLive;

    private Set<Integer> featureList = new HashSet<>();

    @Inject
    private HistoryCoefficientService historyCoefficientService;

    /**
     * 当前窗口用户维度的uv量统计
     */
    private Map<DimensionValue, HashMap<Long, ThemeViewInfo>> userDimensionClickCache = new HashMap<>();

    /**
     * 当前窗口下用户维度对应的所属segmentation
     */
    private Map<DimensionValue, Set<SegmentationConfig>> userDimensionToSeg = new HashMap<>();

    @Inject
    @Named("segmentation")
    private List<SegmentationConfig> segmentationConfigs;

    public AccumulateThemeUV() {
    }

    @Inject
    public AccumulateThemeUV(@Named("featureList") List<Duration> featureList, @Named("windowSize") Optional<Duration>
            windowSize) {
        if (windowSize.isPresent()) {
            this.windowSize = windowSize.get();
        }
        featureList.stream().forEach(feature -> this.featureList.add(Long.valueOf(feature.getSeconds() / this.windowSize
                .getSeconds()).intValue()));
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
    public void collect(Long themeId, ThemeEventFeature eventFeature) {
        segmentationConfigs.stream().forEach(segmentationConfig -> {
            // 用户维度具体值
            Set<DimensionValue> dimensionValues = segmentationConfig.getUserDimensions(eventFeature);

            dimensionValues.forEach(userDimensionValue -> {
                if (userDimensionValue.getFeatures() == null || userDimensionValue.getFeatures().size() == 0) {
                    LOGGER.error("config:{} lead a null !! user features: {}", segmentationConfig.getUserDimension(),
                            eventFeature.getUserFeature());
                    return;
                }
                // 讲用户维度的具体值关联到segmentationConfig
                if (this.userDimensionToSeg.containsKey(userDimensionValue)) {
                    this.userDimensionToSeg.get(userDimensionValue).add(segmentationConfig);
                } else {
                    Set<SegmentationConfig> configs = new HashSet<>();
                    configs.add(segmentationConfig);
                    this.userDimensionToSeg.put(userDimensionValue, configs);
                }

                // 累加各用户维度下的sku uv
                HashMap<Long, ThemeViewInfo> themeViewInfos;
                if (userDimensionClickCache.containsKey(userDimensionValue)) {
                    themeViewInfos = userDimensionClickCache.get(userDimensionValue);
                } else {
                    themeViewInfos = new HashMap<>();
                    userDimensionClickCache.put(userDimensionValue, themeViewInfos);
                }
                if (themeViewInfos.containsKey(themeId)) {
                    ThemeViewInfo themeViewInfo = themeViewInfos.get(themeId);
                    if (eventFeature.getBehaviorField() == BehaviorField.EXPOSURE) {
                        // 主题页曝光
                        eventFeature.getUvCondition().forEach(period -> themeViewInfo.uvAdd(period));
                    } else {
                        //主题页点击
                        themeViewInfo.clkAdd();
                    }
                } else {
                    ThemeViewInfo themeViewInfo = new ThemeViewInfo(themeId, userDimensionValue.getFeatureKey(), this
                            .featureList);
                    themeViewInfos.put(themeId, themeViewInfo);
                    if (eventFeature.getBehaviorField() == BehaviorField.EXPOSURE) {
                        // 主题页曝光
                        eventFeature.getUvCondition().forEach(period -> themeViewInfo.uvAdd(period));
                    } else {
                        //主题页点击
                        themeViewInfo.clkAdd();
                    }
                }
            });
        });
    }

    @Override
    public int windowSize() {
        return Long.valueOf(windowSize.getSeconds() / defaultInterval.getSeconds()).intValue();
    }

    @Override
    public void shuffle(ResultCollection resultCollection) {
        long current = System.currentTimeMillis();
        try {
            if (this.userDimensionClickCache.size() == 0) {
                return;
            } else {
                LOGGER.debug("user dimension size:{}, \n {}", userDimensionClickCache.size(),
                        userDimensionClickCache.keySet());
                userDimensionClickCache.entrySet().parallelStream().forEach(dimensionEntry -> {
                    DimensionValue userDimensionValue = dimensionEntry.getKey();
                    HashMap<Long, ThemeViewInfo> viewInfos = dimensionEntry.getValue();

                    LOGGER.debug("computer the user dimension:{}, sku size:{}", userDimensionValue.getFeatures(),
                            viewInfos.size());

                    // 当前用户维度下的segmentation定义
                    Set<SegmentationConfig> recallSegmentation = userDimensionToSeg.get(userDimensionValue);

                    // 当前用户维度下各segmentation的topN召回,有四种算法
                    Map<SegmentationInfo, HashMap<String, Queue<ThemeWithScore>>> recallTopN = new HashMap<>();

                    try {
                        this.historyCoefficientService.getAndRefreshHc(this.getName().concat("_hc"), userDimensionValue,
                                viewInfos, alpha, this.hcTimeToLive);
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                        return;
                    }
                    // 计算此维度下的feature
                    viewInfos.forEach((themeId, themeViewInfo) -> {

                        LOGGER.debug("{}:{}--> uv:{}, clk:{}", userDimensionValue.toString(), themeId,
                                themeViewInfo.getPeriodUVCount(), themeViewInfo.getPeriodClkCount());

                        // 生成feature
                        UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.Builder builder =
                                UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.newBuilder();
                        builder.setItemId(themeId);
                        builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("time")
                                .setValue(current / 1000));
                        featureList.stream().forEach(period -> {
                            String minutes = Long.valueOf(period * windowSize.toMinutes()).toString();
                            minutes = minutes.equals("0") ? "10" : minutes;
                            long uv = themeViewInfo.getUV(period);
                            long clk = themeViewInfo.getClkCount(period);
                            double cpu = 0;
                            if (uv != 0) {
                                cpu = BigDecimal.valueOf(clk).divide(BigDecimal.valueOf(uv), 4, BigDecimal
                                        .ROUND_HALF_UP).doubleValue();
                            }
                            builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("uv" +
                                    minutes).setValue(uv));
                            builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("clk" +
                                    minutes).setValue(clk));
                            builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("cpu" +
                                    minutes).setValue(cpu));
                        });
                        // 增加窗口期的hc作为feature输出
                        builder.addFeature(
                                UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("hc10").setValue
                                        (BigDecimal.valueOf(themeViewInfo.getPreHc())
                                                .multiply(BigDecimal.valueOf(10000))
                                                .setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue()));

                        resultCollection.addOutput(this.getName(), featureServiceType, featureSubType,
                                themeViewInfo.getFeatureKey(), builder.build().toByteArray(), Duration.ofHours(1));

                        // 日志记录
                        runtimeTrace.info("themeBurstFeature", new FeatureLog<>(themeViewInfo, builder));

                        // 计算召回使用的分数
                        Map<String, ThemeWithScore> versionScores = ThemeScoreComputer.getScoreComputers().stream()
                                .collect(Collectors.toMap(computer -> computer.getName(),
                                        computer -> new ThemeWithScore(themeId, computer.getScore(builder))
                                ));
                        LOGGER.debug("{}:{}--> {}", userDimensionValue.toString(), themeId, versionScores.toString());

                        // 将theme增加到对应segmentation的topN中进行分数比对
                        recallSegmentation.forEach(segmentationConfig -> {
                            Set<SegmentationInfo> segmentationsInfo = segmentationConfig.getSegmentation
                                    (userDimensionValue, null);
                            segmentationsInfo.forEach(segmentationInfo -> {
                                HashMap<String, Queue<ThemeWithScore>> topNversions;
                                if (recallTopN.containsKey(segmentationInfo)) {
                                    topNversions = recallTopN.get(segmentationInfo);
                                } else {
                                    topNversions = new HashMap<>();
                                    ThemeScoreComputer.getScoreComputers().stream().forEach(computer ->
                                            topNversions.put(computer.getName(),
                                                    new PriorityQueue<>(new TopNComparator())));
                                    recallTopN.put(segmentationInfo, topNversions);
                                }
                                // 进行比对获取topN
                                versionScores.forEach((version, score) -> {
                                    Queue<ThemeWithScore> topN = topNversions.get(version);
                                    if (topN.size() < segmentationConfig.getTopSize()) {
                                        topN.add(score);
                                    } else {
                                        if (topN.peek().getScore() < score.getScore()) {
                                            topN.poll();
                                            topN.add(score);
                                        }
                                    }
                                });
                            });
                        });
                    });

                    LOGGER.debug("recall:-->{}", recallTopN.toString());

                    // 生成 pb recall
                    recallTopN.forEach(((segmentationInfo, scores) ->
                            resultCollection.addMapResult(this.getName(), segmentationInfo, scores)
                    ));
                });
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            // 清空缓存
            for (DimensionValue dimensionValue : new HashSet<>(userDimensionClickCache.keySet())) {
                HashMap<Long, ThemeViewInfo> clkCache = userDimensionClickCache.get(dimensionValue);
                for (long themeId : new HashSet<>(clkCache.keySet())) {
                    ThemeViewInfo viewInfo = clkCache.get(themeId);
                    // 做过期处理
                    if (viewInfo.clear()) {
                        clkCache.remove(themeId);
                    }
                }
                if (clkCache.isEmpty()) {
                    this.userDimensionClickCache.remove(dimensionValue);
                    this.userDimensionToSeg.remove(dimensionValue);
                }
            }
        }
    }

    public static class TopNComparator implements Comparator<ThemeWithScore>, Serializable {
        @Override
        public int compare(ThemeWithScore o1, ThemeWithScore o2) {
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
