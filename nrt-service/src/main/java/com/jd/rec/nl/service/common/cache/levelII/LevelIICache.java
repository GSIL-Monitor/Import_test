package com.jd.rec.nl.service.common.cache.levelII;

import com.jd.rec.nl.core.cache.CacheDefine;
import com.jd.rec.nl.core.cache.guava.GuavaCache;

import javax.cache.CacheManager;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LevelIICache<K extends Serializable, V extends Serializable> extends GuavaCache<K, V> {

    /**
     * 用于二级缓存
     */
    private javax.cache.Cache<K, V> levelIICache;

    @SuppressWarnings("unchecked")
    public LevelIICache(String cacheName, LevelIICacheConfiguration configuration, CacheManager cacheManager) {
        super(cacheName, configuration, cacheManager);
        // 构建二级缓存
        CacheDefine levelIIDefine = configuration.getLevelIICacheDefine();
        levelIICache = levelIIDefine.getProvider().getCacheManager().createCache(cacheName.concat("_levelII"),
                levelIIDefine.getConfiguration());
    }


    @Override
    public V get(K key) {
        V value = super.get(key);
        if (value == null) {
            value = this.levelIICache.get(key);
            if (value != null) {
                super.put(key, value);
            }
        }
        return value;
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys) {
        Map<K, V> cached = new HashMap<K, V>(super.getAll(keys));
        if (cached.size() == keys.size()) {
            return cached;
        }
        Set<K> noCached = keys.stream().filter(key -> !cached.containsKey(key)).collect(Collectors.toSet());
        Map<K, V> levelIICached = levelIICache.getAll(noCached);
        if (levelIICached.size() > 0) {
            super.putAll(levelIICached);
            cached.putAll(levelIICached);
        }
        return cached;
    }

    @Override
    public boolean containsKey(K key) {
        return super.containsKey(key) || levelIICache.containsKey(key);
    }

    @Override
    public void put(K key, V value) {
        super.put(key, value);
        this.levelIICache.put(key, value);
    }

    @Override
    public V getAndPut(K key, V value) {
        V previous = null;
        if (super.containsKey(key)) {
            previous = super.get(key);
        } else {
            if (levelIICache.containsKey(key)) {
                previous = levelIICache.get(key);
            }
        }
        this.put(key, value);
        return previous;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        super.putAll(map);
        levelIICache.putAll(map);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        if (this.containsKey(key)) {
            return false;
        } else {
            this.put(key, value);
            return true;
        }
    }

    @Override
    public boolean remove(K key) {
        boolean exist = levelIICache.remove(key);
        if (exist) {
            super.remove(key);
        }
        return exist;
    }

    @Override
    public boolean remove(K key, V oldValue) {
        super.remove(key, oldValue);
        return levelIICache.remove(key, oldValue);
    }

    @Override
    public V getAndRemove(K key) {
        super.getAndRemove(key);
        return levelIICache.getAndRemove(key);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        super.replace(key, oldValue, newValue);
        return levelIICache.replace(key, oldValue, newValue);
    }

    @Override
    public boolean replace(K key, V value) {
        super.replace(key, value);
        return levelIICache.replace(key, value);
    }

    @Override
    public V getAndReplace(K key, V value) {
        super.getAndReplace(key, value);
        return levelIICache.getAndReplace(key, value);
    }

    @Override
    public void removeAll(Set<? extends K> keys) {
        super.removeAll(keys);
        levelIICache.removeAll(keys);
    }

    @Override
    public void removeAll() {
        Set<K> allKeys = cacheWrap.asMap().keySet();
        levelIICache.removeAll(allKeys);
        super.removeAll();
    }

    @Override
    public void clear() {
        this.removeAll();
    }

}
