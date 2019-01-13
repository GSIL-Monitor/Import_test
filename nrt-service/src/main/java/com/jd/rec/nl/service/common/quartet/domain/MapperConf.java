package com.jd.rec.nl.service.common.quartet.domain;

import com.jd.rec.nl.service.modules.user.domain.RequiredBehavior;

import java.io.Serializable;
import java.util.List;
import java.util.Set;


public class MapperConf implements Serializable {

    private List<RequiredBehavior> requiredBehaviors;

    private List<PredictorConf> requiredRelatedSkus;

    private boolean itemProfileFlag;

    private List<PredictorConf> requiredExtraItemProfiles;

    private List<PredictorConf> requiredMiscInfo;

    private Set<String> modelList;

    public List<PredictorConf> getRequiredExtraItemProfiles() {
        return requiredExtraItemProfiles;
    }

    public void setRequiredExtraItemProfiles(List<PredictorConf> requiredExtraItemProfiles) {
        this.requiredExtraItemProfiles = requiredExtraItemProfiles;
    }

    public Set<String> getModelList() {
        return modelList;
    }

    public void setModelList(Set<String> modelList) {
        this.modelList = modelList;
    }

    public List<RequiredBehavior> getRequiredBehaviors() {
        return requiredBehaviors;
    }

    public void setRequiredBehaviors(List<RequiredBehavior> requiredBehaviors) {
        this.requiredBehaviors = requiredBehaviors;
    }

    public List<PredictorConf> getRequiredRelatedSkus() {
        return requiredRelatedSkus;
    }

    public void setRequiredRelatedSkus(List<PredictorConf> relatedSkus) {
        this.requiredRelatedSkus = relatedSkus;
    }

    public boolean getItemProfileFlag() {
        return itemProfileFlag;
    }

    public void setIfGetItemProfile(boolean itemProfileFlag) {
        this.itemProfileFlag = itemProfileFlag;
    }

    public List<PredictorConf> getRequiredMiscInfo() {
        return requiredMiscInfo;
    }

    public void setRequiredMiscInfo(List<PredictorConf> miscInfo) {
        this.requiredMiscInfo = miscInfo;
    }
}


