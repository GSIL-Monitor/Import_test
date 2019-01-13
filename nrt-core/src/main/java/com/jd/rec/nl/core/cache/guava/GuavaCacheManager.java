package com.jd.rec.nl.core.cache.guava;

import com.google.common.collect.ImmutableSet;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class GuavaCacheManager implements CacheManager {

    private CachingProvider cachingProvider;
    private Properties defaultProperties;
    private Map<String, GuavaCache> storedCaches = new HashMap<>();
    private AtomicBoolean closed = new AtomicBoolean(true);

    public GuavaCacheManager(CachingProvider cachingProvider, Properties defaultProperties) {
        this.cachingProvider = cachingProvider;
        this.defaultProperties = defaultProperties;
        closed.set(false);
    }

    @Override
    public CachingProvider getCachingProvider() {
        return cachingProvider;
    }

    @Override
    public URI getURI() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public Properties getProperties() {
        return defaultProperties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V, C extends Configuration<K, V>> Cache<K, V> createCache(String cacheName, C configuration) throws IllegalArgumentException {
        GuavaCache cache = new GuavaCache(cacheName, (GuavaConfiguration) configuration, this);
        storedCaches.put(cacheName, cache);
        return cache;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        Cache cache = getCache(cacheName);
        if (cache == null) {
            return null;
        }
        Configuration configuration = cache.getConfiguration(GuavaConfiguration.class);
        if (keyType == configuration.getKeyType() && valueType == configuration.getValueType()) {
            return cache;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return storedCaches.get(cacheName);
    }

    @Override
    public Iterable<String> getCacheNames() {
        return ImmutableSet.copyOf(storedCaches.keySet());
    }

    @Override
    public void destroyCache(String cacheName) {
        if (cacheName == null) {
            throw new NullPointerException();
        }
        Cache<?, ?> cache = storedCaches.remove(cacheName);
        if (cache != null) {
            cache.close();
        }
    }

    @Override
    public void enableManagement(String cacheName, boolean enabled) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void enableStatistics(String cacheName, boolean enabled) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            for (Cache<?, ?> cache : storedCaches.values()) {
                try {
                    cache.close();
                } catch (Exception e) {
                    // no-op
                }
            }
            storedCaches.clear();
            cachingProvider.close();
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
