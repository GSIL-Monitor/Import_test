package com.jd.rec.nl.app.origin.modules.promotionalburst;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.*;
import com.jd.rec.nl.service.base.quartet.WindowCollector;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.trace.RuntimeTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recsys.prediction_service.BurstFeature;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

/**
 * 累加当前窗口的sku uv,并在窗口结束时计算sku爆品得分以及收集每个segmentation的topN sku召回
 * 具体做法为:
 * 1.在窗口累计期,按用户维度的组合feature累加各sku的UV,并记录对应用户feature类型的segmentationConfig以及sku对应的sku维度feature值
 * 2.在窗口结束时,按用户维度计算每个sku的爆品得分,并根据记录的sku feature值和对应segmentationConfig组合用户维度值生成完整的segmentation,
 * 获取每个segmentation下的爆品得分取top100
 *
 * @author linmx
 * @date 2018/7/24
 */
public class AccumulateUVAndScore implements WindowCollector<Long, EventFeature> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccumulateUVAndScore.class);

    private static final int featurePartitionNum = 1000;

    String name = "burst";

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

    @Inject(optional = true)
    @Named("versionPostfix")
    private String versionPostfix = "";

    @Inject
    private BurstCoefficientService burstCoefficientService;

    @Inject
    @Named("HC_Tll")
    private Duration hcTimeToLive;

    @Inject(optional = true)
    @Named("threadNum")
    private int threadNum = 8;

    private ForkJoinPool forkJoinPool;

    @Inject(optional = true)
    @Named("windowSize")
    private Duration windowSize = Duration.ofMinutes(5);

    @Inject(optional = true)
    @Named("recall_uv_threshold")
    private int recallUVThreshold = 200;

    @Inject
    private RuntimeTrace runtimeTrace;

    /**
     * 当前窗口用户维度的sku uv量统计
     */
    private Map<DimensionValue, HashMap<Long, BurstInfo>> userDimensionBurstCache = new HashMap<>();

    /**
     * 当前窗口下用户维度对应的所属segmentation
     */
    private Map<DimensionValue, Set<SegmentationConfig>> userDimensionToSeg = new HashMap<>();

    private Map<Long, Map<String, Object>> skuFeatures = new HashMap<>();

    private List<SegmentationConfig> segmentationConfigs;

    public AccumulateUVAndScore() {
    }

    @Inject
    public AccumulateUVAndScore(@Named("segmentation") List<SegmentationConfig> segmentationConfigs) {
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
    public void collect(Long sku, EventFeature eventFeature) {
        if (skuFeatures.isEmpty()) {
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
            // 累加各用户维度下的sku uv
            HashMap<Long, BurstInfo> skuBurstInfo;
            if (userDimensionBurstCache.containsKey(userDimensionValue)) {
                skuBurstInfo = userDimensionBurstCache.get(userDimensionValue);
            } else {
                skuBurstInfo = new HashMap<>();
                userDimensionBurstCache.put(userDimensionValue, skuBurstInfo);
            }
            if (skuBurstInfo.containsKey(sku)) {
                skuBurstInfo.get(sku).addUV();
            } else {
                HistoryCUV lastHistory = burstCoefficientService.getHistoryCUV(userDimensionValue, sku,
                        System.currentTimeMillis(), this.versionPostfix);
                String featureKeyPost = userDimensionValue.getFeatureKey();
                String featureKey = String.valueOf(sku).concat(featureKeyPost).concat(this.versionPostfix);
                BurstInfo burstInfo = new BurstInfo(featureKey, alpha, punishment, windowSize, lastHistory);
                skuBurstInfo.put(sku, burstInfo);
            }
        });

        // 记录sku维度的属性
        if (!skuFeatures.containsKey(sku)) {
            skuFeatures.put(sku, eventFeature.getItemFeature());
        }
    }

    @Override
    public void shuffle(ResultCollection resultCollection) {
        if (forkJoinPool == null) {
            forkJoinPool = new ForkJoinPool(this.threadNum);
        }
        long current = System.currentTimeMillis();
        ForkJoinTask task = null;
        try {
            if (userDimensionBurstCache.size() == 0) {
                return;
            } else {
                LOGGER.debug("user dimension size:{}, \n {}", userDimensionBurstCache.size(), userDimensionBurstCache.keySet());
                task = forkJoinPool.submit(() ->
                        userDimensionBurstCache.entrySet().parallelStream().forEach(dimensionEntry -> {
                            DimensionValue userDimensionValue = dimensionEntry.getKey();
                            HashMap<Long, BurstInfo> skuBurstInfo = dimensionEntry.getValue();

                            LOGGER.debug("computer the user dimension:{}, sku size:{}", userDimensionValue.getFeatures(),
                                    skuBurstInfo.size());

                            // 当前用户维度下的segmentation定义
                            Set<SegmentationConfig> recallSegmentation = userDimensionToSeg.get(userDimensionValue);
                            // 当前用户维度下各segmentation的topN召回
                            Map<SegmentationInfo, Queue<SkuWithScore>> recallTopN = new HashMap<>();

                            // 计算此维度下sku的得分
                            // BurstFeature.BurstFeatureList.Builder listBuilder = BurstFeature.BurstFeatureList.newBuilder();

                            skuBurstInfo.forEach((sku, burstInfo) -> {

                                double scoreDouble = burstInfo.getScore();
                                LOGGER.debug("{}{}:{}--> preHC:{}, HC:{}, score:{}", userDimensionValue.toString(),
                                        this.versionPostfix, sku, burstInfo.getPreHc(), burstInfo.getHc(), scoreDouble);

                                // 保存新的hc
                                HistoryCUV newHC = new HistoryCUV();
                                newHC.setTimestamp(burstInfo.getTime());
                                newHC.setCoefficient(burstInfo.getHc());
                                burstCoefficientService.saveNewHistoryCUV(userDimensionValue, sku, newHC, hcTimeToLive,
                                        this.versionPostfix);

                                BurstFeature.BurstFeatureInfo.Builder builder = BurstFeature.BurstFeatureInfo.newBuilder();
                                builder.setTime(current / 1000);
                                builder.setHc(new Double(burstInfo.getPreHc() * 10000).intValue());
                                builder.setItemId(sku);
                                builder.setUv(burstInfo.getUv());

                                resultCollection.addOutput(this.getName(), featureServiceType, featureSubType,
                                        burstInfo.getFeatureKey(), builder.build().toByteArray(), hcTimeToLive);

                                // 日志记录
                                runtimeTrace.info("burstFeature", burstInfo);

                                // 对于global分片,对召回的uv值进行阈值控制,临时处理
                                if (burstInfo.getFeatureKey().indexOf("global") != -1 && burstInfo.getUv() < recallUVThreshold) {
                                    return;
                                }

                                // 计算分数
                                SkuWithScore score = new SkuWithScore(sku, scoreDouble);
                                // 将sku增加到对应segmentation的topN中进行分数比对
                                Map<String, Object> feature = this.skuFeatures.get(sku);
                                if (feature != null && feature.size() > 0) {
                                    recallSegmentation.stream().forEach(segmentationConfig -> {
                                        Set<SegmentationInfo> segmentationsInfo = segmentationConfig.getSegmentation
                                                (userDimensionValue, feature);
                                        segmentationsInfo.forEach(segmentationInfo -> {
                                            Queue<SkuWithScore> topN;
                                            if (recallTopN.containsKey(segmentationInfo)) {
                                                topN = recallTopN.get(segmentationInfo);
                                            } else {
                                                topN = new PriorityQueue<>(new TopNComparator());
                                                recallTopN.put(segmentationInfo, topN);
                                            }
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
                                }
                            });

                            // 生成 pb recall
                            recallTopN.forEach(((segmentationInfo, skuWithScores) ->
                                    resultCollection.addMapResult(this.getName(), segmentationInfo,
                                            new ArrayList<>(skuWithScores))
                            ));
                        }));
                task.get(windowSize.getSeconds(), TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            LOGGER.error(String.format("%s run timeOut:%s", this.getName(), e.getMessage()), e);
            if (task != null)
                task.cancel(true);
        } finally {
            // 清空缓存
            LOGGER.debug("finish shuffle,clear cache.[{}]", this.skuFeatures.toString());
            //            runtimeTrace.info("test", "clear");
            this.userDimensionBurstCache.clear();
            this.userDimensionToSeg.clear();
            this.skuFeatures.clear();
        }
    }

    private SkuWithScore getScore(long sku, int uv, HistoryCUV historyCUV) {
        double score = (uv + punishment) / (historyCUV.getCoefficient() + punishment);
        return new SkuWithScore(sku, score);
    }

    public static class TopNComparator implements Comparator<SkuWithScore> {

        @Override
        public int compare(SkuWithScore o1, SkuWithScore o2) {
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
