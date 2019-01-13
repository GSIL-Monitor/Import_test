package com.jd.rec.nl.app.origin.modules.activityburst;

import org.junit.Test;
import recsys.prediction_service.UnifiedItemBurstFeatureOuterClass;

/**
 * @author linmx
 * @date 2018/9/29
 */
public class ActivityAccumulateUVAndScoreTest {

    @Test
    public void testSerial() {
        long activityId = 1111;
        long current = 1000000;
        double preHc = 0.11;
        double hc = 0.12;
        UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.Builder builder =
                UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.newBuilder();
        builder.setItemId(activityId);
        builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("time")
                .setValue(current / 1000));
        builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("hc")
                .setValue(preHc * 10000));
        builder.addFeature(UnifiedItemBurstFeatureOuterClass.Feature.newBuilder().setProper("uv")
                .setValue(hc));
        System.out.println(builder.build().toByteArray());
    }

}