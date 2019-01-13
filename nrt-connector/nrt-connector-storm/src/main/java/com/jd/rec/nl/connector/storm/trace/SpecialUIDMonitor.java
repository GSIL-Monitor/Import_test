package com.jd.rec.nl.connector.storm.trace;

import com.jd.rec.nl.connector.storm.trace.domain.UserRuntimeTrace;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.core.domain.UserEvent;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.service.common.trace.RuntimeTrace;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/9/26
 */
public class SpecialUIDMonitor implements MethodInterceptor {

    private static final Logger LOGGER = getLogger(SpecialUIDMonitor.class);
    com.jd.rec.nl.service.common.trace.RuntimeTrace runtimeTrace;
    List<String> specialUID;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String uid = null;
        Object ret = null;
        Exception exception = null;
        String executorName = null;
        try {
            executorName = getExecutorName(invocation.getThis());
            if (invocation.getArguments() != null) {
                Object arg = invocation.getArguments()[0];
                if (arg instanceof UserEvent) {
                    uid = ((UserEvent) arg).getUid();
                }
            }
            ret = invocation.proceed();
            if (uid == null && ret != null && ret instanceof UserEvent) {
                uid = ((UserEvent) ret).getUid();
            }
            return ret;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            try {
                RuntimeTrace trace = getRuntimeTrace();
                if (uid != null && specialUID.contains(uid)) {
                    LOGGER.error("special uid:{}", uid);
                    UserRuntimeTrace runtimeTrace = new UserRuntimeTrace(uid, executorName,
                            invocation.getArguments() == null ? "" : Arrays.asList(invocation.getArguments()).toString(),
                            ret == null ? "" : ret.toString(), exception == null ? "" : exception.getMessage(),
                            DateFormatUtils.format(new Date(), "yyyyMMdd HHmmss"));
                    trace.info("specialUID", runtimeTrace);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private String getExecutorName(Object aThis) {
        if (aThis instanceof Named) {
            return ((Named) aThis).getName();
        } else {
            return aThis.getClass().getSimpleName();
        }
    }


    private com.jd.rec.nl.service.common.trace.RuntimeTrace getRuntimeTrace() {
        if (runtimeTrace == null) {
            synchronized (this) {
                if (runtimeTrace == null) {
                    runtimeTrace = InjectorService.getCommonInjector().getInstance(com.jd.rec.nl.service.common.trace
                            .RuntimeTrace.class);
                    if (ConfigBase.getSystemConfig().hasPath("trace.uid")) {
                        specialUID = ConfigBase.getSystemConfig().getStringList("trace.uid");
                    } else {
                        specialUID = new ArrayList<>();
                    }
                    LOGGER.error("{} for trace:{}", runtimeTrace.toString(), specialUID.toString());
                }
            }
        }
        return runtimeTrace;
    }

}
