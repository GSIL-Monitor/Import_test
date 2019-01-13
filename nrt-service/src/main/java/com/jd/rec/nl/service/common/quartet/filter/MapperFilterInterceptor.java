package com.jd.rec.nl.service.common.quartet.filter;

import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.quartet.processor.MapProcessor;

import java.lang.reflect.Method;

/**
 * @author linmx
 * @date 2018/10/10
 */
public class MapperFilterInterceptor extends MethodFilterInterceptor {

    private static final Method filterMethod;

    static {
        try {
            filterMethod = MapProcessor.class.getMethod("map", ImporterContext.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Method interceptMethod() {
        return filterMethod;
    }
}
