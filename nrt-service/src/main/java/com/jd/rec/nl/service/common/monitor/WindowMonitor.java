package com.jd.rec.nl.service.common.monitor;

import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.rec.nl.service.common.monitor.domain.InvokeInformation;
import com.jd.rec.nl.service.common.trace.RuntimeTrace;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/8/20
 */
public class WindowMonitor implements MethodInterceptor {

    private static final Logger LOGGER = getLogger(WindowMonitor.class);

    RuntimeTrace runtimeTrace;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String executorName = ((Named) invocation.getThis()).getNamespace();
        String key = "windowShuffle_".concat(executorName);
        String taskName = ((Named) invocation.getThis()).getName();
        MonitorUtils.start(key);
        InvokeInformation information = new InvokeInformation(taskName);
        try {
            return invocation.proceed();
        } catch (Throwable e) {
            information.exception(e.getMessage());
            throw e;
        } finally {
            information.end();
            getRuntimeTrace().info("windowMonitor", information);
            MonitorUtils.end(key);
        }
    }

    private RuntimeTrace getRuntimeTrace() {
        if (runtimeTrace == null) {
            synchronized (this) {
                if (runtimeTrace == null) {
                    runtimeTrace = InjectorService.getCommonInjector().getInstance(RuntimeTrace.class);
                }
            }
        }
        return runtimeTrace;
    }

}
