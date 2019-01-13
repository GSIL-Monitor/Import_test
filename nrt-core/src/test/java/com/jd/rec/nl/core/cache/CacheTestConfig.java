package com.jd.rec.nl.core.cache;

import com.jd.rec.nl.core.cache.guava.GuavaCacheDefine;
import com.jd.rec.nl.core.guice.config.Configuration;

/**
 * @author linmx
 * @date 2018/7/2
 */
@Configuration
public class CacheTestConfig {

    public CacheDefine testCache() {
        GuavaCacheDefine guavaCacheDefine = new GuavaCacheDefine() {
            @Override
            public String getCacheName() {
                return "test";
            }

            @Override
            public boolean isDynamicCreate() {
                return false;
            }
        };
        guavaCacheDefine.setStatisticsInterval(1000L);
        guavaCacheDefine.setExpireAfterWrite(100000L);
        return guavaCacheDefine;
    }

    public CacheDefine test10Cache() {
        GuavaCacheDefine guavaCacheDefine = new GuavaCacheDefine() {
            @Override
            public String getCacheName() {
                return "test1";
            }

            @Override
            public boolean isDynamicCreate() {
                return true;
            }
        };
        guavaCacheDefine.setStatisticsInterval(1000L);
        guavaCacheDefine.setExpireAfterWrite(100000L);
        return guavaCacheDefine;
    }
}
