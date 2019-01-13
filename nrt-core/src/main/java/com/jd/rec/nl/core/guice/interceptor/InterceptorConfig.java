package com.jd.rec.nl.core.guice.interceptor;

import org.aopalliance.intercept.MethodInterceptor;

import java.lang.annotation.Annotation;

/**
 * 用于定义切面的配置类
 *
 * @author linmx
 * @date 2018/5/28
 */
public class InterceptorConfig<M extends MethodInterceptor> {

    private Class<? extends Annotation> annotation;

    private M methodInterceptor;

    public InterceptorConfig(Class<? extends Annotation> annotation, M methodInvocation) {
        this.annotation = annotation;
        this.methodInterceptor = methodInvocation;
    }

    public Class<? extends Annotation> getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    public M getMethodInterceptor() {
        return methodInterceptor;
    }

    public void setMethodInterceptor(M methodInterceptor) {
        this.methodInterceptor = methodInterceptor;
    }
}
