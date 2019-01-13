package com.jd.rec.nl.core.cache.interceptor;

import com.jd.rec.nl.core.cache.CacheFactory;
import org.aopalliance.intercept.MethodInvocation;

import javax.cache.Cache;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linmx
 * @date 2018/7/2
 */
class CacheInfo {

    boolean batch = false;

    String cacheName;

    List<Integer> dynamicCacheNameIndexes = new ArrayList<>();

    int keyIndex;

    int valueIndex;

    public CacheInfo(boolean batch, String cacheName, int keyIndex, int valueIndex) {
        this.batch = batch;
        this.cacheName = cacheName;
        this.keyIndex = keyIndex;
        this.valueIndex = valueIndex;
    }

    public void setDynamicCacheNameIndexes(List<Integer> dynamicCacheNameIndexes) {
        this.dynamicCacheNameIndexes = dynamicCacheNameIndexes;
    }

    public boolean isBatch() {
        return batch;
    }

    public String getCacheName() {
        return cacheName;
    }

    public int getKeyIndex() {
        return keyIndex;
    }

    public int getValueIndex() {
        return valueIndex;
    }

    public Cache getCache(MethodInvocation invocation) {
        if (dynamicCacheNameIndexes.isEmpty()) {
            return CacheFactory.getCache(cacheName);
        } else {
            StringBuilder dynamicName = new StringBuilder(cacheName);
            dynamicCacheNameIndexes.forEach(index -> dynamicName.append("-").append(invocation.getArguments()[index].toString()));
            Cache cache = CacheFactory.getCache(dynamicName.toString());
            if (cache == null) {
                cache = CacheFactory.loadDynamicCache(cacheName, dynamicName.toString());
            }
            return cache;
        }
    }

    protected static class InvalidCacheInfo extends CacheInfo {
        public InvalidCacheInfo() {
            super(false, null, -1, -1);
        }
    }

}
