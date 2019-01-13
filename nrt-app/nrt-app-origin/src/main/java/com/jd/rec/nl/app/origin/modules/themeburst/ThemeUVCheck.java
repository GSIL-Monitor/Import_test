package com.jd.rec.nl.app.origin.modules.themeburst;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.promotionalburst.dataprovider.UserProfileValue;
import com.jd.rec.nl.app.origin.modules.themeburst.domain.ThemeEventFeature;
import com.jd.rec.nl.service.base.quartet.Schedule;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/9/28
 */
public class ThemeUVCheck implements Updater, Schedule {

    private static final Logger LOGGER = getLogger(ThemeUVCheck.class);

    private String name = "themeBurst";

    private Duration windowSize = Duration.ofMinutes(10);

    private List<UserProfileValue> userModels = new ArrayList<>();

    /**
     * 缓存uv数据,结构为:
     * uid -> { 时长(滑动窗口大小的倍数) -> [ Set[themeId] ] }
     */
    private Map<String, Map<Integer, List<Set<Long>>>> uvCache = new HashMap<>();

    private List<Integer> featureList = new ArrayList<>();

    public ThemeUVCheck() {
    }

    @Inject
    public ThemeUVCheck(@Named("featureList") List<Duration> featureList, @Named("userModels") Map<String, UserProfileValue>
            userModels, @Named("windowSize") Optional<Duration> windowSize) {
        if (windowSize.isPresent())
            this.windowSize = windowSize.get();
        featureList.stream().forEach(feature -> this.featureList.add(Long.valueOf(feature.getSeconds() / this.windowSize
                .getSeconds()).intValue()));
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
    public void update(MapperContext mapperContext, ResultCollection resultCollection) {
        String uid = mapperContext.getUid();
        long themeId = mapperContext.getEventContent().getThemeId();
        BehaviorField behaviorField = mapperContext.getEventContent().getBehaviorField();
        ThemeEventFeature eventFeature = new ThemeEventFeature(themeId, behaviorField);
        if (behaviorField == BehaviorField.EXPOSURE) {
            // 如果是曝光,则处理为uv,判断uv是否增加
            if (uvCheck(uid, themeId, eventFeature)) {
                getUserInfo(mapperContext, eventFeature);
                resultCollection.addMapResult(this.getName(), themeId, eventFeature);
            }
        } else {
            // 接入的数据流限制了不是曝光就是点击,对于点击处理为pv
            getUserInfo(mapperContext, eventFeature);
            resultCollection.addMapResult(this.getName(), themeId, eventFeature);
        }
    }

    private void getUserInfo(MapperContext mapperContext, ThemeEventFeature eventFeature) {
        userModels.stream().forEach(userProfileValue -> {
            try {
                Object userModelValue = userProfileValue.getValue(mapperContext, 0);
                if (userModelValue != null) {
                    eventFeature.addUserFeature(userProfileValue.getName(), userModelValue);
                }
            } catch (Exception e) {
                LOGGER.debug(e.getMessage(), e);
            }
        });
    }

    private boolean uvCheck(String uid, long themeId, ThemeEventFeature eventFeature) {
        if (!uvCache.containsKey(uid)) {
            Map<Integer, List<Set<Long>>> userUV = new HashMap<>();
            uvCache.put(uid, userUV);
            this.featureList.stream().forEach(period -> {
                List<Set<Long>> periodInfo = new ArrayList<>();
                userUV.put(period, periodInfo);
                periodInfo.add(new HashSet<>(Collections.singleton(themeId)));
                eventFeature.uvAdd(period);
            });
            return true;
        }
        // 当前uid的点击情况
        Map<Integer, List<Set<Long>>> userUV = this.uvCache.get(uid);
        boolean change = false;
        for (Map.Entry<Integer, List<Set<Long>>> entry : userUV.entrySet()) {
            boolean uvAdd = true;
            // feature的一个维度
            Integer periodId = entry.getKey();
            // 对应的分片信息
            List<Set<Long>> periodInfo = entry.getValue();
            if (periodInfo.size() > 0) {
                // 取最后一个时间片数据,也就是当前窗口的数据
                for (Set<Long> themeIds : periodInfo) {
                    if (themeIds.contains(themeId)) {
                        uvAdd = false;
                        break;
                    }
                }
                if (uvAdd) {
                    periodInfo.get(periodInfo.size() - 1).add(themeId);
                }
            } else {
                periodInfo.add(new HashSet<>(Collections.singleton(themeId)));
            }
            if (uvAdd) {
                change = true;
                eventFeature.uvAdd(periodId);
            }
        }

        return change;
    }

    @Override
    public int intervalSize() {
        return Long.valueOf(windowSize.getSeconds() / defaultInterval.getSeconds()).intValue();
    }

    @Override
    public void trigger(ResultCollection resultCollection) {
        // 清理过期的数据
        LOGGER.debug("clear cache:{}", this.uvCache);
        for (String uid : new HashSet<>(this.uvCache.keySet())) {
            Map<Integer, List<Set<Long>>> userUV = this.uvCache.get(uid);
            int emptyPeriods = 0;
            for (int period : new HashSet<>(userUV.keySet())) {
                List<Set<Long>> periodList = userUV.get(period);
                if (periodList.size() == period) {
                    // 把第一个过期数据给删了
                    periodList.remove(0);
                }
                periodList.add(new HashSet<>());
                int total = periodList.stream().map(uv -> uv.size()).mapToInt(Integer::intValue).sum();
                if (total == 0) {
                    // 说明已经没有点击了
                    emptyPeriods++;
                }
            }
            if (emptyPeriods == this.featureList.size()) {
                // 说明这个用户没有点击了
                this.uvCache.remove(uid);
            }
        }
    }
}
