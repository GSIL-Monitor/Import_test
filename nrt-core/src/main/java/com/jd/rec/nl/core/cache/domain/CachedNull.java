package com.jd.rec.nl.core.cache.domain;

/**
 * 占位符,用于null的替代
 * <p>
 * Created by linmx on 2018/3/8.
 */
public class CachedNull {

    private static CachedNull cachedNull = new CachedNull();

    private CachedNull() {
    }

    public static CachedNull nullValue() {
        return cachedNull;
    }
}
