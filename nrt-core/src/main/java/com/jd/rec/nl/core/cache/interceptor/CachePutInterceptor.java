package com.jd.rec.nl.core.cache.interceptor;

import com.jd.rec.nl.core.cache.CacheFactory;
import com.jd.rec.nl.core.cache.annotation.CacheKVPairs;
import com.jd.rec.nl.core.cache.annotation.CacheName;
import com.jd.rec.nl.core.cache.domain.CachedNull;
import com.jd.rec.nl.core.exception.WrongConfigException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;

import javax.cache.Cache;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheValue;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/5/29
 */
public class CachePutInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = getLogger(CachePutInterceptor.class);

    private static final Map<String, CacheInfo> cachesInfo = new HashMap<>();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        CachePut cachePut = invocation.getStaticPart().getAnnotation(CachePut.class);
        CacheInfo cacheInfo;
        if (cachesInfo.containsKey(invocation.getMethod().toString())) {
            cacheInfo = cachesInfo.get(invocation.getMethod().toString());
        } else {
            cacheInfo = loadCacheInfo(invocation, cachePut);
        }

        if (cacheInfo instanceof CacheInfo.InvalidCacheInfo) {
            return invocation.proceed();
        }

        Cache cache = cacheInfo.getCache(invocation);
        Object key = invocation.getArguments()[cacheInfo.getKeyIndex()];
        Object value = invocation.getArguments()[cacheInfo.getValueIndex()];
        if (!cachePut.afterInvocation() && !(cacheInfo instanceof CacheInfo.InvalidCacheInfo) && cache != null) {
            saveToCache(cache, cacheInfo.isBatch(), key, value);
        }

        Object ret = invocation.proceed();
        if (cachePut.afterInvocation() && !(cacheInfo instanceof CacheInfo.InvalidCacheInfo) && cache != null) {
            saveToCache(cache, cacheInfo.isBatch(), key, value);
        }
        return ret;
    }

    private CacheInfo loadCacheInfo(MethodInvocation invocation, CachePut cachePut) {
        CacheInfo cacheInfo;
        try {
            String cacheName = cachePut.cacheName();
            int keyIndex = -1, valueIndex = -1;
            boolean batch = false;
            List<Integer> cacheNamesIndex = new ArrayList<>();
            int i = 0;
            while (i < invocation.getMethod().getParameters().length) {
                Parameter parameter = invocation.getMethod().getParameters()[i];
                if (parameter.isAnnotationPresent(CacheValue.class)) {
                    if (valueIndex != -1) {
                        throw new WrongConfigException(
                                String.format("There can be only one CacheValue annotation for one method:%s",
                                        invocation.getMethod().toString()));
                    }
                    valueIndex = i;
                } else if (parameter.isAnnotationPresent(CacheKey.class)) {
                    if (keyIndex != -1) {
                        throw new WrongConfigException(
                                String.format("There can be only one CacheKey annotation for one method:%s",
                                        invocation.getMethod().toString()));
                    }
                    keyIndex = i;
                } else if (parameter.isAnnotationPresent(CacheKVPairs.class)) {
                    if (keyIndex != -1 || valueIndex != -1) {
                        throw new WrongConfigException(
                                String.format("one method can be annotate either CacheKey and CacheValue or CacheKVPairs:%s",
                                        invocation.getMethod().toString()));
                    }
                    if (!Map.class.isAssignableFrom(parameter.getType())) {
                        throw new WrongConfigException(
                                String.format("then method annotated with CacheKVPairs must return a value of map:%s",
                                        invocation.getMethod().toString()));
                    }
                    keyIndex = i;
                    valueIndex = i;
                    batch = true;
                }
                if (parameter.isAnnotationPresent(CacheName.class)) {
                    cacheNamesIndex.add(i);
                }
                i++;
            }
            if (keyIndex == -1 || valueIndex == -1) {
                throw new WrongConfigException(String.format("未同时配置CacheValue和CacheKey,此方法的缓存不生效:%s",
                        invocation.getMethod().toString()));
            }
            if (cacheNamesIndex.isEmpty()) {
                if (CacheFactory.getCache(cacheName) == null) {
                    throw new WrongConfigException(String.format("未配置 %s 的缓存配置,此方法的缓存不生效:%s", cacheName,
                            invocation.getMethod().toString()));
                }
            }
            cacheInfo = new CacheInfo(batch, cacheName, keyIndex, valueIndex);
            cacheInfo.setDynamicCacheNameIndexes(cacheNamesIndex);
        } catch (WrongConfigException e) {
            LOGGER.warn(e.getMessage());
            cacheInfo = new CacheInfo.InvalidCacheInfo();
        }
        cachesInfo.put(invocation.getMethod().toString(), cacheInfo);
        return cacheInfo;
    }

    /**
     * 保存到缓存
     *
     * @param cache 缓存
     * @param batch 是否批量
     * @param key
     * @param value
     */
    private void saveToCache(Cache cache, boolean batch, Object key, Object value) {
        if (batch) {
            Map willSave = new HashMap();
            ((Map) value).forEach((itemKey, itemValue) ->
                    willSave.put(itemKey, itemValue == null ? CachedNull.nullValue() : itemValue));
            cache.putAll(willSave);
        } else {
            cache.put(key, value == null ? CachedNull.nullValue() : value);
        }
    }

}
