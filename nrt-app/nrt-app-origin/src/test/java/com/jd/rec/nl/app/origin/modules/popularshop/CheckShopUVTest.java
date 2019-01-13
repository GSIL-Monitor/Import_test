package com.jd.rec.nl.app.origin.modules.popularshop;

import com.jd.rec.nl.app.origin.modules.popularshop.domain.EventFeature;
import com.jd.rec.nl.app.origin.modules.popularshop.domain.SegmentationInfo;
import com.jd.rec.nl.app.origin.modules.popularshop.domain.ShopWithScore;
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
import org.junit.Before;
import org.junit.Test;
import p13.recsys.UserProfileAge;
import p13.recsys.UserProfileGender;
import p13.recsys.UserProfilePriceGrade;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.*;

public class CheckShopUVTest {
    static CheckShopUV checkShopUV;

    static AccumulateShopUVAndScore accumulateShopUVAndScore;

    static ReduceShopBurstRecall reduceShopBurstRecall;


    @Before
    public void prepareDebug() {
        Map config = new HashMap();
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.DBProxy", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Clerk", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Predictor", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Jimdb", true);
        //        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.UnifiedOutput", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Zeus", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.ILogKafka", true);
        config.put("quartet.windowUnit", "10seconds");
        config.put("burst.params.windowSize", "60seconds");
        com.typesafe.config.Config thread = ConfigFactory.parseMap(config);
        ConfigBase.setThreadConfig(thread);

        ExperimentVersionHolder experimentVersionHolder = new ExperimentVersionHolder();
        experimentVersionHolder.register(new CheckShopUV());
        experimentVersionHolder.build();
        checkShopUV = (CheckShopUV) experimentVersionHolder.getNodeAllBranches("popularshop").get(0);

        ExperimentVersionHolder experimentVersionHolder1 = new ExperimentVersionHolder();
        experimentVersionHolder1.register(new AccumulateShopUVAndScore());
        experimentVersionHolder1.build();
        accumulateShopUVAndScore = (AccumulateShopUVAndScore) experimentVersionHolder1.getNodeAllBranches("popularshop").get(0);


