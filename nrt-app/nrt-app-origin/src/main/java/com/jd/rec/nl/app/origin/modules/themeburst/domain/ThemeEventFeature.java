package com.jd.rec.nl.app.origin.modules.themeburst.domain;

import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.EventFeature;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;

import java.util.ArrayList;
import java.util.List;

/**
 * @author linmx
 * @date 2018/9/28
 */
public class ThemeEventFeature extends EventFeature {

    long themeId;

    BehaviorField behaviorField;

    /**
     * uv增加的时间段
     */
    List<Integer> uvCondition = new ArrayList<>();

    public ThemeEventFeature(long themeId, BehaviorField behaviorField) {
        this.themeId = themeId;
        this.behaviorField = behaviorField;
    }

    public BehaviorField getBehaviorField() {
        return behaviorField;
    }

    public void uvAdd(int periodFlag) {
        uvCondition.add(periodFlag);
    }

    public List<Integer> getUvCondition() {
        return uvCondition;
    }
}
