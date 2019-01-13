package com.jd.rec.nl.app.origin.modules.rbcidblacklist;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.rbcidblacklist.domain.RbcidExposure;
import com.jd.rec.nl.app.origin.modules.rbcidblacklist.domain.RbcidOrder;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.operator.SimpleUpdater;
import com.jd.rec.nl.service.modules.db.service.DBService;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import com.jd.si.venus.core.CommenInfo;
import org.slf4j.Logger;
import p13.recsys.UnifiedUserProfile2Layers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/10/31
 */
public class RbcidBlacklistUpdater implements SimpleUpdater<List<RbcidExposure>> {
    private static final Logger LOGGER = getLogger(RbcidBlacklistUpdater.class);
    private String name = "rbcidBlacklist";

    @Inject
    @Named("modelId")
    private int subType;

    @Inject
    @Named("maxSize")
    private int maxSize;

    @Inject
    @Named("tableName")
    private String tableName;

    @Inject
    @Named("tableName_material")
    private String tableName_material;

    @Inject
    @Named("exposure_threshold")
    private int exposureThreshold;

    @Inject
    @Named("afterDays")
    private int afterDays;

    @Inject
    @Named("liveTime_Blacklist")
    private Duration liveTime_Blacklist;

    @Inject
    @Named("liveTime_Order")
    private Duration liveTime_Order;

    @Inject
    @Named("liveTime_Exposure")
    private Duration liveTime_Exposure;

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
    public String getKey(MapperContext context, List<RbcidExposure> ret) {
        return "nl_nrtup_".concat(context.getEventContent().getUid());
    }

    @Override
    public List<RbcidExposure> update(MapperContext context) throws Exception {

        if (context.getEventContent().getBehaviorField() == BehaviorField.ORDER) {
            order(context);
            return null;
        }

        List<String> rbcids = getRbcids(context);
        if (rbcids == null || rbcids.isEmpty()) {
            return null;
        }

        String uid = context.getUid();
        List<RbcidExposure> rbcidExposure = getCachedRbcidExposure(uid);

        long eventTime = context.getEventContent().getTimestamp();
        List<RbcidExposure> blacklist;
        if (context.getEventContent().getBehaviorField() == BehaviorField.CLICK || context.getEventContent().getBehaviorField() == BehaviorField.SEARCH) {
            // 点击和搜索行为
            blacklist = click(new HashSet<>(rbcids), rbcidExposure);
        } else {
            // 曝光行为
            blacklist = exposure(rbcids, eventTime, rbcidExposure);
        }
        dbService.save(this.getNamespace(), uid + "-rbcidExposure", rbcidExposure, liveTime_Exposure);
        return blacklist;
    }

    private List<RbcidExposure> getCachedRbcidExposure(String uid) throws Exception {
        List<RbcidExposure> rbcidExposure = dbService.query(this.getNamespace(), uid + "-rbcidExposure");
        if (rbcidExposure == null) {
            rbcidExposure = new ArrayList<>();
        }
        return rbcidExposure;
    }

    private List<String> getRbcids(MapperContext context) throws Exception {
        List<String> rbcids = new ArrayList<>();
        List<String> rbcidNew = getRbcidNew(context);
        String pin = context.getEventContent().getPin();
        Set<RbcidOrder> validRbcids = getValidRbcid(pin);
        dbService.save(this.getNamespace(), pin + "-rbcidOrder", validRbcids, liveTime_Order);
        Set<RbcidOrder> monitoredRbcids = daysFilter(validRbcids);
        if (rbcidNew.isEmpty() || monitoredRbcids == null || monitoredRbcids.isEmpty()) {
            return null;
        }

        Set<String> monitors = new HashSet<>();
        monitoredRbcids.forEach(rbcidOrder -> monitors.add(rbcidOrder.getRbcid()));
        rbcidNew.forEach(rbcid -> {
            if (monitors.contains(rbcid)) {
                rbcids.add(rbcid);
            }
        });
        return rbcids;
    }

    private List<String> getRbcidNew(MapperContext context) {
        List<String> rbcidNew = new ArrayList<>();
        if (context.getEventContent().getBehaviorField() == BehaviorField.SEARCH) {
            Set<String> re = new HashSet<>(context.getMaterial().get(tableName_material).values());
            re.forEach(s -> {
                if (s.contains(":") && s.length() > 2) {
                    rbcidNew.add(s.substring(s.indexOf(":") + 2, s.length() - 2));
                }
            });
        } else {
            for (Map.Entry<Long, CommenInfo> entry : context.getSkuExtraProfiles().get(tableName).entrySet()) {
                CommenInfo commenInfo = entry.getValue();
                rbcidNew.add(commenInfo.getCustomAttribute().get(0));
            }
        }
        return rbcidNew;
    }

