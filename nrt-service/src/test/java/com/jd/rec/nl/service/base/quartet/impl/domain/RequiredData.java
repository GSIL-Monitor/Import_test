package com.jd.rec.nl.service.base.quartet.impl.domain;

import java.util.List;

/**
 * @author linmx
 * @date 2018/6/13
 */
public class RequiredData {

    List<Behavior> behaviors;
    List<Related> related;
    boolean itemProfile;
    List<Misc> misc;

    public RequiredData() {
    }

    public boolean isItemProfile() {
        return itemProfile;
    }

    public void setItemProfile(boolean itemProfile) {
        this.itemProfile = itemProfile;
    }

    public List<Behavior> getBehaviors() {
        return behaviors;
    }

    public void setBehaviors(List<Behavior> behaviors) {
        this.behaviors = behaviors;
    }

    public List<Related> getRelated() {
        return related;
    }

    public void setRelated(List<Related> related) {
        this.related = related;
    }

    public List<Misc> getMisc() {
        return misc;
    }

    public void setMisc(List<Misc> misc) {
        this.misc = misc;
    }

}
