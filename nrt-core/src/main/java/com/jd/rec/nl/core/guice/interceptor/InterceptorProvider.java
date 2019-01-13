package com.jd.rec.nl.core.guice.interceptor;

import org.aopalliance.intercept.MethodInterceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author linmx
 * @date 2018/5/17
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface InterceptorProvider {

    /**
     * @return
     */
    Class<? extends MethodInterceptor> value();

}