    private Set<RbcidOrder> getValidRbcid(String pin) throws Exception {
        Set<RbcidOrder> rbcidsCached = dbService.query(this.getNamespace(), pin + "-rbcidOrder");
        if (rbcidsCached == null) {
            rbcidsCached = new HashSet<>();
        }

        return expiredFilter(rbcidsCached);
    }


    public Set<RbcidOrder> expiredFilter(Set<RbcidOrder> rbcidsCached) {
        Set<RbcidOrder> vaildRbcids = new HashSet<>();
        rbcidsCached.forEach(rbcidInfo -> {
            long Dvalue = System.currentTimeMillis() - rbcidInfo.getTimeStamp();
            if (Dvalue < (rbcidInfo.getPeriod() * 1000)) {
                vaildRbcids.add(rbcidInfo);
            }
        });
        return vaildRbcids;
    }

    public Set<RbcidOrder> daysFilter(Set<RbcidOrder> validRbcids) {
        Set<RbcidOrder> monitoredRbcids = new HashSet<>();
        validRbcids.forEach(validRbcid -> {
            long timeAfterOrder = validRbcid.getTimeStamp() + afterDays * 24 * 3600 * 1000; //订单中的rbcid两天后再开始监控
            if (timeAfterOrder < System.currentTimeMillis()) {
                monitoredRbcids.add(validRbcid);
            }
        });
        return monitoredRbcids;
    }

    private void order(MapperContext context) throws Exception {
        String pin = context.getEventContent().getPin();

        Set<RbcidOrder> validRbcids = getValidRbcid(pin);
        Map<String, Long> newRbcidToConst = new HashMap<>();
        Map<Long, CommenInfo> skuExPro = context.getSkuExtraProfiles().get(tableName);
        if (skuExPro == null) {
            return;
        }
        for (Map.Entry<Long, CommenInfo> entry : skuExPro.entrySet()) {
            CommenInfo commenInfo = entry.getValue();
            if (commenInfo != null && commenInfo.getCustomAttribute().size() > 1) {
                long periodDay;
                try {
                    periodDay = Long.valueOf(commenInfo.getCustomAttribute().get(1));
                } catch (NumberFormatException e) {
                    throw new InvalidDataException("CustomAttribute中的复购周期天数字段不合法");
                }
                newRbcidToConst.put(commenInfo.getCustomAttribute().get(0), periodDay * 24 * 3600);//天数转换成秒
            }
        }
        if (newRbcidToConst.isEmpty()) {
            return;
        }
        long timeStamp = context.getEventContent().getTimestamp();
        Set<String> rbcidsNew = newRbcidToConst.keySet();
        Set<RbcidOrder> currentMR = new CopyOnWriteArraySet<>(validRbcids);
        currentMR.forEach(rbcidInfo -> {
            String rbcidHistory = rbcidInfo.getRbcid();
            if (rbcidsNew.contains(rbcidHistory))
                currentMR.remove(rbcidInfo);
        });
        validRbcids.clear();
        validRbcids.addAll(currentMR);
        for (String rbcid : rbcidsNew) {
            RbcidOrder newRbcidInfo = new RbcidOrder(rbcid, timeStamp, newRbcidToConst.get(rbcid));
            validRbcids.add(newRbcidInfo);
        }
        dbService.save(this.getNamespace(), pin + "-rbcidOrder", validRbcids, liveTime_Order);
    }

