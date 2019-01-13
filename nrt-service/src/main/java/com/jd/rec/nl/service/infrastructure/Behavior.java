package com.jd.rec.nl.service.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.rec.nl.service.infrastructure.domain.BehaviorRequest;
import com.jd.rec.nl.service.modules.user.domain.BehaviorInfo;
import com.jd.rec.nl.service.modules.user.domain.RequiredBehavior;
import com.jd.si.behavior.BehaviorResponse;
import com.jd.si.behavior.ResultCode;
import com.jd.si.behavior.SKUData;
import com.jd.si.behavior.client.BaseBehavior;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/12
 */
@Singleton
public class Behavior implements BaseInfrastructure {

    private static final Logger LOGGER = getLogger(Behavior.class);

    @Inject
    @ENV("monitor.behavior_key")
    private String monitorKey;

    public Map<String, Set<BehaviorInfo>> getBehaviors(String uid, List<RequiredBehavior> queryBehaviors) {
        try {
            MonitorUtils.start(monitorKey);
            // 控制在system中打日志
            PrintStream err = System.err;
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            final BehaviorResponse resp = new BaseBehavior().getBehavior(
                    new BehaviorRequest(uid, queryBehaviors), CLIENT_ID);
            System.setErr(err);
            if (resp == null) {
                LOGGER.debug("Null response of behavior request for " + uid);
                return null;
            }
            if (resp.resultcode != ResultCode.OK) {
                switch (resp.resultcode) {
                    case BAD_PARAMETER:
                        LOGGER.debug("Bad parameter in behavior request for {}", uid);
                        break;
                    case USER_NOT_EXIST:
                        LOGGER.debug("Not exist user {}", uid);
                        break;
                    case NO_RESULT:
                        LOGGER.debug("No behavior result for {}", uid);
                        break;
                    default:
                        LOGGER.debug("Unknown reason request behavior for {}", uid);
                        break;
                }
                return null;
            }
            if (resp.data == null || resp.data.isEmpty()) {
                return null;
            }
            final Map<String, Set<BehaviorInfo>> behaviors = new HashMap<>();
            for (Map.Entry<String, com.jd.si.behavior.Behavior> reqBehavior : resp.data.entrySet()) {
                com.jd.si.behavior.Behavior behavior = reqBehavior.getValue();
                String behaviorType = reqBehavior.getKey();
                Set<BehaviorInfo> behaviorsByType = new LinkedHashSet<>();
                if (behavior.skuData == null || behavior.skuData.isEmpty()) {
                    continue;
                }
                for (final SKUData skuData : behavior.skuData) {
                    final BehaviorInfo behaviorElem;
                    try {
                        behaviorElem = new BehaviorInfo(skuData.sku, Long.parseLong(skuData.time), skuData.count);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    behaviorsByType.add(behaviorElem);
                }
                behaviors.put(behaviorType, behaviorsByType);
                //            Behavior behavior = resp.data.get(Const.CLICK_BEHAVIOR_NAME);
                //            if (behavior == null) {
                //                LOGGER.info("No click-behaviors for " + uid);
                //                return null;
                //            }
            }
            return behaviors;
        } catch (Exception e) {
            //            LOGGER.error(e.getMessage());
            MonitorUtils.error(monitorKey, e);
            throw new EnvironmentException("behavior", e);
        } finally {
            MonitorUtils.end(monitorKey);
        }
    }
}
