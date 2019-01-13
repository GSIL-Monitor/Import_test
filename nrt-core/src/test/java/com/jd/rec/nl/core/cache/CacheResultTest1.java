package com.jd.rec.nl.core.cache;

import com.jd.rec.nl.core.cache.annotation.CacheKeys;
import com.jd.rec.nl.core.cache.annotation.CacheName;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/7/2
 */
public class CacheResultTest1 {

    @CacheResult(cacheName = "test1")
    public String get(@CacheKey String string, @CacheName int n) {
        System.out.println("get from raw");
        return string.concat("-").concat(String.valueOf(n));
    }


    @CacheResult(cacheName = "test1")
    public Map<String, String> getAll(@CacheKeys Collection<String> keys, @CacheName int n) {
        System.out.println("get from raw");
        Map<String, String> ret = new HashMap<>();
        for (String key : keys) {
            ret.put(key, key.concat("-").concat(String.valueOf(n)));
        }
        return ret;
    }


}