        ExperimentVersionHolder experimentVersionHolder2 = new ExperimentVersionHolder();
        experimentVersionHolder2.register(new ReduceShopBurstRecall());
        experimentVersionHolder2.build();
        reduceShopBurstRecall = (ReduceShopBurstRecall) experimentVersionHolder2.getNodeAllBranches("popularshop").get(0);
    }

    //模拟 MapperContext  中数据
    public MapperContext prepareContext(String uid, long sku, long shopId, int age, int gender, double userPrcWeight, long... pws) {
        MapperContext input = new MapperContext();
        ImporterContext context = new ImporterContext();

            context.setBehaviorField(BehaviorField.CLICK);

        context.setSkus(Collections.singleton(sku));
        context.setUid(uid);
        context.setShopIds(Collections.singleton(shopId));
        input.setEventContent(context);
        UserProfile userProfile = new UserProfile();

        //设置用户属性 uid   age
        UserProfileAge.RecsysUserAgeProto.Builder builder = UserProfileAge.RecsysUserAgeProto.newBuilder();
        builder.setUid(uid);
        builder.setAgeId(age);
        userProfile.addProfile("recsys_p_uid_to_age_lr_4label", UserService.deserialize("recsys_p_uid_to_age_lr_4label",
                ByteBuffer.wrap(builder.build().toByteArray())));

        //设置用户属性   uid   gender
        UserProfileGender.RecsysUserGenderProto.Builder builder1 = UserProfileGender.RecsysUserGenderProto.newBuilder();
        builder1.setUid(uid);
        builder1.setGender(gender);
        userProfile.addProfile("recsys_p_uid_to_gender_lr", UserService.deserialize("recsys_p_uid_to_gender_lr", ByteBuffer
                .wrap(builder1.build().toByteArray())));

        //设置属性 uid  用户综合购买的价格等级
        UserProfilePriceGrade.UnifiedUserProfilePriceGradeProto.Builder builder2 = UserProfilePriceGrade
                .UnifiedUserProfilePriceGradeProto.newBuilder();
        builder2.setUid(uid);
        //用户综合购买的价格等级
        builder2.setUserPrcWeight(userPrcWeight);
        //用户分类下的价格等级
        userProfile.addProfile("recsys_up_price", UserService.deserialize("recsys_up_price", ByteBuffer.wrap
                (builder2.build().toByteArray())));
        input.setUserProfile(userProfile);

        Map<Long, ItemProfile> itemProfileMap = new HashMap<>();
        ItemProfile itemProfile = new ItemProfile();
        itemProfile.setSku(sku);
        itemProfile.setShopId(shopId);
        itemProfileMap.put(sku, itemProfile);
        input.setSkuProfiles(itemProfileMap);

        Map<String, Map<String, Map<Long, Float>>> miscInfo = new HashMap<>();
        Map<String, Map<Long, Float>> skuPW = new HashMap<>();
        Map<Long, Float> pwInfo = new HashMap<>();
        Arrays.stream(pws).forEach(pw -> pwInfo.put(pw, 0.1f));
        skuPW.put(String.valueOf(shopId), pwInfo);
        miscInfo.put("r_shop_to_c1_sale", skuPW);
        input.setMisc(miscInfo);

        return input;
    }

    @Test
    public void update() {
        MapperContext mapperContext = prepareContext("uid1", 111L,111L, 1, 1, 0.8D,1,2);
        ResultCollection resultCollection = new ResultCollection();
        checkShopUV.update(mapperContext, resultCollection);
        resultCollection.finish();
        MapResult mapResult = resultCollection.getNext();
        Serializable key;
        Serializable value;
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
           // System.out.println(String.format("key:%s, value:%s", key, value));
            accumulateShopUVAndScore.collect((Long) mapResult.getKey(), (EventFeature) mapResult.getValue());
            mapResult = resultCollection.getNext();
        }
        mapperContext = prepareContext("uid2", 111L,111L, 2, 2, 0.2D,2,3);
        resultCollection = new ResultCollection();
        checkShopUV.update(mapperContext, resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
           // System.out.println(String.format("key:%s, value:%s", key, value));
            accumulateShopUVAndScore.collect((Long) mapResult.getKey(), (EventFeature) mapResult.getValue());
            mapResult = resultCollection.getNext();
        }

        mapperContext = prepareContext("uid3", 333L,111L, 3, 3, 0.6D,3,4);
        resultCollection = new ResultCollection();
        checkShopUV.update(mapperContext, resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
          //  System.out.println(String.format("key:%s, value:%s", key, value));
            accumulateShopUVAndScore.collect((Long) mapResult.getKey(), (EventFeature) mapResult.getValue());
            mapResult = resultCollection.getNext();
        }


        resultCollection = new ResultCollection();
        accumulateShopUVAndScore.shuffle(resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
            //System.out.println(String.format("key:%s, value:%s", key, value));
            if (!mapResult.isFin()) {
                reduceShopBurstRecall.collect((SegmentationInfo) key, (ArrayList<ShopWithScore>) value);
            }
            mapResult = resultCollection.getNext();
        }

        resultCollection = new ResultCollection();
        reduceShopBurstRecall.reduce(resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
           // System.out.println(String.format("key:%s, value:%s", key, value));
            mapResult = resultCollection.getNext();
        }
    }



    //为一样的时候
    @Test
    public void update3() {
        MapperContext mapperContext = prepareContext("uid1", 111L,111L, 1, 1, 0.8D,1);
        ResultCollection resultCollection = new ResultCollection();
        checkShopUV.update(mapperContext, resultCollection);
        resultCollection.finish();
        MapResult mapResult = resultCollection.getNext();
        Serializable key;
        Serializable value;
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
            // System.out.println(String.format("key:%s, value:%s", key, value));
            accumulateShopUVAndScore.collect((Long) mapResult.getKey(), (EventFeature) mapResult.getValue());
            //accumulateShopUVAndScore.shuffle(resultCollection);
            mapResult = resultCollection.getNext();
        }
         mapperContext = prepareContext("uid1", 111L,111L, 1, 1, 0.8D,2);
        resultCollection = new ResultCollection();
        checkShopUV.update(mapperContext, resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
            // System.out.println(String.format("key:%s, value:%s", key, value));
            accumulateShopUVAndScore.collect((Long) mapResult.getKey(), (EventFeature) mapResult.getValue());
            mapResult = resultCollection.getNext();
        }

        mapperContext = prepareContext("uid3", 111L,111L, 1, 1, 0.8D,3);
        resultCollection = new ResultCollection();
        checkShopUV.update(mapperContext, resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
            //  System.out.println(String.format("key:%s, value:%s", key, value));
            accumulateShopUVAndScore.collect((Long) mapResult.getKey(), (EventFeature) mapResult.getValue());
            mapResult = resultCollection.getNext();
        }


        resultCollection = new ResultCollection();
        accumulateShopUVAndScore.shuffle(resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
            //System.out.println(String.format("key:%s, value:%s", key, value));
            if (!mapResult.isFin()) {
                reduceShopBurstRecall.collect((SegmentationInfo) key, (ArrayList<ShopWithScore>) value);
            }
            mapResult = resultCollection.getNext();
        }

        resultCollection = new ResultCollection();
        reduceShopBurstRecall.reduce(resultCollection);
        resultCollection.finish();
        mapResult = resultCollection.getNext();
        while (mapResult != null) {
            key = mapResult.getKey();
            value = mapResult.getValue();
            // System.out.println(String.format("key:%s, value:%s", key, value));
            mapResult = resultCollection.getNext();
        }
    }



    @Test
    public void update2(){
        for(int i = 0 ;i<10;i++){
            MapperContext mapperContext = prepareContext("uid"+i,11l+i,1+i,1+i,1+i,0.1d+i,1+i);
            ResultCollection resultCollection = new ResultCollection();
            checkShopUV.update(mapperContext, resultCollection);
            MapResult mapResult = resultCollection.getNext();
            Serializable key;
            Serializable value;
            while (mapResult != null) {
                key = mapResult.getKey();
                value = mapResult.getValue();
                System.out.println(String.format("key:%s, value:%s", key, value));
                accumulateShopUVAndScore.collect((Long) mapResult.getKey(), (EventFeature) mapResult.getValue());
                accumulateShopUVAndScore.shuffle(resultCollection);
                mapResult = resultCollection.getNext();
            }
            mapResult = resultCollection.getNext();
            while (mapResult != null) {
                key = mapResult.getKey();
                value = mapResult.getValue();
                System.out.println(String.format("key:%s, value:%s", key, value));
                if (!mapResult.isFin()) {
                    reduceShopBurstRecall.collect((SegmentationInfo) key, (ArrayList<ShopWithScore>) value);
                    reduceShopBurstRecall.reduce(resultCollection);
                }
            }

        }
    }


}