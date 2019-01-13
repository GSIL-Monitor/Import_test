package com.jd.rec.nl.app.origin.modules.themeburst;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.DimensionValue;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.SegmentationInfo;
import com.jd.rec.nl.app.origin.modules.themeburst.domain.ThemeWithScore;
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
 * 汇总各分片的召回,并计算topN
 *
 * @author linmx
 * @date 2018/7/24
 */
public class ReduceRecall implements Reducer<SegmentationInfo, HashMap<String, Queue<ThemeWithScore>>> {

    private static final Logger LOGGER = getLogger(ReduceRecall.class);

    @Inject
    public RuntimeTrace runtimeTrace;

    String name = "themeBurst";

    @Inject(optional = true)
    @Named("windowSize")
    private Duration windowSize = Duration.ofMinutes(10);

    @Inject
    @Named("recallServiceType")
    private String recallServiceType;

    @Inject
    @Named("recallSubType")
    private int recallSubType;

    @Inject(optional = true)
    @Named("recallOutput")
    private boolean recallOutput = true;


    private Map<SegmentationInfo, HashMap<String, Queue<ThemeWithScore>>> topN = new HashMap<>();

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
    private byte[] serializeRecallList(Collection<ThemeWithScore> recallList) {
        List<PredictionItem> predictionItems = new ArrayList<>();
        recallList.forEach(score -> {
            PredictionItem item = new PredictionItem();
            item.setSkuData(new SKUData().setSku(score.getThemeId()));
            item.setWeight(score.getScore());
            predictionItems.add(item);
        });
        return ContainerSerializer.getCompactSerializer().listToCompactBytes(predictionItems).toBytes();
    }

    @Override
    public void collect(SegmentationInfo segmentationInfo, HashMap<String, Queue<ThemeWithScore>> shardingTop) {
        if (this.topN.containsKey(segmentationInfo)) {
            Map<String, Queue<ThemeWithScore>> storedVersionsTop = this.topN.get(segmentationInfo);
            shardingTop.forEach((version, versionTop) -> {
                if (storedVersionsTop.containsKey(version)) {
                    Queue<ThemeWithScore> storedTop = storedVersionsTop.get(version);
                    versionTop.stream().forEach(themeWithScore -> {
                        if (storedTop.size() < segmentationInfo.getTopSize()) {
                            storedTop.add(themeWithScore);
                        } else {
                            if (storedTop.peek().getScore() < themeWithScore.getScore()) {
                                storedTop.poll();
                                storedTop.add(themeWithScore);
                            }
                        }
                    });
                } else {
                    storedVersionsTop.put(version, versionTop);
                }
            });
        } else {
            HashMap<String, Queue<ThemeWithScore>> newTop = new HashMap();
            shardingTop.forEach((version, versionTop) -> {
                PriorityQueue<ThemeWithScore> topQueue = new PriorityQueue<>(new AccumulateThemeUV.TopNComparator());
                topQueue.addAll(versionTop);
                newTop.put(version, topQueue);
            });
            this.topN.put(segmentationInfo, newTop);
        }
    }

    @Override
    public void reduce(ResultCollection resultCollection) {
        try {
            //            String current = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss").concat(":");
            this.topN.forEach(((segmentationInfo, versionsTop) -> {
                String key = segmentationInfo.toString();
                //                try {
                //                    dbService.save(this.getNamespace(), "statistics", key, current.concat(skuWithScores
                // .toString()), Duration
                //                            .ofMinutes(5));
                //                } catch (Exception e) {
                //                    e.printStackTrace();
                //                }
                versionsTop.forEach((version, tops) -> {
                    String recallKey = key.concat(String.valueOf(DimensionValue.SEG_SEP)).concat(version);
                    MapResult<String, String> traceValue = new MapResult<>(this.getNamespace(), recallKey, tops.toString());
                    LOGGER.debug(traceValue.toString());
                    runtimeTrace.info("themeBurstRecall", traceValue);
                    //                if (recallOutput)
                    resultCollection.addOutput(this.getName(), recallServiceType, recallSubType, recallKey,
                            serializeRecallList(tops), windowSize.plus(windowSize));
                });
            }));
        } finally {
            this.topN.clear();
        }
    }
}
