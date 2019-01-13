package com.jd.rec.nl.app.origin.modules.themeburst;

import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.SegmentationInfo;
import com.jd.rec.nl.app.origin.modules.themeburst.domain.ThemeEventFeature;
import com.jd.rec.nl.app.origin.modules.themeburst.domain.ThemeWithScore;
import com.jd.rec.nl.core.experiment.MultiVersionHolder;
import com.jd.rec.nl.service.base.quartet.domain.MapResult;
import com.jd.rec.nl.service.base.quartet.domain.OutputResult;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.experiment.ExperimentVersionHolder;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import com.jd.rec.nl.service.modules.user.domain.UserProfile;
import com.jd.rec.nl.service.modules.user.service.UserService;
import org.junit.Before;
import org.junit.Test;
import p13.recsys.UserProfileAge;
import p13.recsys.UserProfileGender;
import p13.recsys.UserProfilePriceGrade;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Queue;

/**
 * @author linmx
 * @date 2018/9/29
 */
public class ThemeUVCheckTest {

    private ThemeUVCheck uvCheck;

    private AccumulateThemeUV accumulateThemeUV;

    private ReduceRecall reduceRecall;

    public MapperContext prepareContext(String uid, BehaviorField behavior, long themeId, int age, int gender, int cid3,
                                        int brandId,
                                        long... pws) {
        MapperContext input = new MapperContext();
        ImporterContext context = new ImporterContext();
        context.setBehaviorField(behavior);
        context.setUid(uid);
        context.setThemeId(themeId);
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
        builder2.setUserPrcWeight(0.3);
        userProfile.addProfile("recsys_up_price", UserService.deserialize("recsys_up_price", ByteBuffer.wrap
                (builder2.build().toByteArray())));
        input.setUserProfile(userProfile);
        return input;
    }

    @Before
    public void before() {
        MultiVersionHolder versionHolder = new ExperimentVersionHolder();
        ThemeUVCheck check = new ThemeUVCheck();
        versionHolder.register(check);
        versionHolder.build();
        uvCheck = (ThemeUVCheck) versionHolder.getNodeAllBranches(check.getNamespace()).get(0);

        versionHolder = new ExperimentVersionHolder();
        AccumulateThemeUV accumulateThemeUV = new AccumulateThemeUV();
        versionHolder.register(accumulateThemeUV);
        versionHolder.build();
        this.accumulateThemeUV = (AccumulateThemeUV) versionHolder.getNodeAllBranches(accumulateThemeUV.getNamespace()).get(0);

        versionHolder = new ExperimentVersionHolder();
        ReduceRecall recall = new ReduceRecall();
        versionHolder.register(recall);
        versionHolder.build();
        this.reduceRecall = (ReduceRecall) versionHolder.getNodeAllBranches(recall.getNamespace()).get(0);
    }

    @Test
    public void update() throws Exception {
        ResultCollection collection = new ResultCollection();
        MapperContext context = prepareContext("111", BehaviorField.EXPOSURE, 56, 1, 2, 222, 222, 999, 888);
        uvCheck.update(context, collection);
        uvCheck.trigger(collection);
        uvCheck.update(prepareContext("111", BehaviorField.EXPOSURE, 56, 1, 2, 222, 222, 999, 888), collection);
        uvCheck.update(prepareContext("111", BehaviorField.EXPOSURE, 57, 1, 2, 222, 222, 999, 888), collection);
        uvCheck.update(prepareContext("111", BehaviorField.CLICK, 57, 1, 2, 222, 222, 999, 888), collection);
        collection.finish();
        MapResult result;
        while ((result = collection.getNext()) != null) {
            accumulateThemeUV.collect((Long) result.getKey(), (ThemeEventFeature) result.getValue());
        }
        uvCheck.trigger(collection);
        ResultCollection resultCollection = new ResultCollection();
        System.out.println("cycle 1");
        accumulateThemeUV.shuffle(resultCollection);
        resultCollection.finish();
        processResult(resultCollection);

        // 周期二
        System.out.println("cycle 2");
        resultCollection = new ResultCollection();
        uvCheck.trigger(collection);
        accumulateThemeUV.shuffle(resultCollection);
        resultCollection.finish();
        processResult(resultCollection);

        // 周期三
        System.out.println("cycle 3");
        resultCollection = new ResultCollection();
        uvCheck.trigger(collection);
        accumulateThemeUV.shuffle(resultCollection);
        resultCollection.finish();
        processResult(resultCollection);

        // 周期四
        System.out.println("cycle 4");
        resultCollection = new ResultCollection();
        uvCheck.trigger(collection);
        accumulateThemeUV.shuffle(resultCollection);
        resultCollection.finish();
        processResult(resultCollection);

        //周期五
        System.out.println("cycle 5");
        resultCollection = new ResultCollection();
        uvCheck.trigger(collection);
        accumulateThemeUV.shuffle(resultCollection);
        resultCollection.finish();
        processResult(resultCollection);

        //周期六
        System.out.println("cycle 6");
        resultCollection = new ResultCollection();
        uvCheck.trigger(collection);
        accumulateThemeUV.shuffle(resultCollection);
        resultCollection.finish();
        processResult(resultCollection);

        // 周期七
        System.out.println("cycle 7");
        resultCollection = new ResultCollection();
        uvCheck.trigger(collection);
        accumulateThemeUV.shuffle(resultCollection);
        resultCollection.finish();
        processResult(resultCollection);
    }

    private void processResult(ResultCollection resultCollection) {
        MapResult output;
        while ((output = resultCollection.getNext()) != null) {
            if (output instanceof OutputResult) {
                String ret = String.format("serviceType:%s, subType:%d, key:%s, value:%s", ((OutputResult) output).getType(),
                        ((OutputResult) output).getSubType(), output.getKey(), output.getValue());
                System.out.println(ret);
            } else {
                this.reduceRecall.collect((SegmentationInfo) output.getKey(), (HashMap<String, Queue<ThemeWithScore>>) output
                        .getValue());
            }
        }
        ResultCollection reduceResult = new ResultCollection();
        this.reduceRecall.reduce(reduceResult);
        reduceResult.finish();
        while ((output = resultCollection.getNext()) != null) {
            String ret = String.format("serviceType:%s, subType:%d, key:%s, value:%s", ((OutputResult) output).getType(),
                    ((OutputResult) output).getSubType(), output.getKey(), output.getValue());
            System.out.println(ret);
        }
    }

}