package com.jd.rec.nl.service.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.feeder.pipeline.ServiceType;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.unifiedfeed.feedclient.FeedClient;
import org.slf4j.Logger;

import java.time.Duration;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/7/11
 */
@Singleton
public class UnifiedOutput implements BaseInfrastructure {

    private static final Logger LOGGER = getLogger(UnifiedOutput.class);

    FeedClient feedClient;

    @Inject
    @ENV("monitor.unifiedOut_key")
    private String monitorKey;

    protected UnifiedOutput(boolean mock) {
    }

    @Inject
    public UnifiedOutput() {
        this.feedClient = new FeedClient();
    }

    public boolean output(String key, String type, int subTypeId, Duration ttl, byte[] value) {
        LOGGER.debug("save to unified feedClient:type->{}, subTypeId->{}, key->{}, expiretime->{}, value->{}", type, String
                .valueOf(subTypeId), key, ttl == null ? "null" : ttl.toString(), value);
        MonitorUtils.start(monitorKey);
        try {
            if (ServiceType.valueOf(type) == ServiceType.USER_PROFILE_NRT) {
                LOGGER.error("feeder can't support user profile:{}-{}", type, subTypeId);
                return false;
            }
            return feedClient.addMessage(key, ServiceType.valueOf(type), subTypeId, ttl == null ? 0 : ttl.getSeconds(), value);
        } catch (Exception e) {
            MonitorUtils.error(monitorKey, e);
            throw e;
        } finally {
            MonitorUtils.end(monitorKey);
        }
    }
}
