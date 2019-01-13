package com.jd.rec.nl.service.common.monitor;

import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.rec.nl.service.base.quartet.domain.OutputResult;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author linmx
 * @date 2018/8/16
 */
public class ExporterMonitor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        OutputResult outputResult = (OutputResult) invocation.getArguments()[0];
        String monitorKey = outputResult.getType().concat("-").concat(String.valueOf(outputResult.getSubType()));
        try {
            MonitorUtils.start(monitorKey);
            return invocation.proceed();
        } catch (Exception e) {
            MonitorUtils.error(monitorKey, e);
            throw e;
        } finally {
            MonitorUtils.end(monitorKey);
        }
    }
}
