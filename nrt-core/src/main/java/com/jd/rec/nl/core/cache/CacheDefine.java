package com.jd.rec.nl.core.cache;

import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;

/**
 * @author linmx
 * @date 2018/6/26
 */
public interface CacheDefine {

    /**
     * 指定使用的cache provider
     *
     * @return
     */
    CachingProvider getProvider();

    /**
     * 获取cache配置
     *
     * @return
     */
    Configuration getConfiguration();

    /**
     * 获取cache名
     *
     * @return
     */
    String getCacheName();

    /**
     * 是否根据运行时情况动态生成
     *
     * @return
     */
    boolean isDynamicCreate();
}
