package com.jd.rec.nl.service.common.experiment;

import com.jd.rec.nl.core.pubsub.ApplicationEvent;
import com.typesafe.config.Config;

/**
 * @author linmx
 * @date 2018/11/5
 */
public class ParameterChangeEvent extends ApplicationEvent {

    String appName;

    long placementId;

    int expId;

    int version;

    private Config param;

    public ParameterChangeEvent(String source, String appName, long placementId, int expId, Config param, int version) {
        super(source);
        this.appName = appName;
        this.placementId = placementId;
        this.expId = expId;
        this.param = param;
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public long getPlacementId() {
        return placementId;
    }

    public void setPlacementId(long placementId) {
        this.placementId = placementId;
    }

    public int getExpId() {
        return expId;
    }

    public void setExpId(int expId) {
        this.expId = expId;
    }

    public Config getParam() {
        return param;
    }

    public void setParam(Config param) {
        this.param = param;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof ParameterChangeEvent)) {
            return false;
        }
        if (appName.equals(((ParameterChangeEvent) obj).appName) &&
                placementId == ((ParameterChangeEvent) obj).placementId && expId == ((ParameterChangeEvent) obj).expId
                && version <= ((ParameterChangeEvent) obj).version) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ParameterChangeEvent{" +
                "appName='" + appName + '\'' +
                ", placementId=" + placementId +
                ", expId=" + expId +
                ", version=" + version +
                ", param=" + param +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                '}';
    }
}
