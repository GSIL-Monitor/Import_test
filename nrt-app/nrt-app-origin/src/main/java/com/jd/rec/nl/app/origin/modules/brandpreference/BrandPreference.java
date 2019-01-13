package com.jd.rec.nl.app.origin.modules.brandpreference;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.brandpreference.domain.BrandSku;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.modules.db.service.DBService;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import org.slf4j.Logger;
import p13.nearline.NlNrtUp;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author wl
 * @date 2018/8/14
 */
public class BrandPreference implements Updater {


    private static final Logger LOGGER = getLogger(BrandPreference.class);
    private static final String historyModelNameBytes = "nl_nrtup_history_brand";
    private static final String similarModelNameBytes = "nl_nrtup_similar_brand";
    private static final String cid3HistoryModelNameBytes = "nl_nrtup_cid3_history_brand";
    private static final String cid3SimilarModelNameBytes = "nl_nrtup_cid3_similar_brand";
    private String name = "brandPreference";
    @Inject
    @Named("tableName")
    private String simCid3brandIdtableName;

    @Inject
    @Named("weight_ratio_scale")
    private int weightRatioScale;

    @Inject
    @Named("sim_key_size")
    private int simKeySize;

    @Inject
    @Named("history_size")
    private int historySize;

    @Inject
    @Named("sim_size")
    private int simSize;

    @Inject
    @Named("modelId")
    private int subType;
    @Inject
    @Named("liveTime")
    private Duration liveTime;

    @Inject
    @Named("sku_num_thershold")
    private int skuNumThershold;

    @Inject
    private DBService dbService;

    @Inject
    @Named("maxSize")
    private int maxSize;

    private Map<String, Float> emptyMap = new HashMap<>();

