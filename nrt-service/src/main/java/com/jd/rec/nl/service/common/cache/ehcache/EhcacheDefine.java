package com.jd.rec.nl.service.common.cache.ehcache;

import com.jd.rec.nl.core.cache.CacheDefine;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;
import org.ehcache.jsr107.Eh107Configuration;
import org.ehcache.jsr107.EhcacheCachingProvider;

import javax.cache.configuration.Configuration;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author wl
 * @date 2018/9/14
 */
public abstract class EhcacheDefine implements CacheDefine {

    //默认过期时间
    protected long expireAfterWrite = 1L;
    //过期时间单位
    protected TimeUnit expireTimeUnit = SECONDS;
    //堆外缓存大小
    private int OffHepapSize;

    public int getOffHepapSize() {
        return OffHepapSize;
    }

    public void setOffHepapSize(int offHepapSize) {
        OffHepapSize = offHepapSize;
    }

    public TimeUnit getExpireTimeUnit() {
        return expireTimeUnit;
    }

    public void setExpireTimeUnit(TimeUnit expireTimeUnit) {
        this.expireTimeUnit = expireTimeUnit;
    }

    public long getExpireAfterWrite() {
        return expireAfterWrite;
    }

    public void setExpireAfterWrite(long expireAfterWrite) {
        this.expireAfterWrite = expireAfterWrite;
    }

    @Override
    public EhcacheCachingProvider getProvider() {
        return new EhcacheCachingProvider();
    }

    @Override
    public Configuration getConfiguration() {
        CacheConfiguration<Object, Object> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Object.class, Object.class, ResourcePoolsBuilder.newResourcePoolsBuilder().offheap(OffHepapSize, MemoryUnit.MB))
                .withExpiry(Expirations.timeToLiveExpiration(new Duration(expireAfterWrite, expireTimeUnit)))
                .withKeySerializer(new KryoSerialize<>()).withValueSerializer(new KryoSerialize<>()).build();
        Configuration<Object, Object> configuration = Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfiguration);
        return configuration;
    }

    @Override
    public boolean isDynamicCreate() {
        return false;
    }
}
