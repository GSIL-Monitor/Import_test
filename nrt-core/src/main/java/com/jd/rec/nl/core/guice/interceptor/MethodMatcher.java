package com.jd.rec.nl.core.guice.interceptor;

import com.google.inject.matcher.AbstractMatcher;

import java.lang.reflect.Method;

/**
 * @author linmx
 * @date 2018/6/21
 */
public class MethodMatcher extends AbstractMatcher<Method> {

    String methodName;

    public MethodMatcher(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public boolean matches(Method method) {
        return method.getName().equals(methodName);
    }
}
