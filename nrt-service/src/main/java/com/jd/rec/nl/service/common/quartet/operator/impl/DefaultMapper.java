package com.jd.rec.nl.service.common.quartet.operator.impl;

import com.google.inject.Inject;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.quartet.domain.MapperConf;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.domain.PredictorConf;
import com.jd.rec.nl.service.common.quartet.operator.RequiredMiscInfoKey;
import com.jd.rec.nl.service.infrastructure.domain.DataSource;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import com.jd.rec.nl.service.modules.item.service.ItemService;
import com.jd.rec.nl.service.modules.misc.service.MiscService;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import com.jd.rec.nl.service.modules.user.domain.BehaviorInfo;
import com.jd.rec.nl.service.modules.user.domain.RequiredBehavior;
import com.jd.rec.nl.service.modules.user.domain.UserProfile;
import com.jd.rec.nl.service.modules.user.service.UserService;
import com.jd.si.venus.core.CommenInfo;

import java.io.Serializable;
import java.util.*;

public class DefaultMapper implements Serializable {

    @Inject
    private UserService userService;

    @Inject
    private MiscService miscService;

    @Inject
    private ItemService itemService;


    public MapperContext process(final MapperConf conf, final ImporterContext importerContext) throws Exception {

        final MapperContext mapperContext = new MapperContext();
        mapperContext.setEventContent(importerContext);

        final String uid = importerContext.getUid();
        final long timestamp = importerContext.getTimestamp();
        final Set<Long> skus = importerContext.getSkus();
        final BehaviorField behaviorField = importerContext.getBehaviorField();

        final Set<Long> allSkusLong = new HashSet<>();
        final Set<String> allSkusString = new HashSet<>();
        if (skus != null && !skus.isEmpty()) {
            allSkusLong.addAll(skus);
            skus.forEach(i -> allSkusString.add(i.toString()));
        }

        queryUserBehaviors(conf, mapperContext, allSkusLong, allSkusString, uid, timestamp, behaviorField);

        queryUserProfiles(mapperContext, uid, conf.getModelList());

        querySimAndRelateSku(conf, mapperContext, allSkusLong, allSkusString);

        querySkuProfiles(conf, mapperContext, allSkusLong);

        queryItemExtraProfiles(conf.getRequiredExtraItemProfiles(), mapperContext);

        queryMiscInfo(conf, mapperContext);

        return mapperContext;
    }

    /**
     * 查询扩展的商品画像(从user model和selector中查询)
     *
     * @param requiredExtraProfiles
     * @param mapperContext
     */
    private void queryItemExtraProfiles(List<PredictorConf> requiredExtraProfiles, MapperContext mapperContext) {
        if (requiredExtraProfiles != null && requiredExtraProfiles.size() > 0) {
            Map<String, Map<Long, CommenInfo>> skuExtraProfiles = new HashMap<>();
            Map<String, Map<String, String>> materials = new HashMap<>();
            requiredExtraProfiles.stream().forEach(requiredExtraProfile -> {
                String tableName = requiredExtraProfile.getTableName();
                Set<String> keys =
                        RequiredMiscInfoKey.getProvider(requiredExtraProfile.getKey()).getKeys(mapperContext);
                if (keys == null || keys.isEmpty()) {
                    return;
                }
                if (requiredExtraProfile.getSource() == DataSource.userModel) {
                    skuExtraProfiles.put(tableName, itemService.getItemExtraProfiles(keys, tableName));
                } else if (requiredExtraProfile.getSource() == DataSource.selector) {
                    materials.put(tableName, itemService.getFromSelector(keys, tableName));
                }
            });
            mapperContext.setSkuExtraProfiles(skuExtraProfiles);
            mapperContext.setMaterial(materials);
        }
    }

    private void queryUserProfiles(MapperContext mapperContext, String uid, Set<String> modelList) {
        if (modelList != null && modelList.size() > 0) {
            mapperContext.setUserProfile(userService.getUserProfiles(uid, modelList));
        } else {
            mapperContext.setUserProfile(new UserProfile());
        }
    }

