package com.jd.rec.nl.app.origin.modules.cid3blacklist;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.cid3blacklist.domain.Cid3Exposure;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.operator.SimpleUpdater;
import com.jd.rec.nl.service.modules.db.service.DBService;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;
import p13.nearline.NlEntranceCid3Blacklist;

import java.time.Duration;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/26
 */
/*
zec modified in 20180813
 */
public class Cid3BlacklistUpdater implements SimpleUpdater<List<Cid3Exposure>> {
    private static final Logger LOGGER = getLogger(Cid3BlacklistUpdater.class);
    private String name = "cid3Blacklist";

    @Inject
    @Named("modelId")
    private int subType;
    @Inject
    @Named("maxSize")
    private int maxSize;


    @Inject
    @Named("exposure_threshold")
    private int exposureThreshold;

    @Inject
    @Named("liveTime")
    private Duration liveTime;

    @Inject
    private DBService dbService;

    @Override
    public String getName() {
        return name;
    }


    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getKey(MapperContext context, List<Cid3Exposure> ret) {
        return "nl_nrtup_".concat(context.getEventContent().getUid());
    }

    @Override
    public List<Cid3Exposure> update(MapperContext context) throws Exception {
        //LOGGER.error("----------------cid3--update----- ");
        String uid = context.getUid();
        long eventTime = context.getEventContent().getTimestamp();
        // 行为的sku
        Set<Long> skus = context.getEventContent().getSkus();
        Set<Integer> cid3s = new HashSet<>();

        Map<Long, ItemProfile> profiles = context.getSkuProfiles();
        if (profiles == null || profiles.isEmpty()) {
            return null;
        }
        profiles.forEach((sku, profile) -> {
            if (skus.contains(sku) && profile.getCid3() > 0) {
                cid3s.add(profile.getCid3());
            }
        });
        if (cid3s.isEmpty()) {
            return null;
        }
        List<Cid3Exposure> cid3Exposure = dbService.query(this.getNamespace(), uid);

        if (cid3Exposure == null) {
            cid3Exposure = new ArrayList<>();
        }
        List<Cid3Exposure> blacklist;
        if (context.getEventContent().getBehaviorField() == BehaviorField.CLICK) {
            // 点击行为
            blacklist = click(cid3s, cid3Exposure);
        } else {
            // 曝光行为
            blacklist = exposure(cid3s, eventTime, cid3Exposure);
        }

        dbService.save(this.getNamespace(), uid, cid3Exposure, liveTime);
/*        if (blacklist != null) {
            LOGGER.error("----Update----blacklist!=null---");

        }
        if (blacklist == null) {
            LOGGER.error("----Update----blacklist==null---");
        }*/
        return blacklist;
    }


    private List<Cid3Exposure> exposure(Set<Integer> cid3s, long exposureTime, List<Cid3Exposure> cid3Exposures) {
        SortedSet<Cid3Exposure> blacklist = new TreeSet<>();
        boolean changed = false;
        long now = System.currentTimeMillis();

        if (cid3Exposures.size() == 0) {

            for (int cid3 : cid3s) {
                Cid3Exposure newExposure = new Cid3Exposure(cid3, exposureTime + liveTime.toMillis(), 1);
                cid3Exposures.add(newExposure);

                if (1 == this.exposureThreshold) {
                    newExposure.setBlacklist(true);
                    changed = true;
                    blacklist.add(newExposure);
                }
            }

        } else {
            // 对cid3曝光做累加
            int i = 0;
            while (i < cid3Exposures.size()) {
                Cid3Exposure exposure = cid3Exposures.get(i);
                // 累加曝光
                if (cid3s.contains(exposure.getCid3())) {
                    exposure.setExpireTime(exposureTime + liveTime.toMillis());
                    if (!exposure.isBlacklist()) {
                        exposure.setExposureCount(exposure.getExposureCount() + 1);
                        if (exposure.getExposureCount() == this.exposureThreshold) {
                            exposure.setBlacklist(true);
                            changed = true;
                        }
                    }
                    cid3s.remove(exposure.getCid3());
                } else {
                    // 判断失效时间
                    if (exposure.getExpireTime() <= now) {
                        if (exposure.isBlacklist()) {
                            changed = true;
                        }
                        cid3Exposures.remove(i);
                        continue;
                    }
                }
                if (exposure.isBlacklist()) {
                    blacklist.add(exposure);
                }
                i++;
            }
            // 增加新的cid3曝光
            for (int cid3 : cid3s) {
                cid3Exposures.add(new Cid3Exposure(cid3, exposureTime + liveTime.toMillis(), 1));
            }
        }

        // 判断黑名单数量是否超限
        while (blacklist.size() > this.maxSize) {
            changed = true;
            cid3Exposures.remove(blacklist.first());
            blacklist.remove(blacklist.first());
        }
        if (changed) {
            //  LOGGER.error("----Update----exposure---return blacklist!=null---");
            return new ArrayList<>(blacklist);
        } else {
            //  LOGGER.error("----Update----exposure---return blacklist== null-");
            return null;
        }
    }

    private List<Cid3Exposure> click(Set<Integer> cid3s, List<Cid3Exposure> cid3Blacklists) {
        List<Cid3Exposure> blacklist = new ArrayList<>();
        long now = System.currentTimeMillis();
        boolean changed = false;
        int i = 0;
        while (i < cid3Blacklists.size()) {
            Cid3Exposure cid3Exposure = cid3Blacklists.get(i);
            if (cid3s.contains(cid3Exposure.getCid3())) {
                // 点击删除
                cid3Blacklists.remove(i);
                if (cid3Exposure.isBlacklist()) {
                    changed = true;
                }
                continue;
            } else if (cid3Exposure.getExpireTime() <= now) {
                // 过期删除
                cid3Blacklists.remove(i);
                if (cid3Exposure.isBlacklist()) {
                    changed = true;
                }
                continue;
            } else {
                i++;
            }
            if (cid3Exposure.isBlacklist()) {
                blacklist.add(cid3Exposure);
            }
        }
        // 有变化才返回,不然仅更新本地
        if (changed) {
            //   LOGGER.error("----Update----click---return blacklist != null-");
            return blacklist;
        } else {
            //   LOGGER.error("----Update----click---return blacklist== null-");
            return null;
        }

    }

    @Override
    public byte[] serialize(MapperContext context, List<Cid3Exposure> result) {
        NlEntranceCid3Blacklist.BlackList.Builder builder = NlEntranceCid3Blacklist.BlackList.newBuilder();
        if (result.size() != 0) {
            for (Cid3Exposure exposure : result) {
                NlEntranceCid3Blacklist.Cid3Info.Builder cid3Builder = NlEntranceCid3Blacklist.Cid3Info.newBuilder();
                cid3Builder.setTime(exposure.getExpireTime());
                cid3Builder.setCid3(exposure.getCid3());
                builder.addCid3List(cid3Builder.build());
            }
        }
        return builder.build().toByteArray();
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
