package com.jd.rec.nl.service.common.cache.Jimdb;

import com.jd.rec.nl.core.cache.CacheDefine;

import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.spi.CachingProvider;
import java.util.concurrent.TimeUnit;

/**
 * @author linmx
 * @date 2018/7/2
 */
public abstract class JimdbCacheDefine implements CacheDefine {

    private long statisticsInterval = -1;

    private long expireAfterWrite = -1;

    private long expireAfterAccess = -1L;

    private TimeUnit expireTimeUnit = TimeUnit.SECONDS;

    public void setExpireAfterAccess(long expireAfterAccess) {
        this.expireAfterAccess = expireAfterAccess;
    }

    public void setExpireAfterWrite(long expireAfterWrite) {
        this.expireAfterWrite = expireAfterWrite;
    }

    public void setExpireTimeUnit(TimeUnit expireTimeUnit) {
        this.expireTimeUnit = expireTimeUnit;
    }

    @Override
    public CachingProvider getProvider() {
        return new JimdbCacheProvider();
    }

    @Override
    public Configuration getConfiguration() {
        JimdbConfiguration custom = new JimdbConfiguration<>();
        custom.setStoreByValue(false);
        custom.setTypes(Object.class, Object.class);
        custom.setReadThrough(false);

        if (this.statisticsInterval != -1) {
            custom.setStatisticsInterval(this.statisticsInterval);
            custom.setStatisticsEnabled(true);
        }
        if (expireAfterWrite != -1) {
            final ModifiedExpiryPolicy policy = new ModifiedExpiryPolicy(new Duration(expireTimeUnit, expireAfterWrite));
            custom.setExpiryPolicyFactory((Factory<ExpiryPolicy>) () -> policy);
        } else if (expireAfterAccess != -1) {
            final TouchedExpiryPolicy policy = new TouchedExpiryPolicy(new Duration(expireTimeUnit, expireAfterAccess));
            custom.setExpiryPolicyFactory((Factory<ExpiryPolicy>) () -> policy);
        }
        return custom;
    }

    @Override
    public boolean isDynamicCreate() {
        return false;
    }
}
