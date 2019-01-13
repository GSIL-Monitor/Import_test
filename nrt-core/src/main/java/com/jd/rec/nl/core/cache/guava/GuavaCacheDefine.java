package com.jd.rec.nl.core.cache.guava;

import com.jd.rec.nl.core.cache.CacheDefine;

import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.integration.CacheLoader;
import javax.cache.spi.CachingProvider;
import java.util.concurrent.TimeUnit;

/**
 * @author linmx
 * @date 2018/6/26
 */
public abstract class GuavaCacheDefine implements CacheDefine {

    protected long maximumSize = -1L;

    protected long maximumWeight = -1L;

    protected long statisticsInterval = -1L;

    protected long expireAfterWrite = -1L;

    protected TimeUnit expireTimeUnit = TimeUnit.SECONDS;

    protected long expireAfterAccess = -1L;

    protected CacheLoader cacheLoader;

    /**
     * 设置cacheLoader
     * 使用声明式cache时,尽量不配置read-through cache,read-through功能已由切面实现
     *
     * @param cacheLoader
     */
    @Deprecated
    public void setCacheLoader(CacheLoader cacheLoader) {
        this.cacheLoader = cacheLoader;
    }

    public void setMaximumSize(long maximumSize) {
        this.maximumSize = maximumSize;
    }

    public void setMaximumWeight(long maximumWeight) {
        this.maximumWeight = maximumWeight;
    }

    public void setStatisticsInterval(long statisticsInterval) {
        this.statisticsInterval = statisticsInterval;
    }

    public void setExpireAfterWrite(long expireAfterWrite) {
        this.expireAfterWrite = expireAfterWrite;
    }

    public void setExpireTimeUnit(TimeUnit expireTimeUnit) {
        this.expireTimeUnit = expireTimeUnit;
    }

    public void setExpireAfterAccess(long expireAfterAccess) {
        this.expireAfterAccess = expireAfterAccess;
    }

    @Override
    public CachingProvider getProvider() {
        return new GuavaCacheProvider();
    }

    @Override
    public Configuration getConfiguration() {
        GuavaConfiguration custom = new GuavaConfiguration<>();
        custom.setStoreByValue(false);
        custom.setTypes(Object.class, Object.class);
        custom.setReadThrough(true);
        if (maximumSize != -1)
            custom.setMaximumSize(maximumSize);
        else if (maximumWeight != -1) {
            custom.setMaximumWeight(maximumWeight);
        }
        if (statisticsInterval != -1) {
            custom.setStatisticsInterval(statisticsInterval);
            custom.setStatisticsEnabled(true);
        }
        if (expireAfterWrite != -1) {
            final ModifiedExpiryPolicy policy = new ModifiedExpiryPolicy(new Duration(expireTimeUnit, expireAfterWrite));
            custom.setExpiryPolicyFactory((Factory<ExpiryPolicy>) () -> policy);
        } else if (expireAfterAccess != -1) {
            final TouchedExpiryPolicy policy = new TouchedExpiryPolicy(new Duration(expireTimeUnit, expireAfterAccess));
            custom.setExpiryPolicyFactory((Factory<ExpiryPolicy>) () -> policy);
        }
        if (cacheLoader != null) {
            custom.setReadThrough(true);
            custom.setCacheLoaderFactory(() -> cacheLoader);
        } else {
            custom.setReadThrough(false);
        }
        return custom;
    }

    @Override
    public boolean isDynamicCreate() {
        return false;
    }
}