    private void queryUserBehaviors(final MapperConf conf, final MapperContext mapperContext,
                                    final Set<Long> allSkusLong, final Set<String> allSkusString,
                                    final String uid, final long timestamp, final BehaviorField behaviorField) {
        final List<RequiredBehavior> behaviorRequires = conf.getRequiredBehaviors();
        if (behaviorRequires != null && !behaviorRequires.isEmpty() && uid != null) {
            Map<BehaviorField, Set<BehaviorInfo>> behaviors =
                    userService.refreshAndQueryBehavior(uid, allSkusLong, timestamp,
                            behaviorField, behaviorRequires);
            if (!behaviors.isEmpty()) {
                for (Set<BehaviorInfo> userBehavior : behaviors.values()) {
                    for (BehaviorInfo behaviorInfo : userBehavior) {
                        long sku = behaviorInfo.getSku();
                        allSkusLong.add(sku);
                        allSkusString.add(String.valueOf(sku));
                    }
                }
                mapperContext.setBehaviors(behaviors);
            }
        }
    }

    private void querySimAndRelateSku(final MapperConf conf, final MapperContext mapperContext,
                                      final Set<Long> allSkusLong, final Set<String> allSkusString) {
        final List<PredictorConf> confStep2 = conf.getRequiredRelatedSkus();
        if (confStep2 != null && !confStep2.isEmpty() && !allSkusString.isEmpty()) {
            Map<String, Map<String, Map<Long, Float>>> tableToSkuToSkuAndWeight = new HashMap<>();
            for (PredictorConf predictorConf : confStep2) {
                final String tableName = predictorConf.getTableName();
                final int limit = predictorConf.getLimit();
                final Map<String, Map<Long, Float>> skuToSkuAndWeight = miscService.getRelatedWithoutNull(allSkusString,
                        tableName, limit);
                if (!skuToSkuAndWeight.isEmpty()) {
                    tableToSkuToSkuAndWeight.put(tableName, skuToSkuAndWeight);
                    for (Map<Long, Float> map : skuToSkuAndWeight.values()) {
                        Set<Long> relSkus = map.keySet();
                        allSkusLong.addAll(relSkus);
                        relSkus.forEach(i -> allSkusString.add(i.toString()));
                    }
                }
            }
            if (!tableToSkuToSkuAndWeight.isEmpty()) {
                mapperContext.setSimAndRelSkus(tableToSkuToSkuAndWeight);
            }
        }
    }

    private void querySkuProfiles(final MapperConf conf, final MapperContext mapperContext,
                                  final Set<Long> allSkusLong) throws
            Exception {
        final boolean confStep3 = conf.getItemProfileFlag();
        if (confStep3 && !allSkusLong.isEmpty()) {
            // 取回全部商品画像
            Map<Long, ItemProfile> skuProfile = itemService.getProfilesWithoutNull(allSkusLong);
            if (!skuProfile.isEmpty()) {
                mapperContext.setSkuProfiles(skuProfile);
            }
        }
    }

    private void queryMiscInfo(final MapperConf conf, final MapperContext mapperContext) throws IllegalAccessException,
            InstantiationException, ClassNotFoundException {
        final List<PredictorConf> requiredMiscInfo = conf.getRequiredMiscInfo();
        if (requiredMiscInfo != null && !requiredMiscInfo.isEmpty()) {
            Map<String, Map<String, Map<Long, Float>>> misc = new HashMap<>();
            for (PredictorConf predictorConf : requiredMiscInfo) {
                String tableName = predictorConf.getTableName();
                RequiredMiscInfoKey requiredMiscInfoKey = RequiredMiscInfoKey.getProvider(predictorConf.getKey());
                Set<String> keys = requiredMiscInfoKey.getKeys(mapperContext);
                if (keys == null || keys.isEmpty()) {
                    continue;
                }
                Map<String, Map<Long, Float>> predictors =
                        miscService.getRelatedWithoutNull(keys, tableName, predictorConf
                                .getLimit());
                misc.put(tableName, predictors);
            }
            mapperContext.setMisc(misc);
        }
    }
}
