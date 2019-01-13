package com.jd.rec.nl.app.origin.modules.popularshop.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wl
 * @date 2018/9/20
 */
public class EventFeature implements Serializable {

    String uid;

    Long shopId;

    Map<String, Object> userFeature = new HashMap<>();

    Map<String, Object> shopFeature = new HashMap<>();

    boolean shop1h = false;

    boolean shop5m = false;

    private String type;

    public EventFeature(String uid, Long shopId) {
        this.uid = uid;
        this.shopId = shopId;
    }

    public Map<String, Object> getUserFeature() {
        return userFeature;
    }

    public void setUserFeature(Map<String, Object> userFeature) {
        this.userFeature = userFeature;
    }

    public Map<String, Object> getShopFeature() {
        return shopFeature;
    }

    public void setShopFeature(Map<String, Object> shopFeature) {
        this.shopFeature = shopFeature;
    }

    public boolean isShop1h() {
        return shop1h;
    }

    public void setShop1h(boolean shop1h) {
        this.shop1h = shop1h;
    }

    public boolean isShop5m() {
        return shop5m;
    }

    public void setShop5m(boolean shop5m) {
        this.shop5m = shop5m;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public void addUserFeature(String featureName, Object value) {
        userFeature.put(featureName, value);
    }

    public void addShopFeature(String featureName, Object value) {
        shopFeature.put(featureName, value);
    }


}
