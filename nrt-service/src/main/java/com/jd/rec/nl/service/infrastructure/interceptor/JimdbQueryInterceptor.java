package com.jd.rec.nl.service.infrastructure.interceptor;

import com.google.inject.Inject;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.service.infrastructure.Jimdb;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @ClassName JimdbQueryInterceptor
 * @Description TODO
 * @Author rocky
 * @Date 2018/11/30 下午3:58
 * @Version 1.0
 */
public class JimdbQueryInterceptor implements MethodInterceptor {
    @Inject
    private Jimdb jimdb;//线上jimdb

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        //todo
        Object proceed = invocation.proceed();
        Object[] arguments = invocation.getArguments();
        if (proceed == null) {
            proceed = jimdb.hGetValue((CLUSTER) arguments[0], (byte[]) arguments[1], (byte[]) arguments[2]);
        }
        return proceed;
    }

}
