package com.jd.rec.nl.app.origin.modules.promotionalburst;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.SegmentationConfig;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.SegmentationInfo;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.SkuWithScore;
import com.jd.rec.nl.service.base.quartet.Reducer;
import com.jd.rec.nl.service.base.quartet.domain.MapResult;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.trace.RuntimeTrace;
import com.jd.si.diviner.PredictionItem;
import com.jd.si.diviner.SKUData;
import com.jd.si.util.ContainerSerializer;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 汇总各分片的爆品召回,并计算topN
 *
 * @author linmx
 * @date 2018/7/24
 */
public class ReduceBurstRecall implements Reducer<SegmentationInfo, ArrayList<SkuWithScore>> {

    private static final Logger LOGGER = getLogger(ReduceBurstRecall.class);

    @Inject
    public RuntimeTrace runtimeTrace;

    String name = "burst";

    private Map<String, SegmentationConfig> segmentationConfigs = new HashMap<>();

    @Inject(optional = true)
    @Named("windowSize")
    private Duration windowSize = Duration.ofMinutes(5);

    @Inject
    @Named("recallServiceType")
    private String recallServiceType;

    @Inject
    @Named("recallSubType")
    private int recallSubType;

    @Inject(optional = true)
    @Named("recallOutput")
    private boolean recallOutput = true;

    @Inject(optional = true)
    @Named("versionPostfix")
    private String versionPostfix = "";

    private Map<SegmentationInfo, Queue<SkuWithScore>> topSkus = new HashMap<>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 序列化操作
     *
     * @param recallList
     * @return
     */
    private byte[] serializeRecallList(Collection<SkuWithScore> recallList) {
        List<PredictionItem> predictionItems = new ArrayList<>();
        recallList.forEach(skuWithScore -> {
            PredictionItem item = new PredictionItem();
            item.setSkuData(new SKUData().setSku(skuWithScore.getSku()));
            item.setWeight(skuWithScore.getScore());
            predictionItems.add(item);
        });
        return ContainerSerializer.getCompactSerializer().listToCompactBytes(predictionItems).toBytes();
    }

    @Override
    public void collect(SegmentationInfo segmentationInfo, ArrayList<SkuWithScore> shardingTop) {
        if (this.topSkus.containsKey(segmentationInfo)) {
            Queue<SkuWithScore> mergedTop = this.topSkus.get(segmentationInfo);
            shardingTop.stream().forEach(skuWithScore -> {
                if (mergedTop.size() < segmentationInfo.getTopSize()) {
                    mergedTop.add(skuWithScore);
                } else {
                    if (mergedTop.peek().getScore() < skuWithScore.getScore()) {
                        mergedTop.poll();
                        mergedTop.add(skuWithScore);
                    }
                }
            });
        } else {
            PriorityQueue<SkuWithScore> topQueue = new PriorityQueue<>(new AccumulateUVAndScore.TopNComparator());
            topQueue.addAll(shardingTop);
            this.topSkus.put(segmentationInfo, topQueue);
        }
    }

    @Override
    public void reduce(ResultCollection resultCollection) {
        try {
            //            String current = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss").concat(":");
            this.topSkus.forEach(((segmentationInfo, skuWithScores) -> {
                String key = segmentationInfo.toString().concat(this.versionPostfix);
                // 由于文档原因导致下游的key与定义不一致,特殊处理一下
                if (key.indexOf("brandId") != -1)
                    key = key.replaceAll("brandId", "brand");
                //                try {
                //                    dbService.save(this.getNamespace(), "statistics", key, current.concat(skuWithScores
                // .toString()), Duration
                //                            .ofMinutes(5));
                //                } catch (Exception e) {
                //                    e.printStackTrace();
                //                }
                MapResult<String, String> traceValue = new MapResult<>(this.getNamespace(), key, skuWithScores.toString());
                runtimeTrace.info("burstRecall", traceValue);
                //                if (recallOutput)
                resultCollection.addOutput(this.getName(), recallServiceType, recallSubType, key,
                        serializeRecallList(skuWithScores), windowSize);
            }));
        } finally {
            this.topSkus.clear();
        }
    }
}
