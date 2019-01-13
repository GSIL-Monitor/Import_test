package com.jd.rec.nl.core.guice.config;

import com.jd.rec.nl.core.guice.InjectorService;

/**
 * 用于个性化初始化操作,在configuration中定义bean后,将在系统第一次调用 {@link InjectorService#getCommonInjector()}
 * 时触发调用{@link Initializer#initialize()}方法
 *
 * @author linmx
 * @date 2018/8/27
 */
public interface Initializer {

    /**
     * 初始化方法
     */
    void initialize();
}
