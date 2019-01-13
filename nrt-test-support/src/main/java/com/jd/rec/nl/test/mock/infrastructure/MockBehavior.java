package com.jd.rec.nl.test.mock.infrastructure;

import com.google.inject.Singleton;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.service.infrastructure.Behavior;
import com.jd.rec.nl.service.modules.user.domain.BehaviorInfo;
import com.jd.rec.nl.service.modules.user.domain.RequiredBehavior;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author linmx
 * @date 2018/9/6
 */

@Mock(Behavior.class)
@Singleton
public class MockBehavior extends Behavior {
    @Override
    public Map<String, Set<BehaviorInfo>> getBehaviors(String uid, List<RequiredBehavior> queryBehaviors) {
        return queryBehaviors.stream().map(behaviorRequired -> behaviorRequired.getType().getFieldName())
                .collect(Collectors
                        .toMap(fieldName -> fieldName, fieldName -> new HashSet()));
    }
}