    @Override
    public void update(MapperContext mapperContext, ResultCollection resultCollection) {
        try {
            String uid = mapperContext.getUid();
            List<BrandSku> brandSkus = dbService.query(getNamespace(), uid);
            if (brandSkus == null) {
                brandSkus = new ArrayList<>();
            }
            addSkuOrRemoveExpireTime(mapperContext, brandSkus);

            //默认数据存储7天
            if (brandSkus == null || brandSkus.isEmpty()) {
                return;
            }
            dbService.save(this.getNamespace(), uid, brandSkus, liveTime);
            Set<Long> skus = new HashSet<>();
            for (int i = 0; i < brandSkus.size(); i++) {
                skus.add(Long.valueOf(brandSkus.get(i).getSku()));
            }

            //如果用户浏览的sku小于3个那就不计算品牌偏好
            if (skus.size() < skuNumThershold) {
                LOGGER.warn("SKU num does achieve the threshold for uuid:" + uid);
                return;
            }
            List<String> cid3BrandIds = new ArrayList<>();
            for (int i = 0; i < brandSkus.size(); i++) {
                String brandIds = brandSkus.get(i).getBrandId();
                cid3BrandIds.add(brandIds);
            }
            if (cid3BrandIds.isEmpty()) {
                LOGGER.warn("No valid brandId and cid3 in profiles");
                return;
            }
            LOGGER.warn("cid3BrandIds:" + cid3BrandIds.size() + ":" + cid3BrandIds);
            /**
             * 得到浏览过cid3品牌权重
             */
            Map<String, Float> historyCid3Weight = computeHistoryBrandsWeights(cid3BrandIds);
            /**
             * 相似品牌权重
             */
            Map<String, Float> similarCid3Weight = computeSimilarBrandsWeights(historyCid3Weight, brandSkus);
            //最后的结果,最终浏览的品牌默认保留5个
            Map<String, Float> historyCid3Result = processResult(historyCid3Weight, historySize);
            //最后的结果，相似品牌保留15个
            Map<String, Float> similarCid3Result = processResult(similarCid3Weight, simSize);
            //将所有的结果封装起来
            List<Map<String, Float>> finalWeights = new ArrayList<>();
            //因为浏览brandid和相似brandid偏好值是没有计算的，目前是不需要计算的，因此是为空。传入2个空值进去的
            finalWeights.add(emptyMap);
            finalWeights.add(emptyMap);
            finalWeights.add(historyCid3Result);
            finalWeights.add(similarCid3Result);
            final Map<String, byte[]> modelResultMap = new HashMap<>();
            byte[] historyBytes = serializeBrandPreferResult(uid, finalWeights.get(0));
            byte[] similarBytes = serializeBrandPreferResult(uid, finalWeights.get(1));
            byte[] cid3HistoryBytes = serializeBrandPreferResult(uid, finalWeights.get(2));
            byte[] cid3SimilarBytes = serializeBrandPreferResult(uid, finalWeights.get(3));

            modelResultMap.put(historyModelNameBytes, historyBytes);
            modelResultMap.put(similarModelNameBytes, similarBytes);
            modelResultMap.put(cid3HistoryModelNameBytes, cid3HistoryBytes);
            modelResultMap.put(cid3SimilarModelNameBytes, cid3SimilarBytes);
            for (Map.Entry<String, byte[]> entry : modelResultMap.entrySet()) {
                resultCollection.addOutput(name, "USER_PROFILE_NRT", subType, entry.getKey(), entry.getValue(), liveTime);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

    }


    /**
     * 如果为空，就增加新的，如果时间过期就删除掉
     *
     * @param mapperContext
     * @param brandSkus
     */
    private void addSkuOrRemoveExpireTime(MapperContext mapperContext, List<BrandSku> brandSkus) {
        Long current = System.currentTimeMillis();
        Long expireTime = mapperContext.getEventContent().getTimestamp();
        Set<Long> skus = mapperContext.getEventContent().getSkus();
        if (mapperContext.getMisc() == null) {
            return;
        }
        Map<String, Map<Long, Float>> misc = mapperContext.getMisc().get(simCid3brandIdtableName);
        if (misc == null || misc.isEmpty()) {
            return;
        }
        if (mapperContext.getSkuProfiles() == null) {
            return;
        }
        Map<Long, ItemProfile> profileMap = mapperContext.getSkuProfiles();
        if (profileMap.isEmpty()) {
            return;
        }
        if (brandSkus.isEmpty()) {
            for (long sku : skus) {
                ItemProfile itemProfile = profileMap.get(sku);
                if (itemProfile == null) {
                    continue;
                }
                String brandIds = itemProfile.getCid3() + ":" + itemProfile.getBrandId();
                BrandSku brandSku = new BrandSku();
                Map<Long, Float> relatedMap = misc.get(brandIds);
                if (relatedMap == null || relatedMap.size() == 0) {
                    continue;
                }
                for (Map.Entry<Long, Float> relateBrandInfo : relatedMap.entrySet()) {
                    Float relatedBrandWeight = relateBrandInfo.getValue();
                    if (relatedBrandWeight <= 0 || relateBrandInfo == null) {
                        continue;
                    }
                    //若通过cid3:brandid(String)返回Long型，需自行转换成cid3:brandid格式；
                    Long cid3BrandId = relateBrandInfo.getKey();
                    String relateBrandId = this.getKeyFromCid3BrandId(cid3BrandId);
                    if (brandIds.equals(relateBrandId)) {
                        continue;
                    }
                    brandSku = new BrandSku(expireTime, sku, brandIds, relatedBrandWeight);
                }
                brandSkus.add(brandSku);
            }
        } else {
            BrandSku brandSku2 = new BrandSku();
            int index = 0;
            while (index < brandSkus.size()) {
                BrandSku brandSku = brandSkus.get(index);
                if (brandSku.getExpireTime() <= current - liveTime.toMillis()) {
                    brandSkus.remove(brandSku);
                    continue;
                }
                if (skus.contains(brandSku.getSku())) {
                    skus.remove(brandSku.getSku());
                    continue;
                }
                index++;
            }
            if (!skus.isEmpty()) {
                for (long sku : skus) {
                    ItemProfile itemProfile = profileMap.get(sku);
                    if (itemProfile == null) {
                        continue;
                    }
                    String brandIds = itemProfile.getCid3() + ":" + itemProfile.getBrandId();
                    Map<Long, Float> relatedMap = misc.get(brandIds);
                    if (relatedMap == null || relatedMap.isEmpty()) {
                        continue;
                    }
                    for (Map.Entry<Long, Float> relateBrandInfo : relatedMap.entrySet()) {
                        Float relatedBrandWeight = relateBrandInfo.getValue();
                        if (relatedBrandWeight <= 0) {
                            continue;
                        }
                        Long cid3BrandId = relateBrandInfo.getKey();
                        String relateBrandId = this.getKeyFromCid3BrandId(cid3BrandId);
                        if (brandIds.equals(relateBrandId)) {
                            continue;
                        }
                        brandSku2 = new BrandSku(expireTime, sku, brandIds, relatedBrandWeight);
                    }
                    brandSkus.add(brandSku2);
                }
            }
        }
        brandSkus.sort((a1, a2) -> {
            if (a1.getExpireTime() > a2.getExpireTime()) {
                return 1;
            } else if (a1.getExpireTime() == a2.getExpireTime()) {
                return 0;
            } else {
                return -1;
            }
        });
        //保存的不能超过200个
        if (brandSkus.size() > maxSize) {
            final Iterator<BrandSku> i = brandSkus.iterator();
            i.next();
            i.remove();
        }
    }

    /**
     * 计算历史(即：浏览过的)品牌的权重
     *
     * @param brandIds 品牌id
     * @return 品牌id 权重
     */
    private Map<String, Float> computeHistoryBrandsWeights(final Collection<String> brandIds) {
        final int total = brandIds.size();
        LOGGER.warn("computeHistoryBrandsWeights+total:" + total);
        final Map<String, Float> weights = new HashMap<>();
        final float delta = (float) 1.0 / total;
        for (final String brandId : brandIds) {
            final Float weight = weights.get(brandId);
            if (weight == null) {
                weights.put(brandId, delta);
            } else {
                weights.put(brandId, weight + delta);
            }
        }
        LOGGER.warn("computeHistoryBrandsWeights+weights:" + weights);
        return weights;
    }

    /**
     * 计算相似的品牌权重
     *
     * @param historyBrandsWeights 历史品牌权重
     * @param brandSkus            历史数据
     * @return 品牌id 权重
     */
    private Map<String, Float> computeSimilarBrandsWeights(final Map<String, Float> historyBrandsWeights, List<BrandSku>
            brandSkus) {
        final Map<String, Float> weights = new HashMap<>();
        final Map<String, Float> simKeyMap;
        if (brandSkus == null && brandSkus.size() == 0) {
            return emptyMap;
        }
        if (historyBrandsWeights == null && historyBrandsWeights.size() == 0) {
            return emptyMap;
        }
        if (simKeySize > 0) {
            simKeyMap = processResult(historyBrandsWeights, simKeySize);
        } else {
            simKeyMap = historyBrandsWeights;
        }

        for (int i = 0; i < brandSkus.size(); i++) {
            //得到相似品牌的权重
            Float relatedBrandWeight = brandSkus.get(i).getWeight();
            //得到历史品牌权重，根据key(brandId)< relatedBrandSet.getKey()获取的是brandId >
            Float historyBrandWeight = simKeyMap.get(brandSkus.get(i).getBrandId());
            if (relatedBrandWeight <= 0) {
                continue;
            }
            //相似品牌的权重
            Float finalWeight = historyBrandWeight * relatedBrandWeight;
            if (!weights.containsKey(brandSkus.get(i).getBrandId())) {
                weights.put(brandSkus.get(i).getBrandId(), finalWeight);
            } else {
                Float lastWeight = weights.get(brandSkus.get(i).getBrandId());
                //以最大的为标准
                if (lastWeight < finalWeight) {
                    weights.put(brandSkus.get(i).getBrandId(), finalWeight);
                }
            }
        }
        LOGGER.warn("computeSimilarBrandsWeights:" + weights);
        return weights;
    }


    /**
     * 需要分别进行2次查询，若通过cid3:brandid(String)返回Long型，需自行转换成cid3:brandid格式；
     *
     * @param cid3BrandId
     * @return
     */
    private String getKeyFromCid3BrandId(Long cid3BrandId) {
        Long cid3 = cid3BrandId >> 32;
        Long brandId = cid3BrandId - (cid3 << 32);
        return cid3 + ":" + brandId;
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
            return emptyMap;
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

    /**
     * 以这样的形式做一个序列化的操作
     *
     * @param uid
     * @param value
     * @return
     */
    public byte[] serializeBrandPreferResult(String uid, Map<String, Float> value) {
        NlNrtUp.NrtUp.Builder builder = NlNrtUp.NrtUp.newBuilder();
        builder.setUid(uid);
        for (Map.Entry<String, Float> entry : value.entrySet()) {
            NlNrtUp.PropertyWeight.Builder propertyBuilder = NlNrtUp.PropertyWeight.newBuilder();
            propertyBuilder.setPropertyId(entry.getKey());
            propertyBuilder.setWeight(entry.getValue());
            builder.addProp(propertyBuilder.build());
        }
        NlNrtUp.NrtUp result = builder.build();
        return result.toByteArray();
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
