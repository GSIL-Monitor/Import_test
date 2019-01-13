package com.jd.rec.nl.app.origin.modules.promotionalburst;

import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.EventFeature;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.SegmentationInfo;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.SkuWithScore;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.service.base.quartet.domain.MapResult;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.experiment.ExperimentVersionHolder;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import com.jd.rec.nl.service.modules.user.domain.UserProfile;
import com.jd.rec.nl.service.modules.user.service.UserService;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.math.stat.descriptive.rank.Median;
import org.junit.Before;
import org.junit.Test;
import p13.recsys.UserProfileAge;
import p13.recsys.UserProfileGender;
import p13.recsys.UserProfilePriceGrade;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author linmx
 * @date 2018/8/8
 */
public class CheckSkuUVTest {

    static CheckSkuUV checkSkuUV;

    static AccumulateUVAndScore accumulateUVAndScore;

    static ReduceBurstRecall reduceBurstRecall;

    @Before
    public void prepareDebug() {
        Map config = new HashMap();
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.DBProxy", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Clerk", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Predictor", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Jimdb", true);
        //        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.UnifiedOutput", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Zeus", true);
        config.put("quartet.intervalUnit", "10seconds");
        config.put("burst.params.intervalSize", "60seconds");
        com.typesafe.config.Config thread = ConfigFactory.parseMap(config);
        ConfigBase.setThreadConfig(thread);

        ExperimentVersionHolder experimentVersionHolder = new ExperimentVersionHolder();
        experimentVersionHolder.register(new CheckSkuUV());
        experimentVersionHolder.build();
        checkSkuUV = (CheckSkuUV) experimentVersionHolder.getNodeAllBranches("burst").get(0);

        ExperimentVersionHolder experimentVersionHolder1 = new ExperimentVersionHolder();
        experimentVersionHolder1.register(new AccumulateUVAndScore());
        experimentVersionHolder1.build();
        accumulateUVAndScore = (AccumulateUVAndScore) experimentVersionHolder1.getNodeAllBranches("burst").get(0);


