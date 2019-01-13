package com.jd.rec.nl.service.common.quartet.domain;

import com.google.common.collect.ImmutableMap;
import com.jd.rec.nl.core.domain.KeyValue;
import com.jd.rec.nl.core.domain.UserEvent;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import com.jd.rec.nl.service.modules.user.domain.BehaviorInfo;
import com.jd.rec.nl.service.modules.user.domain.UserProfile;
import com.jd.si.venus.core.CommenInfo;

import java.util.Map;
import java.util.Set;


/**
 * 尽量不要修改此类的方法api,因为会涉及到手工反射的实现MapperFieldReflector.java
 */
public class MapperContext implements KeyValue<String, MapperContext>, UserEvent {

    private ImporterContext eventContent;

    private Map<BehaviorField, Set<BehaviorInfo>> behaviors;

    private UserProfile userProfile;

    private Map<String, Map<String, Map<Long, Float>>> simAndRelSkus;

    private Map<Long, ItemProfile> skuProfiles;

    private Map<String, Map<Long, CommenInfo>> skuExtraProfiles;

    private Map<String, Map<String, Map<Long, Float>>> misc;

    private Map<String, Map<String, String>> material;

    public Map<String, Map<String, String>> getMaterial() {
        return material;
    }

    public void setMaterial(Map<String, Map<String, String>> material) {
        this.material = material;
    }

    public Map<String, Map<Long, CommenInfo>> getSkuExtraProfiles() {
        return skuExtraProfiles;
    }

    public void setSkuExtraProfiles(Map<String, Map<Long, CommenInfo>> skuExtraProfiles) {
        this.skuExtraProfiles = ImmutableMap.copyOf(skuExtraProfiles);
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public ImporterContext getEventContent() {
        return eventContent;
    }

    public void setEventContent(ImporterContext eventContent) {
        this.eventContent = eventContent;
    }

    public Map<BehaviorField, Set<BehaviorInfo>> getBehaviors() {
        return behaviors;
    }

    public void setBehaviors(Map<BehaviorField, Set<BehaviorInfo>> behaviors) {
        this.behaviors = ImmutableMap.copyOf(behaviors);
    }

    public Map<String, Map<String, Map<Long, Float>>> getSimAndRelSkus() {
        return simAndRelSkus;
    }

    public void setSimAndRelSkus(Map<String, Map<String, Map<Long, Float>>> simAndRelSkus) {
        this.simAndRelSkus = ImmutableMap.copyOf(simAndRelSkus);
    }

    public Map<Long, ItemProfile> getSkuProfiles() {
        return skuProfiles;
    }

    public void setSkuProfiles(Map<Long, ItemProfile> skuProfiles) {
        this.skuProfiles = ImmutableMap.copyOf(skuProfiles);
    }

    public Map<String, Map<String, Map<Long, Float>>> getMisc() {
        return misc;
    }

    public void setMisc(Map<String, Map<String, Map<Long, Float>>> misc) {
        this.misc = ImmutableMap.copyOf(misc);
    }

    @Override
    public String getKey() {
        return this.eventContent.getKey();
    }

    @Override
    public MapperContext getValue() {
        return this;
    }

    @Override
    public String getUid() {
        return this.eventContent.getUid();
    }

    @Override
    public String toString() {
        return "MapperContext{" +
                "eventContent=" + eventContent +
                ", behaviors=" + behaviors +
                ", userProfile=" + userProfile +
                ", simAndRelSkus=" + simAndRelSkus +
                ", skuProfiles=" + skuProfiles +
                ", skuExtraProfiles=" + skuExtraProfiles +
                ", misc=" + misc +
                '}';
    }
}
