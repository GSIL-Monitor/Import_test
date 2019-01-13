package com.jd.rec.nl.app.origin.modules.pricegrade;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import org.slf4j.Logger;
import p13.recsys.UnifiedUserProfile2Layers;

import java.time.Duration;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class PriceGrade implements Updater {
    private static final Logger LOGGER = getLogger(PriceGrade.class);
    private String name = "priceGrade";
    private String prefix = "nl_nrtup_";
    private int priceIndex = 4;
    @Inject
    @Named("tableName")
    private String tableName;
    @Inject
    @Named("modelId")
    private int subType;
    @Inject
    @Named("liveTime")
    private Duration liveTime;

    @Override
    public void update(MapperContext mapperContext, ResultCollection resultCollection) {
        final String uid = mapperContext.getUid();
        //  LOGGER.error("uid->{}", uid);
        Map<Long, Double> skuToCid3Price = getSkuToCid3Price(mapperContext);
        //计算整体购买力
        Map<String, Double> totalToMedian = getTotalToMedia(skuToCid3Price);
        //计算cid3购买力
        Map<Integer, List<Double>> cid3ToPrices = getCid3ToPrice(mapperContext, skuToCid3Price);
        Map<String, Double> cid3ToMedian = getCid3ToMedia(cid3ToPrices);
        cid3ToMedian.putAll(totalToMedian);
        //序列化输出
        byte[] cid3value = serialize(uid, cid3ToMedian);
        resultCollection.addOutput(name, "USER_PROFILE_NRT", subType, prefix + uid, cid3value, liveTime);

        //            //LOG日志
        //            cid3ToMedian.forEach((k, v) -> {
        //                LOGGER.error("cid3ToMedia key->{},value->{}", k, v);
        //            });
        //            totalToMedian.forEach((k, v) -> {
        //                LOGGER.error("totalToMedian key->{},value->{} ", k, v);
        //            });
    }

    private Map<Long, Double> getSkuToCid3Price(MapperContext mapperContext) {
        Map<Long, Double> skuToCid3Price = new HashMap<>();
        if (mapperContext.getSkuExtraProfiles() != null) {
            mapperContext.getSkuExtraProfiles().get(tableName).forEach((sku, commenInfo) -> {
                double cid3PriceGrade = Double.parseDouble(commenInfo.getCustomAttribute().get(priceIndex));
                skuToCid3Price.putIfAbsent(sku, cid3PriceGrade);
            });
            return skuToCid3Price;
        } else {
            return Collections.emptyMap();
        }
    }

    private Map<String, Double> getTotalToMedia(Map<Long, Double> skuToCid3Price) {
        Map<String, Double> totalToMedian = new HashMap<>();
        if (skuToCid3Price.size() > 10) {//计算整体购买力
            List<Double> totalPriceList = new ArrayList<>(skuToCid3Price.values());
            Collections.sort(totalPriceList);
            //            System.out.println("totalPriceList：");
            //            totalPriceList.forEach(price->{
            //                System.out.print(price+", ");
            //            });
            int listSize = totalPriceList.size();
            if (listSize % 2 == 0) {
                totalToMedian.put("total", (totalPriceList.get(listSize / 2) + totalPriceList.get(listSize / 2 - 1)) / 2);
            } else
                totalToMedian.put("total", totalPriceList.get(listSize / 2));
        }
        return totalToMedian;
    }

    private Map<Integer, List<Double>> getCid3ToPrice(MapperContext mapperContext, Map<Long, Double> skuToCid3Price) {

        Map<Long, ItemProfile> profiles = mapperContext.getSkuProfiles();
        if (profiles == null || profiles.isEmpty()) {
            return new HashMap<>();
        }
        Map<Integer, List<Double>> cid3ToPrices = new HashMap<>();
        for (Map.Entry<Long, Double> entry : skuToCid3Price.entrySet()) {
            Long sku = entry.getKey();
            Double price = entry.getValue();
            if (!profiles.containsKey(sku)) {
                continue;
            }
            int cid3 = profiles.get(sku).getCid3();
            List<Double> cid3PriceList = cid3ToPrices.get(cid3);
            if (cid3PriceList == null) {
                List<Double> first = new ArrayList<>();
                first.add(price);
                cid3ToPrices.put(cid3, first);
            } else {
                cid3PriceList.add(price);
            }
        }
        //        System.out.println("cid3ToPrices：");
        //        cid3ToPrices.forEach((k,v)->{
        //            System.out.println("key: "+k);
        //            v.forEach(price->{
        //                System.out.print(price+", ");
        //            });
        //        });
        return cid3ToPrices;
    }

    private Map<String, Double> getCid3ToMedia(Map<Integer, List<Double>> cid3ToPrices) {
        Map<String, Double> cid3ToMedian = new HashMap<>();
        for (Map.Entry<Integer, List<Double>> entry : cid3ToPrices.entrySet()) {
            Integer key = entry.getKey();
            List<Double> value = entry.getValue();
            Collections.sort(value);
            int listSize = value.size();
            if (listSize % 2 == 0) {
                cid3ToMedian.put(key.toString(), (value.get(listSize / 2) + value.get(listSize / 2 - 1)) / 2);
            } else
                cid3ToMedian.put(key.toString(), value.get(listSize / 2));
        }
        return cid3ToMedian;
    }

    private byte[] serialize(String uid, Map<String, Double> result) {
        UnifiedUserProfile2Layers.UnifiedUserProfile2layersProto.Builder builder = UnifiedUserProfile2Layers
                .UnifiedUserProfile2layersProto.newBuilder();
        builder.setUid(uid);

        result.entrySet().stream().map(entry -> {
            UnifiedUserProfile2Layers.LevelTwo.Builder levelIIBuilder = UnifiedUserProfile2Layers.LevelTwo.newBuilder();
            levelIIBuilder.setProper(entry.getKey());
            levelIIBuilder.setValue(entry.getValue());
            return levelIIBuilder.build();
        }).forEach(levelTwo -> builder.addLevelTwo(levelTwo));
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
