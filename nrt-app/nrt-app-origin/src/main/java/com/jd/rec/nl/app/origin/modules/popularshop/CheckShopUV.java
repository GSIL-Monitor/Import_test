package com.jd.rec.nl.app.origin.modules.popularshop;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.popularshop.domain.EventFeature;
import com.jd.rec.nl.app.origin.modules.promotionalburst.dataprovider.UserProfileValue;
import com.jd.rec.nl.service.base.quartet.Schedule;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 判断是否累加uv
 *
 * @author wl
 * @date 2018/9/20
 */
public class CheckShopUV implements Updater, Schedule {

    private static final Logger LOGGER = getLogger(CheckShopUV.class);
    /**
     * 店铺uv
     */
    protected List<Map<Long, Set<String>>> shopUV1h = new ArrayList<>();
    protected Map<Long, Set<String>> shopUV5min = new HashMap<>();
    /**
     * 商详页uv
     */
    protected List<Map<Long, Set<String>>> shopSkuUV1h = new ArrayList<>();
    protected Map<Long, Set<String>> shopSkuUV5min = new HashMap<>();
    String name = "popularshop";
    @Inject
    @Named("itemPredictProfile")
    Map<String, String> itemPredictProfiles = new HashMap<>();
    private List<UserProfileValue> userModels = new ArrayList<>();
    @Inject(optional = true)
    @Named("windowSize")
    private Duration windowDuration = Duration.ofMinutes(5);

    public CheckShopUV() {
    }

    @Inject
    public CheckShopUV(@Named("userModels") Map<String, UserProfileValue> userModels) {
        userModels.forEach((name, userProfileValue) -> {
            try {
                UserProfileValue profile = userProfileValue.getInstance();
                profile.setName(name);
                this.userModels.add(profile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

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
        Set<Long> shopIds = new HashSet<>();

        //商详页
        if (input.getEventContent().getBehaviorField().equals(BehaviorField.CLICK)) {
            Set<Long> skus = input.getEventContent().getSkus();
            Map<Long, ItemProfile> itemProfileMap = input.getSkuProfiles();
            if (skus != null && !skus.isEmpty()) {
                if (itemProfileMap != null && !itemProfileMap.isEmpty()) {
                    for (long sku : skus) {
                        ItemProfile itemProfile = itemProfileMap.get(sku);
                        if (itemProfile == null) {
                            continue;
                        }
                        Long shopId = itemProfile.getShopId();
                        if (shopId != null && shopId != 0) {
                            shopIds.add(shopId);
                        }
                    }
                }
            }
        } else if (input.getEventContent().getBehaviorField().equals(BehaviorField.EXPOSURE)) {
            //店铺
            shopIds.addAll(input.getEventContent().getShopIds());
        }

        if (shopIds.isEmpty()) {
            return;
        }

        String type = input.getEventContent().getBehaviorField().toString();
        List<Map<Long, Set<String>>> shop1h = type.equals(BehaviorField.EXPOSURE.toString()) ? shopUV1h : shopSkuUV1h;
        Map<Long, Set<String>> shop5min = type.equals(BehaviorField.EXPOSURE.toString()) ? shopUV5min : shopSkuUV5min;
        //店铺或者商详页
        shopIds.forEach(shopId -> {
            //判断5分钟是否增加
            if (judgment5m(shop5min, shopId, uid)) {
                EventFeature eventFeature = new EventFeature(uid, shopId);
                userModels.stream().forEach(userProfileValue -> {
                    Object userModelValue = null;
                    try {
                        userModelValue = userProfileValue.getValue(input, shopId);
                        if (userModelValue != null) {
                            eventFeature.addUserFeature(userProfileValue.getName(), userModelValue);
                        }
                    } catch (Exception e) {
                        LOGGER.debug(e.getMessage(), e);
                    }
                });
                itemPredictProfiles.forEach((name, tableName) -> {
                    Map<Long, Float> mis = input.getMisc().get(tableName).get(String.valueOf(shopId));
                    if (mis != null) {
                        eventFeature.addShopFeature(name, new HashSet<>(mis.keySet()));
                    }
                });
                eventFeature.setType(type);
                eventFeature.setShopId(shopId);
                eventFeature.setShop5m(true);
                eventFeature.setShop1h(judgment1h(shop1h, shopId, uid));
                resultCollection.addMapResult(this.getName(), shopId, eventFeature);
            }
        });
    }


    /**
     * 如果超过1小时的话就删除掉一开始5分钟的数据
     * 做清除操作的时候，要判断
     *
     * @param shop1h
     */
    public void remove1h(List<Map<Long, Set<String>>> shop1h) {
        if (shop1h.size() == 12) {
            shop1h.remove(0);
        }
        shop1h.add(new HashMap<>());
    }

    /**
     * 判断5分钟是否增加
     *
     * @param shop5min
     * @param shopId
     * @param uid
     * @return
     */
    protected boolean judgment5m(Map<Long, Set<String>> shop5min, Long shopId, String uid) {
        Set<String> uids = shop5min.get(shopId);
        if (uids == null) {
            uids = new HashSet<>();
            uids.add(uid);
            shop5min.put(shopId, uids);
            return true;
        }
        if (!uids.contains(uid)) {
            uids.add(uid);
            return true;
        }
        return false;
    }


    /**
     * 判断1小时内是否增加
     *
     * @param shop1h
     * @param shopId
     * @param uid
     * @return
     */
    protected boolean judgment1h(List<Map<Long, Set<String>>> shop1h, Long shopId, String uid) {
        if (shop1h.size() == 0) {
            shop1h.add(new HashMap<>());
        }
        for (int i = 0; i < shop1h.size(); i++) {
            Map<Long, Set<String>> shopUV = shop1h.get(i);
            if (shopUV.containsKey(shopId) && shopUV.get(shopId).contains(uid)) {
                return false;
            }
        }
        Map<Long, Set<String>> shopUV = shop1h.get(shop1h.size() - 1);
        if (shopUV.containsKey(shopId)) {
            shopUV.get(shopId).add(uid);
        } else {
            Set<String> user = new HashSet<>();
            user.add(uid);
            shopUV.put(shopId, user);
        }
        return true;
    }


    @Override
    public int intervalSize() {
        return Long.valueOf(windowDuration.getSeconds() / defaultInterval.getSeconds()).intValue();
    }

    @Override
    public void trigger(ResultCollection resultCollection) {
        shopSkuUV5min.clear();
        remove1h(shopSkuUV1h);
        shopUV5min.clear();
        remove1h(shopUV1h);
    }

}
