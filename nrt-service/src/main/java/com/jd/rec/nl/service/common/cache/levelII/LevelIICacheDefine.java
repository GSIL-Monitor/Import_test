package com.jd.rec.nl.service.common.cache.levelII;

import com.jd.rec.nl.core.cache.CacheDefine;
import com.jd.rec.nl.core.cache.guava.GuavaCacheDefine;
import com.jd.rec.nl.core.exception.WrongConfigException;

import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.spi.CachingProvider;

/**
 * @author linmx
 * @date 2018/7/2
 */
public abstract class LevelIICacheDefine extends GuavaCacheDefine {

    private CacheDefine levelIIDefine = null;

    public CacheDefine getLevelIIDefine() {
        return levelIIDefine;
    }

    public void setLevelIIDefine(CacheDefine levelIIDefine) {
        this.levelIIDefine = levelIIDefine;
    }

    @Override
    public CachingProvider getProvider() {
        return new LevelIICacheProvider();
    }

    @Override
    public Configuration getConfiguration() {
        LevelIICacheConfiguration custom = new LevelIICacheConfiguration<>();
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
            throw new WrongConfigException("levelII cache doesn't support cache loader");
        }
        custom.setReadThrough(false);
        custom.setLevelIICacheDefine(levelIIDefine);
        return custom;
    }

    @Override
    public boolean isDynamicCreate() {
        return false;
    }
}
