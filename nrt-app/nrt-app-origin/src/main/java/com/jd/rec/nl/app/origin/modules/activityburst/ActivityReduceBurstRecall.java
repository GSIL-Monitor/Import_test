package com.jd.rec.nl.app.origin.modules.activityburst;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.jd.rec.nl.app.origin.modules.activityburst.domain.ActivityIdWithScore;
import com.jd.rec.nl.app.origin.modules.activityburst.domain.SegmentationConfig;
import com.jd.rec.nl.app.origin.modules.activityburst.domain.SegmentationInfo;
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
public class ActivityReduceBurstRecall implements Reducer<SegmentationInfo, ArrayList<ActivityIdWithScore>> {

    private static final Logger LOGGER = getLogger(ActivityReduceBurstRecall.class);

    @Inject
    public RuntimeTrace runtimeTrace;

    String name = "activityBurst";

    private Map<String, SegmentationConfig> segmentationConfigs = new HashMap<>();

    @Inject(optional = true)
    @Named("windowSize")
    private Duration windowSize = Duration.ofMinutes(1);

    @Inject
    @Named("recallServiceType")
    private String recallServiceType;

    @Inject
    @Named("recallSubType")
    private int recallSubType;

    @Inject(optional = true)
    @Named("recallOutput")
    private boolean recallOutput = true;


    private Map<SegmentationInfo, Queue<ActivityIdWithScore>> topActivityIds = new HashMap<>();

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
    private byte[] serializeRecallList(Collection<ActivityIdWithScore> recallList) {
        List<PredictionItem> predictionItems = new ArrayList<>();
        recallList.forEach(activityIdWithScore -> {
            PredictionItem item = new PredictionItem();
            item.setSkuData(new SKUData().setSku(activityIdWithScore.getActivityId()));
            item.setWeight(activityIdWithScore.getScore());
            predictionItems.add(item);
        });
        return ContainerSerializer.getCompactSerializer().listToCompactBytes(predictionItems).toBytes();
    }

    @Override
    public void collect(SegmentationInfo segmentationInfo, ArrayList<ActivityIdWithScore> shardingTop) {
        if (this.topActivityIds.containsKey(segmentationInfo)) {
            Queue<ActivityIdWithScore> mergedTop = this.topActivityIds.get(segmentationInfo);
            shardingTop.stream().forEach(activityIdWithScore -> {
                if (mergedTop.size() < segmentationInfo.getTopSize()) {
                    mergedTop.add(activityIdWithScore);
                } else {
                    if (mergedTop.peek().getScore() < activityIdWithScore.getScore()) {
                        mergedTop.poll();
                        mergedTop.add(activityIdWithScore);
                    }
                }
            });
        } else {
            PriorityQueue<ActivityIdWithScore> topQueue = new PriorityQueue<>(new ActivityAccumulateUVAndScore.TopNComparator());
            topQueue.addAll(shardingTop);
            this.topActivityIds.put(segmentationInfo, topQueue);
        }
    }

    @Override
    public void reduce(ResultCollection resultCollection) {
        try {
            //            String current = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss").concat(":");
            this.topActivityIds.forEach(((segmentationInfo, activityIdWithScores) -> {
                String key = segmentationInfo.toString();
                //                try {
                //                    dbService.save(this.getNamespace(), "statistics", key, current.concat(activityIdWithScores
                // .toString()), Duration
                //                            .ofMinutes(5));
                //                } catch (Exception e) {
                //                    e.printStackTrace();
                //                }
                MapResult<String, String> traceValue = new MapResult<>(this.getNamespace(), key, activityIdWithScores.toString());
                runtimeTrace.info("actBurstRecall", traceValue);
                //                if (recallOutput)
                resultCollection.addOutput(this.getName(), recallServiceType, recallSubType, key,
                        serializeRecallList(activityIdWithScores), windowSize);
            }));
        } finally {
            this.topActivityIds.clear();
        }
    }
}
