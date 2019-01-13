package com.jd.rec.nl.connector.storm.trace;

import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.jd.rec.nl.core.debug.DebugInfoThreadLocal;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.guice.interceptor.MethodMatcher;
import com.jd.rec.nl.service.common.trace.MethodTraceInterceptor;
import com.jd.rec.nl.service.infrastructure.DBProxy;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.service.modules.db.service.DBService;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/28
 */
public class DBTraceInterceptor extends MethodTraceInterceptor {

    private static final Logger LOGGER = getLogger(DBTraceInterceptor.class);

    private static final String NAMESPACE = "DBTrace-";

    public static final Duration tll = Duration.ofHours(1);

    @Override
    protected void output(StringBuilder sb) {
        DBProxy dbProxy = InjectorService.getCommonInjector().getInstance(DBProxy.class);
        String uid = DebugInfoThreadLocal.getCurrentUid();
        try {
            StringBuilder value = new StringBuilder();
            byte[] preValue = dbProxy.getValue(CLUSTER.HT, NAMESPACE.concat(uid));
            if (preValue != null && preValue.length < 1024 * 500) {
                value.append(new String(preValue)).append("\\n");
            }
            value.append(sb);
            LOGGER.error(value.toString());
            dbProxy.setValue(CLUSTER.HT, NAMESPACE.concat(uid), value.toString().getBytes(), tll);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    protected boolean filter(MethodInvocation invocation, Object result, Throwable throwable) {
        return DebugInfoThreadLocal.isTrace();
    }

    @Override
    public Matcher<Class> matchedClass() {
        return Matchers.subclassesOf(DBService.class);
    }

    @Override
    public Matcher<Method> matchedMethod() {
        return new MethodMatcher("save").or(new MethodMatcher("query"));
    }
}
