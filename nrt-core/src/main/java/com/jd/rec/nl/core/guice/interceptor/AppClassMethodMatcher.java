package com.jd.rec.nl.core.guice.interceptor;

import com.google.inject.matcher.AbstractMatcher;

import java.lang.reflect.Method;

/**
 * @author linmx
 * @date 2018/5/17
 */
public class AppClassMethodMatcher extends AbstractMatcher<Method> {

    private static final String appPath = "com.jd.rec.nl";

    @Override
    public boolean matches(Method method) {
        return method.getDeclaringClass().getName().startsWith(appPath);
    }
}
