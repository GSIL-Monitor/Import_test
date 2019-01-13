package com.jd.rec.nl.core.cache.guava;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class GuavaCache<K, V> implements javax.cache.Cache<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(GuavaCache.class);

    private final AtomicBoolean closed = new AtomicBoolean();

    protected Cache<K, V> cacheWrap;

    private String cacheName;

    private CacheManager cacheManager;

    private MutableConfiguration<K, V> configuration;

    private ReentrantLock lock = new ReentrantLock();

    private long statisticsInterval;
    private long statisticsTimestamp;
    private Logger statisticsLogger;

    @SuppressWarnings("unchecked")
    public GuavaCache(String cacheName, GuavaConfiguration configuration, CacheManager cacheManager) {
        this.cacheName = cacheName;
        this.configuration = configuration;
        this.cacheManager = cacheManager;

        CacheBuilder cacheBuilder = CacheBuilder.newBuilder();

        if (configuration.getMaximumSize() != -1) {
            cacheBuilder.maximumSize(configuration.getMaximumSize());
        } else if (configuration.getMaximumWeight() != -1) {
            cacheBuilder.maximumWeight(configuration.getMaximumWeight());
        }

        ExpiryPolicy expiryPolicy = (ExpiryPolicy) configuration.getExpiryPolicyFactory().create();
        if (expiryPolicy instanceof ModifiedExpiryPolicy) // == Guava expire after write
        {
            Duration d = expiryPolicy.getExpiryForUpdate();
            cacheBuilder.expireAfterWrite(d.getDurationAmount(), d.getTimeUnit());
        } else if (expiryPolicy instanceof TouchedExpiryPolicy) // == Guava expire after access
        {
            Duration d = expiryPolicy.getExpiryForAccess();
            cacheBuilder.expireAfterAccess(d.getDurationAmount(), d.getTimeUnit());
        }

        if (configuration.isStatisticsEnabled()) {
            cacheBuilder.recordStats();
            this.statisticsInterval = configuration.getStatisticsInterval();
            this.statisticsLogger = LoggerFactory.getLogger(cacheName);
        }

        if (configuration.isReadThrough()) {
            final Factory<CacheLoader<K, V>> factory = configuration.getCacheLoaderFactory();
            this.cacheWrap = cacheBuilder.build(new com.google.common.cache.CacheLoader() {
                CacheLoader cacheLoader = factory.create();

                @Override
                @ParametersAreNonnullByDefault
                public Object load(Object key) throws Exception {
                    return cacheLoader.load(key);
                }

                @Override
                public Map loadAll(Iterable keys) throws Exception {
                    return cacheLoader.loadAll(keys);
                }
            });
        } else {
            this.cacheWrap = cacheBuilder.build();
        }
        this.closed.set(false);
    }

    @Override
    public V get(K key) {
        this.cacheStatistics();
        try {
            if (configuration.isReadThrough())
                return ((LoadingCache<K, V>) cacheWrap).get(key);
            else {
                return cacheWrap.getIfPresent(key);
            }
        } catch (ExecutionException e) {
            logger.warn(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys) {
        this.cacheStatistics();
        if (keys == null || keys.contains(null)) {
            throw new NullPointerException();
        }
        try {
            if (configuration.isReadThrough())
                return ((LoadingCache<K, V>) cacheWrap).getAll(keys);
            else {
                return cacheWrap.getAllPresent(keys);
            }
        } catch (ExecutionException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return cacheWrap.asMap().containsKey(key);
    }

    @Override
    public void loadAll(final Set<? extends K> keys, final boolean replaceExistingValues, final CompletionListener
            completionListener) {
        this.cacheStatistics();
        if (keys == null || completionListener == null) {
            throw new NullPointerException();
        }
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                if (configuration.isReadThrough()) {
                    for (K key : keys) {
                        if (!cacheWrap.asMap().containsKey(key)) {
                            ((LoadingCache<K, V>) cacheWrap).get(key);
                        } else if (replaceExistingValues) {
                            ((LoadingCache<K, V>) cacheWrap).refresh(key);
                        }
                    }
                }
            } catch (Exception e) {
                completionListener.onException(e);
            } finally {
                completionListener.onCompletion();
            }
        });
    }

    @Override
    public void put(K key, V value) {
        this.cacheStatistics();
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        cacheWrap.put(key, value);
    }

    @Override
    public V getAndPut(K key, V value) {
        this.cacheStatistics();
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        if (cacheWrap.asMap().containsKey(key)) {
            return cacheWrap.asMap().put(key, value);
        }
        cacheWrap.put(key, value);
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        this.cacheStatistics();
        if (map == null || map.containsKey(null)) {
            throw new NullPointerException();
        }
        cacheWrap.putAll(map);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        this.cacheStatistics();
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        return (cacheWrap.asMap().putIfAbsent(key, value) == null);
    }

    @Override
    public boolean remove(K key) {
        this.cacheStatistics();
        if (key == null) {
            throw new NullPointerException();
        }
        boolean exist = cacheWrap.asMap().containsKey(key);
        if (exist) {
            cacheWrap.invalidate(key);
        }
        return exist;
    }

    @Override
    public boolean remove(K key, V oldValue) {
        this.cacheStatistics();
        if (key == null || oldValue == null) {
            throw new NullPointerException();
        }
        return cacheWrap.asMap().remove(key, oldValue);
    }

    @Override
    public V getAndRemove(K key) {
        this.cacheStatistics();
        if (key == null) {
            throw new NullPointerException();
        }
        return cacheWrap.asMap().remove(key);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        this.cacheStatistics();
        if (key == null || oldValue == null || newValue == null) {
            throw new NullPointerException();
        }
        return cacheWrap.asMap().replace(key, oldValue, newValue);
    }

    @Override
    public boolean replace(K key, V value) {
        this.cacheStatistics();
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        return (cacheWrap.asMap().replace(key, value) != null);
    }

    @Override
    public V getAndReplace(K key, V value) {
        this.cacheStatistics();
        if (key == null || value == null) {
            throw new NullPointerException();
        }

        return cacheWrap.asMap().replace(key, value);
    }

    @Override
    public void removeAll(Set<? extends K> keys) {
        this.cacheStatistics();
        if (keys == null || keys.contains(null)) {
            throw new NullPointerException();
        }
        cacheWrap.invalidateAll(keys);
    }

    @Override
    public void removeAll() {
        this.cacheStatistics();
        cacheWrap.invalidateAll();
    }

    @Override
    public void clear() {
        this.removeAll();
    }

    @Override
    public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments) throws EntryProcessorException {
        throw new UnsupportedOperationException("guava cache not supported this method");
    }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor,
                                                         Object... arguments) {
        throw new UnsupportedOperationException("guava cache not supported this method");
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @Override
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    @Override
    public void close() {
        this.cacheStatistics();
        if (closed.compareAndSet(false, true)) {
            cacheWrap.invalidateAll();
            cacheWrap.cleanUp();
            cacheManager.destroyCache(this.getName());
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        if (clazz.isAssignableFrom(LoadingCache.class)) {
            return clazz.cast(this.cacheWrap);
        }
        throw new IllegalArgumentException();
    }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {

        List<Entry<K, V>> list = new ArrayList<>();

        for (final Map.Entry<K, V> entry : cacheWrap.asMap().entrySet()) {
            list.add(new Entry<K, V>() {
                @Override
                public K getKey() {
                    return entry.getKey();
                }

                @Override
                public V getValue() {
                    return entry.getValue();
                }

                @Override
                public <T> T unwrap(Class<T> clazz) {
                    return clazz.cast(entry);
                }
            });
        }

        return Collections.unmodifiableList(list).iterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
        return (C) configuration;
    }

    /**
     * 进行统计信息打印
     */
    private void cacheStatistics() {
        if (!this.configuration.isStatisticsEnabled()) {
            return;
        }
        if (System.currentTimeMillis() - this.statisticsTimestamp < this.statisticsInterval) {
            return;
        }
        if (!lock.isLocked()) {
            boolean locked = false;
            try {
                locked = lock.tryLock();
                if (locked) {
                    CacheStats stats = this.cacheWrap.stats();
                    this.statisticsLogger
                            .error("{} cache status: size = {}, hitCount= {}, missCount = {}, hitRate = {}, evictCount= {} [{}] ",
                                    this.cacheName, this.cacheWrap.asMap().size(), stats.hitCount(), stats.missCount(),
                                    stats.hitRate(), stats.evictionCount(), this.toString());
                    this.statisticsTimestamp = System.currentTimeMillis();

                }
            } finally {
                if (locked) {
                    lock.unlock();
                }
            }
        }
    }
}
