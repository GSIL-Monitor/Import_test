package com.jd.rec.nl.test.mock.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.service.infrastructure.DBProxy;
import com.jd.rec.nl.service.infrastructure.UnifiedOutput;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/7/11
 */
@Mock(UnifiedOutput.class)
@Singleton
public class MockUnifiedOutput extends UnifiedOutput {

    private static final Logger LOGGER = getLogger(MockUnifiedOutput.class);

    @Inject
    private DBProxy dbProxy;

    public MockUnifiedOutput() {
        super(true);
    }

    @Override
    public boolean output(String key, String type, int subTypeId, Duration ttl, byte[] value) {
        //        LOGGER.error("export: type->{} subType->{}", type, subTypeId);
        String viewKey = key;
        int content = -1;
        String current = DateFormatUtils.format(new Date(), "yyyyMMdd HHmmss");
        //        if (subTypeId == 2) {
        //            // 召回数据
        //            List<PredictionItem> predictionItems = ContainerSerializer.getCompactDeSerializer(value)
        // .fromCompactBytesToList
        //                    (PredictionItem.class);
        //
        //            //            List<SkuWithScore> finalContent = new ArrayList<>();
        //            //            predictionItems.forEach(predictionItem -> {
        //            //                SkuWithScore skuWithScore = new SkuWithScore(predictionItem.getSkuData().getActivityId(),
        // predictionItem
        //            //                        .getWeight());
        //            //                ((ArrayList) finalContent).add(skuWithScore);
        //            //            });
        //            //            content = finalContent;
        //            content = predictionItems.size();
        //        } else if (subTypeId == 3) {
        //            NlBurstFeature.BurstFeatureList builder = null;
        //            try {
        //                builder = NlBurstFeature.BurstFeatureList.parseFrom(value);
        //                content = builder.getBurstFeatureInfoList().size();
        //            } catch (Exception e) {
        //                e.printStackTrace();
        //            }
        //            try {
        //                NlBurstFeature.BurstFeatureKey keyBuilder = NlBurstFeature.BurstFeatureKey.parseFrom(key.getBytes
        //                        ());
        //                viewKey = keyBuilder.toString().replaceAll("\\n", " ");
        //            } catch (InvalidProtocolBufferException e) {
        //                e.printStackTrace();
        //            }
        //        }
        try {
            byte[] stored = dbProxy.getValue(CLUSTER.HT, "burstResult".concat("-").concat(String.valueOf(subTypeId)));
            if (stored == null) {
                stored = new byte[0];
            }

            StringBuilder sb = new StringBuilder("key:");
            sb.append(viewKey).append(", value num:").append(content).append(", time:").append(current);
            String newStr = new String(stored).concat("\n").concat(sb.toString());
            dbProxy.setValue(CLUSTER.HT, "burstResult".concat("-").concat(String.valueOf(subTypeId)), newStr.getBytes(),
                    Duration.ofMinutes(10));

            LOGGER.debug(sb.toString());
            //            LOGGER.error(" key:{}, expireTime:{}, value:{}", viewKey, expireTime, MethodTraceInterceptor
            // .traceObject(content, 0));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
