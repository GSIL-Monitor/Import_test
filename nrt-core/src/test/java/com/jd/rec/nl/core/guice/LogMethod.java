package com.jd.rec.nl.core.guice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/5/28
 */
public class LogMethod implements MethodInterceptor {
    private static final Logger LOGGER = getLogger(LogMethod.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        LOGGER.info("run:" + invocation.getStaticPart().toString());
        return invocation.proceed();
    }
}
