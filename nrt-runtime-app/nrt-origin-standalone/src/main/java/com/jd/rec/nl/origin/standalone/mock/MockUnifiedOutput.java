package com.jd.rec.nl.origin.standalone.mock;

import com.google.inject.Singleton;
import com.jd.feeder.pipeline.ServiceType;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.service.common.trace.MethodTraceInterceptor;
import com.jd.rec.nl.service.infrastructure.UnifiedOutput;
import com.jd.unifiedfeed.feedclient.util.SubTypePojo;
import com.jd.unifiedfeed.feedclient.util.SynProperty;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

//import recsys.prediction_service.NlBurstFeature;

/**
 * @author linmx
 * @date 2018/7/11
 */
@Mock(UnifiedOutput.class)
@Singleton
public class MockUnifiedOutput extends UnifiedOutput {

    private static final Logger LOGGER = getLogger(MockUnifiedOutput.class);

    public MockUnifiedOutput() {
        super(true);
    }

    @Override
    public boolean output(String key, String type, int subTypeId, Duration ttl, byte[] value) {
        Object content = new String(value);
        String viewKey = key;
        //        if (subTypeId == 2) {
        //            List<PredictionItem> predictionItems = ContainerSerializer.getCompactDeSerializer(value)
        // .fromCompactBytesToList
        //                    (PredictionItem.class);
        //
        //            List<SkuWithScore> finalContent = new ArrayList<>();
        //            predictionItems.forEach(predictionItem -> {
        //                SkuWithScore skuWithScore = new SkuWithScore(predictionItem.getSkuData().getSku(), predictionItem
        // .getWeight());
        //                ((ArrayList) finalContent).add(skuWithScore);
        //            });
        //            content = finalContent;
        //        } else if (subTypeId == 3) {
        //            NlBurstFeature.BurstFeatureList builder = null;
        //            try {
        //                builder = NlBurstFeature.BurstFeatureList.parseFrom(value);
        //                content = builder.getBurstFeatureInfoList();
        //            } catch (Exception e) {
        //                e.printStackTrace();
        //            }
        //            try {
        //                NlBurstFeature.BurstFeatureKey keyBuilder = NlBurstFeature.BurstFeatureKey.parseFrom(key.getBytes
        //                        ());
        //                viewKey = keyBuilder.toString().replaceAll("\\n", " ");
        //            } catch (Exception e) {
        //                e.printStackTrace();
        //            }
        //        } else {
        //            content = new String(value);
        //        }
        try {

            String protoDir = "proto/";
            SynProperty synProperty = new SynProperty();
            SubTypePojo subTypePojo = synProperty.getPojo(subTypeId);
            String subType = subTypePojo.getSubTypeName();
            File dir = new File(protoDir);
            if(!dir.exists()){
                dir.mkdir();
            }
            File protoFile = new File(protoDir + getProtoLogFileName(type, subType, "proto"));
            try {
                if(!protoFile.exists()){
                    protoFile.createNewFile();
                }
                Files.write(Paths.get(protoFile.toURI()), value);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(),e);
            }

            LOGGER.debug("shortName:{}, subType:{}, key:{}, expireTime:{}, value:{}", ServiceType.valueOf(type).name(),
                    subTypeId, viewKey, ttl == null ? "null" :String.valueOf(ttl.getSeconds()).concat(" seconds"),
                    MethodTraceInterceptor.traceObject(content, 0));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private static final DateFormat fileDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    private String getProtoLogFileName(String type, String tableName, String ext) {
        return String.format(
                "%s_%s_%s.%s",
                tableName,type, fileDateFormat.format(new Date()),ext
        );
    }
}
