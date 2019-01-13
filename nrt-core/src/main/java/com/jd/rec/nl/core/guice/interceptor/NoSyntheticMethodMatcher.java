package com.jd.rec.nl.core.guice.interceptor;

import com.google.inject.matcher.AbstractMatcher;

import java.lang.reflect.Method;

/**
 * @author linmx
 * @date 2018/7/23
 */
public final class NoSyntheticMethodMatcher extends AbstractMatcher<Method> {
    public static final NoSyntheticMethodMatcher INSTANCE = new NoSyntheticMethodMatcher();

    private NoSyntheticMethodMatcher() {
    }

    @Override
    public boolean matches(Method method) {
        return !method.isSynthetic();
    }
}