        ExperimentVersionHolder experimentVersionHolder2 = new ExperimentVersionHolder();
        experimentVersionHolder2.register(new ReduceBurstRecall());
        experimentVersionHolder2.build();
        reduceBurstRecall = (ReduceBurstRecall) experimentVersionHolder2.getNodeAllBranches("burst").get(0);
    }

    public MapperContext prepareContext(String uid, long sku, int age, int gender, int cid3, int brandId, long... pws) {
        MapperContext input = new MapperContext();
        ImporterContext context = new ImporterContext();
        context.setBehaviorField(BehaviorField.CLICK);
        context.setUid(uid);
        context.setSkus(Collections.singleton(sku));
        input.setEventContent(context);
        UserProfile userProfile = new UserProfile();
        UserProfileAge.RecsysUserAgeProto.Builder builder = UserProfileAge.RecsysUserAgeProto.newBuilder();
        builder.setUid(uid);
        builder.setAgeId(age);
        userProfile.addProfile("recsys_p_uid_to_age_lr_4label", UserService.deserialize("recsys_p_uid_to_age_lr_4label",
                ByteBuffer.wrap(builder.build().toByteArray())));
        UserProfileGender.RecsysUserGenderProto.Builder builder1 = UserProfileGender.RecsysUserGenderProto.newBuilder();
        builder1.setUid(uid);
        builder1.setGender(gender);
        userProfile.addProfile("recsys_p_uid_to_gender_lr", UserService.deserialize("recsys_p_uid_to_gender_lr", ByteBuffer
                .wrap(builder1.build().toByteArray())));
        UserProfilePriceGrade.UnifiedUserProfilePriceGradeProto.Builder builder2 = UserProfilePriceGrade
                .UnifiedUserProfilePriceGradeProto.newBuilder();
        builder2.setUid(uid);
        builder2.setUserPrcWeight(1.01);
        userProfile.addProfile("recsys_up_price", UserService.deserialize("recsys_up_price", ByteBuffer.wrap
                (builder2.build().toByteArray())));
        input.setUserProfile(userProfile);

        Map<Long, ItemProfile> itemProfileMap = new HashMap<>();
        ItemProfile itemProfile = new ItemProfile();
        itemProfile.setSku(sku);
        itemProfile.setCid3(cid3);
        itemProfile.setBrandId(brandId);
        itemProfileMap.put(sku, itemProfile);
        input.setSkuProfiles(itemProfileMap);

        Map<String, Map<String, Map<Long, Float>>> miscInfo = new HashMap<>();
        Map<String, Map<Long, Float>> skuPW = new HashMap<>();
        Map<Long, Float> pwInfo = new HashMap<>();
        Arrays.stream(pws).forEach(pw -> pwInfo.put(pw, 2f));
        skuPW.put(String.valueOf(sku), pwInfo);
        miscInfo.put("rec_pi_productword", skuPW);
        input.setMisc(miscInfo);
        return input;
    }

    @Test
    public void runUVCheck() {
        MapperContext mapperContext = prepareContext("uid1", 111L, 1, 1, 222, 333, 44, 55);
        ResultCollection resultCollection = new ResultCollection();
        checkSkuUV.update(mapperContext, resultCollection);
        resultCollection.finish();
        MapResult mapResult = resultCollection.getNext();
        Serializable key;
        Serializable value;
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
            System.out.println(String.format("key:%s, value:%s", key, value));
            accumulateUVAndScore.collect((Long) mapResult.getKey(), (EventFeature) mapResult.getValue());
            mapResult = resultCollection.getNext();
        }
        mapperContext = prepareContext("uid2", 222L, 1, 1, 222, 333, 444, 555);
        resultCollection = new ResultCollection();
        checkSkuUV.update(mapperContext, resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
            System.out.println(String.format("key:%s, value:%s", key, value));
            accumulateUVAndScore.collect((Long) mapResult.getKey(), (EventFeature) mapResult.getValue());
            mapResult = resultCollection.getNext();
        }

        mapperContext = prepareContext("uid3", 111L, 2, 1, 222, 333, 44, 55);
        resultCollection = new ResultCollection();
        checkSkuUV.update(mapperContext, resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
            System.out.println(String.format("key:%s, value:%s", key, value));
            accumulateUVAndScore.collect((Long) mapResult.getKey(), (EventFeature) mapResult.getValue());
            mapResult = resultCollection.getNext();
        }


        resultCollection = new ResultCollection();
        accumulateUVAndScore.shuffle(resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
            System.out.println(String.format("key:%s, value:%s", key, value));
            if (!mapResult.isFin()) {
                reduceBurstRecall.collect((SegmentationInfo) key, (ArrayList<SkuWithScore>) value);
            }
            mapResult = resultCollection.getNext();
        }

        resultCollection = new ResultCollection();
        reduceBurstRecall.reduce(resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
            System.out.println(String.format("key:%s, value:%s", key, value));
            mapResult = resultCollection.getNext();
        }
    }

    @Test
    public void checkScore() throws IOException {
        File file = new File("/Users/linmx/Documents/jd/burstrecall.log");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        List<Double> scores = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            line = line.trim().substring(1, line.trim().length() - 1);
            Arrays.stream(line.split(",")).filter(ele -> ele.trim().startsWith("score")).forEach(score -> {
                scores.add(Double.parseDouble(score.split(":")[1].trim()));
            });
        }
        double[] arr = scores.stream().mapToDouble(d -> d).toArray();
        DoubleSummaryStatistics statistics = scores.stream().collect(DoubleSummaryStatistics::new,
                DoubleSummaryStatistics::accept, DoubleSummaryStatistics::combine);
        System.out.println(String.format("size:%d, max:%f, min:%f, mean:%f, median:%f", scores.size(), statistics.getMax(),
                statistics.getMin(), statistics.getAverage(), new Median().evaluate(arr)));
    }


}