package com.jd.rec.nl.core.cache;

import com.jd.rec.nl.core.cache.annotation.CacheKVPairs;
import com.jd.rec.nl.core.cache.annotation.CacheKeys;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/7/2
 */
public class CacheResultTest {

    @CacheResult(cacheName = "test")
    public String get(@CacheKey String string, int n) {
        System.out.println("get from raw");
        if (n == 0) {
            return null;
        }
        return string.concat("-").concat(String.valueOf(n));
    }


    @CacheResult(cacheName = "test")
    public Map<String, String> getAll(@CacheKeys Collection<String> keys, int n) {
        System.out.println("get from raw");
        Map<String, String> ret = new HashMap<>();
        for (String key : keys) {
            if (n == 0) {
                ret.put(key, null);
            } else
                ret.put(key, key.concat("-").concat(String.valueOf(n)));
        }
        return ret;
    }

    @CachePut(cacheName = "test")
    public void put(@CacheKey String key, @CacheValue String value) {
    }

    @CachePut(cacheName = "test")
    public void putAll(@CacheKVPairs Map<String, String> kvMap) {

    }
}
