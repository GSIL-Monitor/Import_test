package com.jd.rec.nl.service.common.quartet.domain;

import com.jd.rec.nl.core.domain.KeyValue;
import com.jd.rec.nl.core.domain.UserEvent;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;

import java.util.HashSet;
import java.util.Set;

public class ImporterContext implements KeyValue<String, ImporterContext>, UserEvent {

    private String uid;

    private long timestamp;

    /**
     * 来源
     */
    private String source;

    /**
     * 行为类型
     */
    private BehaviorField behaviorField;

    /**
     * 涉及的sku
     */
    private Set<Long> skus = new HashSet<>();

    /**
     * 涉及的内容id
     */
    private Set<String> contentIds = new HashSet<>();

    /**
     * 涉及的活动id
     */
    private long activityId;

    /**
     * 楼层id
     */
    private long levelId;

    /**
     * 店铺id
     */
    private Set<Long> shopIds;

    /**
     * 涉及的主题id
     */
    private Long themeId;

    /**
     * 用户pin
     */
    private String pin;

    /**
     * app搜索框词
     */
    private String keyWord;

    public String getKeyWord() {
        return keyWord;
    }

    public void setKeyWord(String keyWord) {
        this.keyWord = keyWord;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public Set<Long> getShopIds() {
        return shopIds;
    }

    public void setShopIds(Set<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public Long getThemeId() {
        return themeId;
    }

    public void setThemeId(Long themeId) {
        this.themeId = themeId;
    }

    public long getActivityId() {
        return activityId;
    }

    public void setActivityId(long activityId) {
        this.activityId = activityId;
    }

    public long getLevelId() {
        return levelId;
    }

    public void setLevelId(long levelId) {
        this.levelId = levelId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Set<Long> getSkus() {
        return skus;
    }

    public void setSkus(Set<Long> skus) {
        this.skus = skus;
    }

    public BehaviorField getBehaviorField() {
        return behaviorField;
    }

    public void setBehaviorField(BehaviorField behaviorField) {
        this.behaviorField = behaviorField;
    }

    public Set<String> getContentIds() {
        return contentIds;
    }

    public void setContentIds(Set<String> contentIds) {
        this.contentIds = contentIds;
    }

    @Override
    public String getKey() {
        return uid;
    }

    @Override
    public ImporterContext getValue() {
        return this;
    }

    @Override
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return "ImporterContext{" +
                "uid='" + uid + '\'' +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", behaviorField=" + behaviorField +
                ", skus=" + skus +
                ", contentIds=" + contentIds +
                ", activityId=" + activityId +
                ", levelId=" + levelId +
                '}';
    }
}
