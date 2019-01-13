package com.jd.rec.nl.core.cache;

import com.google.inject.Module;
import com.google.inject.matcher.Matchers;
import com.jd.rec.nl.core.cache.interceptor.CachePutInterceptor;
import com.jd.rec.nl.core.cache.interceptor.CacheResultInterceptor;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.guice.config.Configuration;

import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheResult;

/**
 * @author linmx
 * @date 2018/5/29
 */
@Configuration
public class CacheConfiguration {

    public Module cacheBinderModule() {
        boolean cacheEnable = true;
        if (ConfigBase.getSystemConfig().hasPath("cache.enable")) {
            cacheEnable = ConfigBase.getSystemConfig().getBoolean("cache.enable");
        }
        if (cacheEnable)
            return binder -> {
                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(CachePut.class), new CachePutInterceptor());
                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheResult.class), new CacheResultInterceptor());
                //            binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheRemove.class), new
                // CachePutInterceptor());
                //            binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheRemoveAll.class), new
                // CachePutInterceptor());
            };
        else
            return null;
    }
}
