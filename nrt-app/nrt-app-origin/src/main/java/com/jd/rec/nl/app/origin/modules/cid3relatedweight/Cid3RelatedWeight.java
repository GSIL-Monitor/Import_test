package com.jd.rec.nl.app.origin.modules.cid3relatedweight;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.cid3relatedweight.domain.Cid3Model;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.modules.db.service.DBService;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import com.jd.uds.grpc.BasicDataTypes;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 获取相关cid3权重计算
 *
 * @author wl
 * @date 2018/8/25
 */
public class Cid3RelatedWeight implements Updater {

    private static final Logger LOGGER = getLogger(Cid3RelatedWeight.class);
    private static final String relatedCid3ModelNameBytes = "nl_nrtrpp_related_cid3";
    /**
     * cid3RelMaps是
     * 查询cid3_relevance_by_scan（打包到资源里）文件得到的cid3以及相关cid3的权重
     */
    private static final Map<String, Map<String, Float>> cid3RelMaps = new ReadCid3RelMapUtil().readCid3RelMaps();
    private String name = "cid3realtedweight";
    @Inject
    @Named("sku_num_thershold")
    private int skuNumThershold;
    @Inject
    @Named("modelId")
    private int subType;
    @Inject
    @Named("liveTime")
    private Duration liveTime;
    @Inject
    @Named("maxSize")
    private int maxSize;
    @Inject
    @Named("rel_key_size")
    private int relKeySize;
    @Inject
    @Named("weight_ratio_scale")
    private int weightRatioScale;
    @Inject
    @Named("rel_size")
    private int relSize;
    @Inject
    private DBService dbService;
    private Map<String, Float> empty = new HashMap<>();