    private List<RbcidExposure> exposure(List<String> rbcidsList, long exposureTime, List<RbcidExposure> rbcidExposures) {
        SortedSet<RbcidExposure> blacklist = new TreeSet<>();
        Set<String> rbcids = new HashSet<>(rbcidsList);
        Map<String, Integer> rbcidToNum = new HashMap<>();
        rbcidsList.forEach(rb -> {
            if (!rbcidToNum.containsKey(rb)) {
                rbcidToNum.put(rb, 1);
            } else {
                rbcidToNum.put(rb, rbcidToNum.get(rb) + 1);
            }
        });
        boolean changed = false;
        long now = System.currentTimeMillis();

        if (rbcidExposures.size() == 0) {
            for (String rbcid : rbcids) {
                RbcidExposure newExposure = new RbcidExposure(rbcid, exposureTime + liveTime_Exposure.toMillis(), rbcidToNum.get(rbcid));
                rbcidExposures.add(newExposure);

                if (newExposure.getExposureCount() >= this.exposureThreshold) {
                    newExposure.setBlacklist(true);
                    changed = true;
                    blacklist.add(newExposure);
                }
            }
        } else {
            // 对cid3曝光做累加
            int i = 0;
            while (i < rbcidExposures.size()) {
                RbcidExposure exposure = rbcidExposures.get(i);
                // 累加曝光
                if (rbcids.contains(exposure.getRbcid())) {
                    changed = true;
                    exposure.setExpireTime(exposureTime + liveTime_Exposure.toMillis());
                    if (!exposure.isBlacklist()) {
                        exposure.setExposureCount(exposure.getExposureCount() + rbcidToNum.get(exposure.getRbcid()));
                        if (exposure.getExposureCount() >= this.exposureThreshold) {
                            exposure.setBlacklist(true);
                            changed = true;
                        }
                    }
                    rbcids.remove(exposure.getRbcid());
                } else {
                    // 判断失效时间
                    if (exposure.getExpireTime() <= now) {
                        if (exposure.isBlacklist()) {
                            changed = true;
                        }
                        rbcidExposures.remove(i);
                        continue;
                    }
                }
                if (exposure.isBlacklist()) {
                    blacklist.add(exposure);
                }
                i++;
            }
            // 增加新的cid3曝光
            for (String rbcid : rbcids) {
                RbcidExposure newExposure = new RbcidExposure(rbcid, exposureTime + liveTime_Exposure.toMillis(), rbcidToNum.get(rbcid));
                rbcidExposures.add(newExposure);
                if (newExposure.getExposureCount() >= this.exposureThreshold) {
                    newExposure.setBlacklist(true);
                    changed = true;
                    blacklist.add(newExposure);
                }
            }
        }

        // 判断黑名单数量是否超限
        while (blacklist.size() > this.maxSize) {
            changed = true;
            rbcidExposures.remove(blacklist.first());
            blacklist.remove(blacklist.first());
        }
        if (changed) {
            return new ArrayList<>(blacklist);
        } else {
            return null;
        }
    }

    private List<RbcidExposure> click(Set<String> rbcids, List<RbcidExposure> rbcidBlacklists) {
        List<RbcidExposure> blacklist = new ArrayList<>();
        long now = System.currentTimeMillis();
        boolean changed = false;
        int i = 0;
        while (i < rbcidBlacklists.size()) {
            RbcidExposure rbcidExposure = rbcidBlacklists.get(i);
            if (rbcids.contains(rbcidExposure.getRbcid())) {
                // 点击删除
                rbcidBlacklists.remove(i);
                if (rbcidExposure.isBlacklist()) {
                    changed = true;
                }
                continue;
            } else if (rbcidExposure.getExpireTime() <= now) {
                // 过期删除
                rbcidBlacklists.remove(i);
                if (rbcidExposure.isBlacklist()) {
                    changed = true;
                }
                continue;
            } else {
                i++;
            }
            if (rbcidExposure.isBlacklist()) {
                blacklist.add(rbcidExposure);
            }
        }
        // 有变化才返回,不然仅更新本地
        if (changed) {
            return blacklist;
        } else {
            return null;
        }

    }

    @Override
    public byte[] serialize(MapperContext mapperContext, List<RbcidExposure> result) {
        UnifiedUserProfile2Layers.UnifiedUserProfile2layersProto.Builder builder = UnifiedUserProfile2Layers
                .UnifiedUserProfile2layersProto.newBuilder();
        builder.setUid(mapperContext.getEventContent().getUid());
        // 遍历topN的cid3得分
        result.stream().map(rbcidExposure -> {
            UnifiedUserProfile2Layers.LevelTwo.Builder levelIIBuilder = UnifiedUserProfile2Layers.LevelTwo.newBuilder();
            levelIIBuilder.setProper(rbcidExposure.getRbcid());
            levelIIBuilder.setValue(rbcidExposure.getExpireTime());
            return levelIIBuilder.build();
        }).forEach(levelTwo -> builder.addLevelTwo(levelTwo));
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

    @Override
    public Duration expireTime() {
        return liveTime_Blacklist;
    }
}
