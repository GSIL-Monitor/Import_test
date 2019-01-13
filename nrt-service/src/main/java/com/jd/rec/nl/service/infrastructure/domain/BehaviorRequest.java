package com.jd.rec.nl.service.infrastructure.domain;

import com.jd.rec.nl.service.modules.user.domain.RequiredBehavior;
import com.jd.si.behavior.BaseRequest;
import com.jd.si.behavior.client.bulider.BehaviorRequestBuilder;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public class BehaviorRequest extends BehaviorRequestBuilder {

    private static final int BEHAVIOR_LIMIT = 200;

    private String uuid;

    private List<RequiredBehavior> behaviorTypes;

    public BehaviorRequest(String id, List<RequiredBehavior> queryBehaviors) {
        uuid = id;
        this.behaviorTypes = queryBehaviors;
    }

    @Override
    public String buildKey() {
        return uuid;
    }

    @Override
    public List<BaseRequest> buildBehaviorType() {
        final List<BaseRequest> types = new ArrayList<>();
        behaviorTypes.forEach(((behaviorRequired) -> {
            final BaseRequest type = new BaseRequest(behaviorRequired.getType().getFieldName());
            type.setOfflineData(true);
            if (behaviorRequired.getLimit() != null) {
                type.setLimit(behaviorRequired.getLimit());
            } else {
                type.setLimit(BEHAVIOR_LIMIT);
            }
            if (behaviorRequired.getPeriod() != null) {
                type.setTime(Clock.systemUTC().millis() - behaviorRequired.getPeriod().toMillis());
            }
            types.add(type);
        }));
        return types;
    }

    @Override
    public boolean buildDebug() {
        return true;
    }
}
