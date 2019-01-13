package com.jd.rec.nl.app.origin.modules.activityburst;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.activityburst.domain.EventFeature;
import com.jd.rec.nl.app.origin.modules.promotionalburst.dataprovider.UserProfileValue;
import com.jd.rec.nl.service.base.quartet.Schedule;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 判断是否累加uv
 *
 * @author zec
 * @date 2018/9/20
 */
public class ActivityCheckUV implements Updater, Schedule {

    private static final Logger LOGGER = getLogger(ActivityCheckUV.class);

    String name = "activityBurst";


    private List<UserProfileValue> userModels = new ArrayList<>();

    private Map<Long, Set<String>> activityIdUVCache = new ConcurrentHashMap<>();

    @Inject(optional = true)
    @Named("windowSize")
    private Duration windowDuration = Duration.ofMinutes(5);


    public ActivityCheckUV() {
    }

    @Inject
    public ActivityCheckUV(@Named("userModels") Map<String, UserProfileValue> userModels) {
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
            Long activityId = input.getEventContent().getActivityId();
       //     LOGGER.error("-----------------ActivityId->{}------------",activityId);
            if (uvCount(activityId, uid)) {
                EventFeature eventFeature = new EventFeature(uid, activityId);
                userModels.stream().forEach(userProfileValue -> {
                    Object userModelValue = null;
                    try {
                        userModelValue = userProfileValue.getValue(input, activityId);
                        if (userModelValue != null) {
                            eventFeature.addUserFeature(userProfileValue.getName(), userModelValue);
                        }
                    } catch (Exception e) {
                        LOGGER.debug(e.getMessage(), e);
                    }
                });
                resultCollection.addMapResult(this.getName(), activityId, eventFeature);
            }


    }

    /**
     * 判断是否属于新的uv
     *
     * @param activityId
     * @return
     */
    private boolean uvCount(Long activityId, String uid) {
        Set<String> uids = activityIdUVCache.get(activityId);
        if (uids == null) {
            uids = new HashSet<>();
            uids.add(uid);
            activityIdUVCache.put(activityId, uids);
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
        activityIdUVCache.clear();
    }
}
