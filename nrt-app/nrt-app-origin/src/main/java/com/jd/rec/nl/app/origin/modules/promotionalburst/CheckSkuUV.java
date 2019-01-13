package com.jd.rec.nl.app.origin.modules.promotionalburst;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.promotionalburst.dataprovider.UserProfileValue;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.ClickFeature;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.EventFeature;
import com.jd.rec.nl.service.base.quartet.Schedule;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.domain.MapperFieldReflector;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 判断是否累加uv
 *
 * @author linmx
 * @date 2018/7/31
 */
public class CheckSkuUV implements Updater, Schedule {

    private static final Logger LOGGER = getLogger(CheckSkuUV.class);

    String name = "burst";

    @Inject
    @Named("itemProfile")
    List<String> itemProfiles = new ArrayList<>();

    @Inject
    @Named("itemPredictProfile")
    Map<String, String> itemPredictProfiles = new HashMap<>();

    @Inject(optional = true)
    @Named("windowSize")
    private Duration windowDuration = Duration.ofMinutes(5);

    private List<UserProfileValue> userModels = new ArrayList<>();

    private Map<Long, Set<String>> skuUVCache = new ConcurrentHashMap<>();

    //    private ScheduledExecutorService scheduleCleaner;

    public CheckSkuUV() {
    }

    @Inject
    public CheckSkuUV(@Named("userModels") Map<String, UserProfileValue> userModels) {
        userModels.forEach((name, userProfileValue) -> {
            try {
                UserProfileValue profile = userProfileValue.getInstance();
                profile.setName(name);
                this.userModels.add(profile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        //        if (this.scheduleCleaner == null) {
        //            Duration defaultWindowSize = Duration.ofMinutes(5);
        //            scheduleCleaner = Executors.newSingleThreadScheduledExecutor();
        //            scheduleCleaner.scheduleAtFixedRate(() -> skuUVCache.clear(), defaultWindowSize.toMinutes(),
        // defaultWindowSize
        //                    .toMinutes(), TimeUnit.MINUTES);
        //        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void update(MapperContext input, ResultCollection resultCollection) {
        String uid = input.getUid();
        input.getEventContent().getSkus().stream().forEach(sku -> {
            if (uvCount(sku, uid)) {
                EventFeature eventFeature = new ClickFeature(uid, sku);
                userModels.stream().forEach(userProfileValue -> {
                    Object userModelValue = null;
                    try {
                        userModelValue = userProfileValue.getValue(input, sku);
                        if (userModelValue != null) {
                            eventFeature.addUserFeature(userProfileValue.getName(), userModelValue);
                        }
                    } catch (Exception e) {
                        LOGGER.debug(e.getMessage(), e);
                    }
                });
                itemProfiles.stream().forEach(profileField -> {
                    String fieldReg = "item:".concat(profileField);
                    try {
                        MapperFieldReflector fieldReflector = MapperFieldReflector.create(fieldReg);
                        Object itemProfile = fieldReflector.get(input, sku);
                        if (itemProfile != null)
                            eventFeature.addItemFeature(profileField, itemProfile);
                    } catch (Exception e) {
                        LOGGER.debug("{} doesn't have profile:{}", sku, profileField);
                    }
                });
                itemPredictProfiles.forEach((name, tableName) -> {
                    Map<Long, Float> mis = input.getMisc().get(tableName).get(String.valueOf(sku));
                    if (mis != null)
                        eventFeature.addItemFeature(name, new HashSet<>(mis.keySet()));
                });
                resultCollection.addMapResult(this.getName(), sku, eventFeature);
            }
        });
    }

    /**
     * 判断是否属于新的uv
     *
     * @param sku
     * @return
     */
    private boolean uvCount(Long sku, String uid) {
        Set<String> uids = skuUVCache.get(sku);
        if (uids == null) {
            uids = new HashSet<>();
            uids.add(uid);
            skuUVCache.put(sku, uids);
            return true;
        }
        if (!uids.contains(uid)) {
            uids.add(uid);
            return true;
        }
        return false;
    }


    @Override
    public int intervalSize() {
        return Long.valueOf(windowDuration.getSeconds() / defaultInterval.getSeconds()).intValue();
    }

    @Override
    public void trigger(ResultCollection resultCollection) {
        LOGGER.debug("clear uv cache![{}]", this.toString());
        skuUVCache.clear();
    }
}
