package com.jd.rec.nl.app.origin.modules.activityburst.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/8/3
 */
public class EventFeature implements Serializable {

    String uid;

    long activityId;

    Map<String, Object> userFeature = new HashMap<>();

    Map<String, Object> itemFeature = new HashMap<>();

    public EventFeature(String uid, long sku) {
        this.uid = uid;
        this.activityId = sku;
    }

    public Map<String, Object> getUserFeature() {
        return userFeature;
    }

    public void setUserFeature(Map<String, Object> userFeature) {
        this.userFeature = userFeature;
    }

    public Map<String, Object> getItemFeature() {
        return itemFeature;
    }

    public void setItemFeature(Map<String, Object> itemFeature) {
        this.itemFeature = itemFeature;
    }

    public void addUserFeature(String featureName, Object value) {
        userFeature.put(featureName, value);
    }

    public void addItemFeature(String featureName, Object value) {
        itemFeature.put(featureName, value);
    }
}
