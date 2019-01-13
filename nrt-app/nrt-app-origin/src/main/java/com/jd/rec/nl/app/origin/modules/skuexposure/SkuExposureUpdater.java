package com.jd.rec.nl.app.origin.modules.skuexposure;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.skuexposure.domain.ExposureAccumulator;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.operator.SimpleUpdater;
import com.jd.rec.nl.service.modules.db.service.DBService;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;
import p13.nearline.NlEntranceSkuExposure;

import java.time.Duration;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/14
 */
public class SkuExposureUpdater implements SimpleUpdater<List<ExposureAccumulator>> {

    private static final Logger LOGGER = getLogger(SkuExposureUpdater.class);

    private String name = "skuExposureAccumulate";

    @Inject
    @Named("modelId")
    private int subType;

    @Inject
    @Named("liveTime")
    private Duration liveTime;

    @Inject
    @Named("tableName")
    private String key;

    @Inject
    @Named("maxSize")
    private int maxSize;

    @Inject
    private DBService dbService;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public String getKey(MapperContext context, List<ExposureAccumulator> ret) {
        return "nl_nrtup_".concat(context.getEventContent().getUid());
    }

    @Override
    public List<ExposureAccumulator> update(MapperContext context) throws Exception {
        String uid = context.getUid();
        final Set<Long> skus = new HashSet<>();
        LOGGER.warn("SkuExposureUpdater_getSource():" + context.getEventContent().getSource());
        if ("appsdk_click.WorthBuyList_ProductExpo".equals(context.getEventContent().getSource())) {
            // 好货曝光流，
            if (context.getMisc() != null) {
                Map<String, Map<Long, Float>> contentToSku = context.getMisc().get(key);
                if (contentToSku != null) {
                    contentToSku.values().forEach(skuMap -> skus.addAll(skuMap.keySet()));
                }
            }
        } else {
            //4个推荐位的时候
            skus.addAll(context.getEventContent().getSkus());
        }

        if (skus.isEmpty()) {
            return null;
        }

        LOGGER.warn("SkuExposureUpdater_skus:" + skus);
        long timestamp = context.getEventContent().getTimestamp();
        long expireTime = timestamp + liveTime.toMillis();
        List<ExposureAccumulator> accumulators = dbService.query(getNamespace(), uid);
        // 更新曝光次数
        if (accumulators == null) {
            accumulators = new ArrayList<>();
        }
        boolean changed;
        LOGGER.warn("behaviorFied:" + context.getEventContent().getBehaviorField());
        if (context.getEventContent().getBehaviorField() == BehaviorField.CLICK) {
            //点击事件,做移除操作
            changed = removeWithClicked(skus, accumulators);
        } else {
            changed = true;
            addSkuExposure(skus, accumulators, expireTime);
        }
        if (changed) {
            // 存储
            dbService.save(getNamespace(), uid, accumulators, liveTime);
            return accumulators;
        } else {
            return null;
        }
    }

    @Override
    public byte[] serialize(MapperContext context, List<ExposureAccumulator> accumulators) {
        NlEntranceSkuExposure.SkuExposureList.Builder list = NlEntranceSkuExposure.SkuExposureList.newBuilder();
        if (accumulators.size() != 0) {
            for (ExposureAccumulator accumulator : accumulators) {
                NlEntranceSkuExposure.Exposure.Builder builder = NlEntranceSkuExposure.Exposure.newBuilder();
                builder.setCount(accumulator.getCount());
                builder.setTime(accumulator.getExpireTime());
                builder.setSku(accumulator.getSku());
                list.addExposures(builder);
            }
        }
        return list.build().toByteArray();
    }

    @Override
    public Duration expireTime() {
        return Duration.ofMinutes(1);
    }

    /**
     * 删除点击行为的sku曝光
     *
     * @param skus
     * @param accumulators
     * @return
     */
    private boolean removeWithClicked(Set<Long> skus, List<ExposureAccumulator> accumulators) {
        boolean changed = false;
        long current = System.currentTimeMillis();
        int index = 0;
        while (index < accumulators.size()) {
            ExposureAccumulator accumulator = accumulators.get(index);
            if (accumulator.getExpireTime() <= current) {
                accumulators.remove(accumulator);
                changed = true;
                continue;
            }
            if (skus.contains(accumulator.getSku())) {
                accumulators.remove(accumulator);
                changed = true;
                continue;
            }
            index++;
        }
        return changed;
    }

    /**
     * 增肌其他行为的曝光
     *
     * @param skus
     * @param accumulators
     * @param expireTime
     */
    public void addSkuExposure(Set<Long> skus, List<ExposureAccumulator> accumulators, Long expireTime) {
        Long current = System.currentTimeMillis();
        int index = 0;
        while (index < accumulators.size()) {
            ExposureAccumulator accumulator = accumulators.get(index);
            if (accumulator.getExpireTime() <= current) {
                accumulators.remove(accumulator);
                continue;
            }
            if (skus.contains(accumulator.getSku())) {
                accumulator.setExpireTime(expireTime);
                accumulator.setCount(accumulator.getCount() + 1);
                skus.remove(accumulator.getSku());
            }
            index++;
        }
        // 重新按时间排序
        accumulators.sort((a1, a2) -> {
            if (a1.getExpireTime() > a2.getExpireTime()) {
                return 1;
            } else if (a1.getExpireTime() == a2.getExpireTime()) {
                return 0;
            } else {
                return -1;
            }
        });

        // 将第一次曝光的商品增加在最后
        for (long sku : skus) {
            ExposureAccumulator exposureAccumulator = new ExposureAccumulator();
            exposureAccumulator.setExpireTime(expireTime);
            exposureAccumulator.setCount(1);
            exposureAccumulator.setSku(sku);
            accumulators.add(exposureAccumulator);
        }
        // 控制最大个数
        while (accumulators.size() > maxSize) {
            accumulators.remove(0);
        }
    }

    @Override
    public String getType() {
        return "USER_PROFILE_NRT";
    }

    @Override
    public int getSubType() {
        return subType;
    }
}
