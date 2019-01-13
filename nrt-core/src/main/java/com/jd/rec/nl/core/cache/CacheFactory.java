package com.jd.rec.nl.core.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import java.util.HashMap;
import java.util.Map;

/**
 * cache工厂类
 */
public abstract class CacheFactory {

    private static final Logger logger = LoggerFactory.getLogger(CacheFactory.class);

    private static final Map<String, Cache> storedCaches = new HashMap<>();

    private static final Map<String, CacheDefine> dynamicCacheDefines = new HashMap<>();

    private static final Map<String, CacheDefine> staticCacheDefines = new HashMap<>();

    private static final Object _lock = new Object();

    /**
     * 获取cache实例
     * 首先根据cacheName获取，如果未发现配置则使用备用的cacheType
     *
     * @param cacheName 缓存名
     * @param <K>       缓存中的key类型
     * @param <V>       缓存中的value类型
     * @return cache实例
     * @throws CacheException 异常
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Cache<K, V> getCache(String cacheName) {
        if (!storedCaches.containsKey(cacheName)) {
            CacheDefine cacheDefine = staticCacheDefines.get(cacheName);
            if (cacheDefine == null) {
                return null;
            }
            synchronized (_lock) {
                if (!storedCaches.containsKey(cacheName)) {
                    CachingProvider provider = cacheDefine.getProvider();
                    CacheManager cacheManager = provider.getCacheManager();
                    Cache cache = cacheManager.createCache(cacheDefine.getCacheName(), cacheDefine.getConfiguration());
                    storedCaches.put(cacheDefine.getCacheName(), cache);
                }
            }
        }
        return storedCaches.get(cacheName);
    }

    /**
     * 获取动态的cache实例
     *
     * @param name
     * @param dynamicName
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Cache<K, V> loadDynamicCache(String name, String dynamicName) {
        if (!storedCaches.containsKey(dynamicName)) {
            CacheDefine cacheDefine = dynamicCacheDefines.get(name);
            if (cacheDefine == null) {
                return null;
            }
            synchronized (_lock) {
                if (!storedCaches.containsKey(dynamicName)) {
                    CachingProvider provider = cacheDefine.getProvider();
                    CacheManager cacheManager = provider.getCacheManager();
                    Cache cache = cacheManager.createCache(dynamicName, cacheDefine.getConfiguration());
                    storedCaches.put(dynamicName, cache);
                }
            }
        }
        return storedCaches.get(dynamicName);
    }


    /**
     * 根据配置信息实例化缓存对象
     *
     * @param cacheDefines cache定义
     * @throws CacheException
     */
    @SuppressWarnings("unchecked")
    public static void loadCache(CacheDefine... cacheDefines) throws CacheException {
        try {
            for (CacheDefine cacheDefine : cacheDefines) {
                if (cacheDefine.isDynamicCreate()) {
                    dynamicCacheDefines.put(cacheDefine.getCacheName(), cacheDefine);
                } else {
                    staticCacheDefines.put(cacheDefine.getCacheName(), cacheDefine);
                }
            }
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

}
