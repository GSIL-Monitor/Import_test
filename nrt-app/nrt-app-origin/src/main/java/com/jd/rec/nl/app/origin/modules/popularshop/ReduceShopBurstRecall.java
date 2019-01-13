package com.jd.rec.nl.app.origin.modules.popularshop;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.popularshop.domain.SegmentationConfig;
import com.jd.rec.nl.app.origin.modules.popularshop.domain.SegmentationInfo;
import com.jd.rec.nl.app.origin.modules.popularshop.domain.ShopWithScore;
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
 * @author wl
 * @date 2018/9/20
 */
public class ReduceShopBurstRecall implements Reducer<SegmentationInfo, ArrayList<ShopWithScore>> {

    private static final Logger LOGGER = getLogger(ReduceShopBurstRecall.class);

    @Inject
    public RuntimeTrace runtimeTrace;

    String name = "popularshop";

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

    @Inject
    @Named("HC_Tll")
    private Duration liveTime;

    /**
     * 1小时的商详页
     */
    private Map<SegmentationInfo, Queue<ShopWithScore>> topShop1h = new HashMap<>();

    /**
     * 5分钟的商详页
     */
    private Map<SegmentationInfo, Queue<ShopWithScore>> topShop5min = new HashMap<>();


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
    private byte[] serializeRecallList(Collection<ShopWithScore> recallList) {
        List<PredictionItem> predictionItems = new ArrayList<>();
        recallList.forEach(shopWithScore -> {
            PredictionItem item = new PredictionItem();
            item.setSkuData(new SKUData().setSku(shopWithScore.getShopId()));
            item.setWeight(shopWithScore.getScore());
            predictionItems.add(item);
        });
        return ContainerSerializer.getCompactSerializer().listToCompactBytes(predictionItems).toBytes();
    }

    @Override
    public void collect(SegmentationInfo segmentationInfo, ArrayList<ShopWithScore> shardingTop) {
        Map<SegmentationInfo, Queue<ShopWithScore>> topShop = segmentationInfo.getVersion().equals("v1") ? topShop1h : topShop5min;
        if (topShop.containsKey(segmentationInfo)) {
            Queue<ShopWithScore> mergedTop = topShop.get(segmentationInfo);
            shardingTop.stream().forEach(ShopWithScore -> {
                if (mergedTop.size() < segmentationInfo.getTopSize()) {
                    mergedTop.add(ShopWithScore);
                } else {
                    if (mergedTop.peek().getScore() < ShopWithScore.getScore()) {
                        mergedTop.poll();
                        mergedTop.add(ShopWithScore);
                    }
                }
            });
        } else {
            PriorityQueue<ShopWithScore> topQueue = new PriorityQueue<>(new AccumulateShopUVAndScore.TopNComparator());
            topQueue.addAll(shardingTop);
            topShop.put(segmentationInfo, topQueue);
        }
    }

    @Override
    public void reduce(ResultCollection resultCollection) {
        try {
            this.topShop1h.forEach(((segmentationInfo, shopWithScores) -> {
                String key = segmentationInfo.toString() + "_" + segmentationInfo.getVersion();
                MapResult<String, String> traceValue = new MapResult<>(this.getNamespace(), key, shopWithScores.toString());
                runtimeTrace.info("shopBurstRecall", traceValue);
                LOGGER.debug("shopBurstRecall:" + shopWithScores + ":" + key);
                //召回统一输出
                resultCollection.addOutput(this.getName(), recallServiceType, recallSubType, key,
                        serializeRecallList(shopWithScores), liveTime);
            }));

            this.topShop5min.forEach(((segmentationInfo, shopWithScores) -> {
                String key = segmentationInfo.toString() + "_" + segmentationInfo.getVersion();
                MapResult<String, String> traceValue = new MapResult<>(this.getNamespace(), key, shopWithScores.toString());
                runtimeTrace.info("shopBurstRecall", traceValue);
                LOGGER.debug("shopBurstRecall:" + shopWithScores + ":" + key);
                //召回统一输出
                resultCollection.addOutput(this.getName(), recallServiceType, recallSubType, key,
                        serializeRecallList(shopWithScores), liveTime);
            }));
        } finally {
            topShop5min.clear();
            topShop1h.clear();
        }
    }
}
