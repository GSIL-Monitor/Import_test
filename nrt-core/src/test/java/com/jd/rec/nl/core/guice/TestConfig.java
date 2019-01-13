package com.jd.rec.nl.core.guice;

import com.jd.rec.nl.core.guice.config.Configuration;
import com.jd.rec.nl.core.guice.interceptor.InterceptorConfig;

/**
 * @author linmx
 * @date 2018/5/28
 */
@Configuration
public class TestConfig {

    public InterceptorConfig debugInterceptor() {
        return new InterceptorConfig<>(TestLog.class, new LogMethod());
    }
}
