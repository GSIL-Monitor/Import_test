package com.jd.rec.nl.connector.standalone.trace;

import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.jd.rec.nl.service.common.quartet.processor.MapProcessor;
import com.jd.rec.nl.service.common.quartet.processor.ParseProcessor;
import com.jd.rec.nl.service.common.quartet.processor.UpdateProcessor;
import com.jd.rec.nl.service.common.trace.MethodTraceInterceptor;
import com.jd.rec.nl.service.modules.db.service.DBService;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author linmx
 * @date 2018/6/28
 */
public class MethodLogInterceptor extends MethodTraceInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger("traceProcessorIO");

    @Override
    protected void output(StringBuilder sb) {
        LOGGER.error(sb.toString());
    }

    @Override
    protected boolean filter(MethodInvocation invocation, Object result, Throwable throwable) {
        return true;
    }

    @Override
    public Matcher<Class> matchedClass() {
        return Matchers.subclassesOf(ParseProcessor.class).or(Matchers.subclassesOf(MapProcessor.class)).or(Matchers
                .subclassesOf(UpdateProcessor.class)).or(Matchers.subclassesOf(DBService.class));
    }

    @Override
    public Matcher<Method> matchedMethod() {
        return null;
    }

}
