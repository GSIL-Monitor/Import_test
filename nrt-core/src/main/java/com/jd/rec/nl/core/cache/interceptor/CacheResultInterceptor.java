package com.jd.rec.nl.core.cache.interceptor;

import com.jd.rec.nl.core.cache.CacheFactory;
import com.jd.rec.nl.core.cache.annotation.CacheKeys;
import com.jd.rec.nl.core.cache.annotation.CacheName;
import com.jd.rec.nl.core.cache.domain.CachedNull;
import com.jd.rec.nl.core.exception.WrongConfigException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;

import javax.cache.Cache;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import java.lang.reflect.Parameter;
import java.util.*;

import static com.jd.rec.nl.core.cache.interceptor.CacheInfo.InvalidCacheInfo;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/27
 */
public class CacheResultInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = getLogger(CacheResultInterceptor.class);

    Map<String, CacheInfo> cacheInfoMap = new HashMap<>();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        CacheResult cacheResult = invocation.getStaticPart().getAnnotation(CacheResult.class);
        CacheInfo cacheInfo;
        if (cacheInfoMap.containsKey(invocation.getMethod().toString())) {
            cacheInfo = cacheInfoMap.get(invocation.getMethod().toString());
        } else {
            cacheInfo = loadCacheInfo(invocation, cacheResult);
        }
        if (cacheInfo instanceof InvalidCacheInfo) {
            return invocation.proceed();
        } else {
            Cache cache = cacheInfo.getCache(invocation);
            if (cache == null) {
                LOGGER.error("cacheInfo[{}] can't get cache from {}", cacheInfo.getCacheName(), invocation.toString());
                cacheInfoMap.put(invocation.getMethod().toString(), new InvalidCacheInfo());
                return invocation.proceed();
            }
            Object key = invocation.getArguments()[cacheInfo.getKeyIndex()];
            if (cacheInfo.isBatch()) {
                Collection request = (Collection) key;
                Map cached = new HashMap<>(cache.getAll(new HashSet(request)));
                for (Object itemKey : cached.keySet()) {
                    if (cached.get(itemKey) instanceof CachedNull) {
                        cached.put(itemKey, null);
                    }
                }
                if (cached.size() == ((Collection) key).size()) {
                    return cached;
                } else {
                    Collection noCache = request.getClass().newInstance();
                    for (Object itemKey : request) {
                        if (!cached.containsKey(itemKey)) {
                            noCache.add(itemKey);
                        }
                    }

                    invocation.getArguments()[cacheInfo.getKeyIndex()] = noCache;
                    Map noCacheValue = new HashMap((Map) invocation.proceed());
                    // 还原参数,防止其他切面用到
                    invocation.getArguments()[cacheInfo.getKeyIndex()] = key;

                    // 缓存未命中的key
                    if (!noCacheValue.isEmpty()) {
                        Map willSave = new HashMap();
                        noCacheValue.forEach((itemKey, value) ->
                                willSave.put(itemKey, value == null ? CachedNull.nullValue() : value));
                        cache.putAll(willSave);

                        noCacheValue.putAll(cached);
                        return noCacheValue;
                    }else {
                        return cached;
                    }
                }
            } else {
                Object value = cache.get(key);
                if (value == null) {
                    value = invocation.proceed();
                    cache.put(key, value == null ? CachedNull.nullValue() : value);
                }
                return value instanceof CachedNull ? null : value;
            }
        }
    }

    private CacheInfo loadCacheInfo(MethodInvocation invocation, CacheResult cacheResult) {
        try {
            String cacheName = cacheResult.cacheName();
            boolean isBatch = false;
            List<Integer> cacheNamesIndex = new ArrayList<>();
            int keyIndex = -1;
            for (int i = 0; i < invocation.getMethod().getParameterCount(); i++) {
                Parameter parameter = invocation.getMethod().getParameters()[i];
                if (parameter.isAnnotationPresent(CacheKey.class)) {
                    if (keyIndex != -1) {
                        throw new WrongConfigException(String.format("CacheKey and CacheKeys only present once in a method:%s",
                                invocation.getMethod().toString()));
                    }
                    keyIndex = i;
                } else if (parameter.isAnnotationPresent(CacheKeys.class)) {
                    if (keyIndex != -1) {
                        throw new WrongConfigException(String.format("CacheKey and CacheKeys only present once in a method:%s",
                                invocation.getMethod().toString()));
                    }
                    if (!Collection.class.isAssignableFrom(parameter.getType())) {
                        throw new WrongConfigException(String.format("CacheKeys can annotate collection parameter only:%s",
                                invocation.getMethod().toString()));
                    }
                    // 校验返回项类型是否为map
                    if (!Map.class.isAssignableFrom(invocation.getMethod().getReturnType())) {
                        throw new WrongConfigException(String.format("batch cache require type of result is map: %s",
                                invocation.getMethod().toString()));
                    }
                    isBatch = true;
                    keyIndex = i;
                }
                if (parameter.isAnnotationPresent(CacheName.class)) {
                    cacheNamesIndex.add(i);
                }
            }
            if (keyIndex == -1) {
                throw new WrongConfigException(String.format("There isn't parameter annotated with cacheKey:%s",
                        invocation.getMethod().toString()));
            }
            if (cacheNamesIndex.isEmpty()) {
                if (CacheFactory.getCache(cacheName) == null) {
                    throw new WrongConfigException(String.format("未配置 %s 的缓存配置,此方法的缓存不生效:%s", cacheName,
                            invocation.getMethod().toString()));
                }
            }
            CacheInfo cacheInfo = new CacheInfo(isBatch, cacheName, keyIndex, -1);
            cacheInfo.setDynamicCacheNameIndexes(cacheNamesIndex);
            this.cacheInfoMap.put(invocation.getMethod().toString(), cacheInfo);
            return cacheInfo;
        } catch (WrongConfigException e) {
            LOGGER.warn(e.getMessage());
            CacheInfo cacheInfo = new InvalidCacheInfo();
            this.cacheInfoMap.put(invocation.getMethod().toString(), cacheInfo);
            return cacheInfo;
        }
    }


}