    @Override
    public void update(MapperContext mapperContext, ResultCollection resultCollection) {
        try {
            String uid = mapperContext.getUid();
            List<Cid3Model> cid3Models = dbService.query(getNamespace(), uid);
            if (cid3Models == null) {
                cid3Models = new ArrayList<>();
            }
            addSkuOrRemoveExpireTime(mapperContext, cid3Models);
            if (cid3Models.isEmpty()) {
                return;
            }
            LOGGER.warn("Update+cid3Models:" + cid3Models);
            dbService.save(getNamespace(), uid, cid3Models, liveTime);
            Set<Long> skus = new HashSet<>();
            for (int i = 0; i < cid3Models.size(); i++) {
                skus.add(Long.valueOf(cid3Models.get(i).getSku()));
            }
            //如果用户浏览的sku小于3个那就不计算品牌偏好
            if (skus.size() < skuNumThershold) {
                LOGGER.warn("SKU num does achieve the threshold for uuid:" + uid);
                return;
            }
            List<String> cid3s = new ArrayList<>();
            for (int i = 0; i < cid3Models.size(); i++) {
                String cid3 = cid3Models.get(i).getCid3() + "";
                cid3s.add(cid3);
            }
            if (cid3s.size() == 0) {
                LOGGER.warn("No valid brandId and cid3 in profiles");
                return;
            }
            //计算历史权重
            Map<String, Float> historyCid3Weights = computeHistoryWeights(cid3s);
            LOGGER.warn("historyCid3Weights" + historyCid3Weights);
            //计算相似权重
            Map<String, Float> relatedCid3Weights = computeRelatedCid3Weights(historyCid3Weights);
            LOGGER.warn("relatedCid3Weights:" + relatedCid3Weights);
            //最后输出结果
            Map<String, Float> relatedCid3Result = processResult(relatedCid3Weights, relSize);
            List<Map<String, Float>> finalWeights = new ArrayList<>();
            finalWeights.add(relatedCid3Result);

            final Map<String, byte[]> modelResultMap = new HashMap<>();
            byte[] cid3RelatedBytes = serializePropertiesPreferResult(uid, finalWeights.get(0));
            modelResultMap.put(relatedCid3ModelNameBytes, cid3RelatedBytes);
            for (Map.Entry<String, byte[]> entry : modelResultMap.entrySet()) {
                resultCollection.addOutput(name, "USER_PROFILE_NRT", subType, entry.getKey(), entry.getValue(), liveTime);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 如果没有就增加，如果有的话，就判断是否过期，过期就删除
     *
     * @param mapperContext 得到的输出数据
     * @param cid3Models    查询出来的结果
     */
    private void addSkuOrRemoveExpireTime(MapperContext mapperContext, List<Cid3Model> cid3Models) {
        Long current = System.currentTimeMillis();
        Long expireTime = mapperContext.getEventContent().getTimestamp();
        Set<Long> skus = mapperContext.getEventContent().getSkus();
        if (skus == null || skus.isEmpty()) {
            return;
        }
        if (mapperContext.getSkuProfiles() == null) {
            return;
        }
        Map<Long, ItemProfile> itemProfileMap = mapperContext.getSkuProfiles();
        if (cid3Models.isEmpty()) {
            for (Long sku : skus) {
                ItemProfile itemProfile = itemProfileMap.get(sku);
                if (itemProfile == null) {
                    continue;
                }
                int cid3 = itemProfile.getCid3();
                Cid3Model cid3Model = new Cid3Model(expireTime, sku, cid3);
                cid3Models.add(cid3Model);
            }
        } else {
            int index = 0;
            while (index < cid3Models.size()) {
                Cid3Model cid3Model1 = cid3Models.get(index);
                if (cid3Model1.getExpireTime() <= current - liveTime.toMillis()) {
                    cid3Models.remove(cid3Model1);
                    continue;
                }
                if (skus.contains(cid3Model1.getSku())) {
                    skus.remove(cid3Model1.getSku());
                    continue;
                }
                index++;
            }
            if (!skus.isEmpty()) {
                for (long sku : skus) {
                    ItemProfile itemProfile = itemProfileMap.get(sku);
                    if (itemProfile == null) {
                        continue;
                    }
                    int cid3 = itemProfile.getCid3();
                    Cid3Model cid3Model = new Cid3Model(expireTime, sku, cid3);
                    cid3Models.add(cid3Model);
                }
            }
        }
        cid3Models.sort((a1, a2) -> {
            if (a1.getExpireTime() > a2.getExpireTime()) {
                return 1;
            } else if (a1.getExpireTime() == a2.getExpireTime()) {
                return 0;
            } else {
                return -1;
            }
        });
        //保存的不能超过200个
        if (cid3Models.size() > maxSize) {
            final Iterator<Cid3Model> i = cid3Models.iterator();
            i.next();
            i.remove();
        }
    }

    /**
     * 计算历史(即：浏览过的)品牌的权重
     *
     * @param profileIds 个人id
     * @return 品牌id 权重
     */
    private Map<String, Float> computeHistoryWeights(final Collection<String> profileIds) {
        int total = profileIds.size();
        final Map<String, Float> weights = new HashMap<>();
        final float delta = (float) 1.0 / total;
        for (String profileId : profileIds) {
            Float weight = weights.get(profileId);
            if (weight == null) {
                weights.put(profileId, delta);
            } else {
                weights.put(profileId, weight + delta);
            }
        }
        //LOGGER.warn("computeHistoryWeights:"+total+","+weights);
        return weights;
    }

    /**
     * 计算相关的权重
     *
     * @param historyCid3WeightMap 历史权重
     * @return
     */
    private Map<String, Float> computeRelatedCid3Weights(Map<String, Float> historyCid3WeightMap) {
        final Map<String, Float> historyCid3Weights;
        if (relKeySize > 0) {
            historyCid3Weights = processResult(historyCid3WeightMap, relKeySize);
        } else {
            historyCid3Weights = historyCid3WeightMap;
        }
        Map<String, Map<String, Float>> relatedCid3s = getCid3RelMaps(cid3RelMaps, historyCid3Weights);
        return computeRelatedPropertyWeights(historyCid3Weights, relatedCid3s);
    }

    /**
     * 得到相关cid3的权重
     *
     * @param cid3RelMaps  查询cid3_relevance_by_scan（打包到资源里）文件得到的cid3以及相关cid3的权重
     * @param historyCid3s 历史cid3权重
     * @return
     */
    private Map<String, Map<String, Float>> getCid3RelMaps(final Map<String, Map<String, Float>> cid3RelMaps,
                                                           final Map<String, Float> historyCid3s) {
        final Map<String, Map<String, Float>> cid3RelatedMaps = new HashMap<>();
        for (Map.Entry<String, Float> historyCid3Set : historyCid3s.entrySet()) {
            String historyCid3Id = historyCid3Set.getKey();
            if (cid3RelMaps.containsKey(historyCid3Id)) {
                cid3RelatedMaps.put(historyCid3Id, cid3RelMaps.get(historyCid3Id));
            }
        }
        // LOGGER.warn("getCid3RelMaps:"+cid3RelatedMaps);
        return cid3RelatedMaps;
    }

    /**
     * 计算相关属性权重
     *
     * @param historyPropertyWeights 历史cid3权重
     * @param relatedProperty        相关cid3权重
     * @return
     */
    private Map<String, Float> computeRelatedPropertyWeights(final Map<String, Float> historyPropertyWeights,
                                                             final Map<String, Map<String, Float>> relatedProperty) {
        final Map<String, Float> weights = new HashMap<>();
        for (Map.Entry<String, Map<String, Float>> relatedPropertySet : relatedProperty.entrySet()) {
            Map<String, Float> relatedPropertyMap = relatedPropertySet.getValue();
            if (relatedPropertyMap == null || relatedPropertyMap.size() == 0) {
                continue;
            }
            Float historyPropertyWeight = historyPropertyWeights.get(relatedPropertySet.getKey());
            for (Map.Entry<String, Float> relatedPropertyInfo : relatedPropertyMap.entrySet()) {
                Float relatedPropertyWeight = relatedPropertyInfo.getValue();
                String relatedPropertyId = relatedPropertyInfo.getKey();
                if (relatedPropertyWeight <= 0) {
                    continue;
                }
                if (historyPropertyWeights.containsKey(relatedPropertyId)) {
                    continue;
                }
                Float finalWeight = historyPropertyWeight * relatedPropertyWeight;
                if (!weights.containsKey(relatedPropertyId)) {
                    weights.put(relatedPropertyId, finalWeight);
                } else {
                    Float lastWeight = weights.get(relatedPropertyId);
                    if (lastWeight < finalWeight) {
                        weights.put(relatedPropertyId, finalWeight);
                    }
                }
            }
        }
        return weights;
    }

    /**
     * 处理结果
     *
     * @param originalMap
     * @param size        (配置文件中设置)大小
     * @return 品牌id 权重
     * 包含降序排列，对权重的四舍五入
     */
    private Map<String, Float> processResult(final Map<String, Float> originalMap, final int size) {
        if (originalMap == null && originalMap.size() == 0) {
            return empty;
        }
        final Map<String, Float> resultMap = new LinkedHashMap<>();
        final List<Map.Entry<String, Float>> list = new ArrayList<>(originalMap.entrySet());
        int count = 0;
        Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {
            //降序排列
            @Override
            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        for (Map.Entry<String, Float> entry : list) {
            if (++count > size) {
                break;
            }
            BigDecimal b = new BigDecimal(entry.getValue());
            Float f = b.setScale(weightRatioScale, BigDecimal.ROUND_HALF_UP).floatValue();
            resultMap.put(entry.getKey(), f);
        }
        return resultMap;
    }

    private byte[] serializePropertiesPreferResult(String uid, Map<String, Float> value) {
        BasicDataTypes.BasicData.Builder builder = BasicDataTypes.BasicData.newBuilder();
        builder.setStrkey(uid);
        for (Map.Entry<String, Float> entry : value.entrySet()) {
            BasicDataTypes.StrPair.Builder strPairBuilder = BasicDataTypes.StrPair.newBuilder();
            strPairBuilder.setKey(entry.getKey());
            strPairBuilder.setValue(entry.getValue());
            builder.addStrPair(strPairBuilder.build());
        }
        return builder.build().toByteArray();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
