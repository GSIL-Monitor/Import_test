package com.jd.rec.nl.core.guice;

import com.jd.rec.nl.core.guice.interceptor.InterceptorProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author linmx
 * @date 2018/5/28
 */
@InterceptorProvider(LogMethod.class)
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TestLog {
}
