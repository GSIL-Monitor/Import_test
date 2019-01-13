package com.jd.rec.nl.service.common.cache.Jimdb;

import com.jd.jim.cli.Cluster;
import com.jd.jim.cli.PipelineClient;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.core.utils.KryoUtils;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.rec.nl.service.infrastructure.Jimdb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class JimdbCache<K extends Serializable, V extends Serializable>
        implements javax.cache.Cache<K, V> {

    public static final String cachePrefix = "nrt_cache_";
    private static final Logger logger = LoggerFactory.getLogger(JimdbCache.class);
    private final AtomicBoolean closed = new AtomicBoolean();

    private String cacheName;

    private JimdbCacheManager cacheManager;

    private MutableConfiguration<K, V> configuration;

    private ReentrantLock lock = new ReentrantLock();

    private Cluster workCluster;

    private long statisticsInterval;

    private long statisticsTimestamp;

    private Logger statisticsLogger;

    private boolean expireEnable = false;

    private boolean flashExpireTime = false;

    private long expireTime;

    private TimeUnit expireTimeUnit;

    private String monitorKey;

    @SuppressWarnings("unchecked")
    public JimdbCache(String cacheName, JimdbConfiguration configuration, JimdbCacheManager cacheManager) {
        if (cacheName == null || cacheName.isEmpty() || null == configuration || null == cacheManager) {
            logger.error("null parameter in jimdbCache's constructor");
            return;
        }
        this.cacheName = cacheName;
        this.configuration = configuration;
        this.cacheManager = cacheManager;
        Jimdb jimdb = InjectorService.getCommonInjector().getInstance(Jimdb.class);
        this.workCluster = jimdb.getCluster(CLUSTER.MJQ);
        this.monitorKey = jimdb.monitorKey;

        if (configuration.isStatisticsEnabled()) {
            this.statisticsInterval = configuration.getStatisticsInterval();
            this.statisticsLogger = LoggerFactory.getLogger(cacheName);
        }

        Factory<ExpiryPolicy> factory = configuration.getExpiryPolicyFactory();
        if (factory != null) {
            expireEnable = true;
            ExpiryPolicy expiryPolicy = factory.create();
            Duration d = expiryPolicy.getExpiryForCreation();
            this.expireTime = d.getDurationAmount();
            this.expireTimeUnit = d.getTimeUnit();
            if (expiryPolicy instanceof TouchedExpiryPolicy) {
                this.flashExpireTime = true;
            }
        }
    }

    //    public static void main(String[] args) {
    //        //verify the serializable and deserializable
    //        Cluster cluster = JimClientFactory.getSingleCluster("jim://1803528671997086613/2");
    //        JimdbConfiguration configuration = new JimdbConfiguration();
    //        final CreatedExpiryPolicy policy = new CreatedExpiryPolicy(new Duration(TimeUnit.MILLISECONDS, 100000));
    //        configuration.setExpiryPolicyFactory(new Factory<ExpiryPolicy>() {
    //            @Override
    //            public ExpiryPolicy create() {
    //                return policy;
    //            }
    //        });
    //        configuration.setCacheLoaderFactory(new Factory<CacheLoader<Integer, String>>() {
    //            @Override
    //            public CacheLoader<Integer, String> create() {
    //                return new CacheLoader<Integer, String>() {
    //                    @Override
    //                    public String load(Integer key) throws CacheLoaderException {
    //                        return "value".concat(key.toString());
    //                    }
    //
    //                    @Override
    //                    public Map<Integer, String> loadAll(Iterable<? extends Integer> keys) throws CacheLoaderException {
    //                        Map<Integer, String> values = new HashMap<>();
    //                        for (Integer key : keys) {
    //                            values.put(key, "value".concat(key.toString()));
    //                        }
    //                        return values;
    //                    }
    //                };
    //            }
    //        });
    //
    //        JimdbCache<Integer, String> cache = new JimdbCache<>("testCache", configuration, new JimdbCacheManager(new
    //                JimdbCacheProvider(), new Properties()));
    //        cache.workCluster = cluster;
    //        String value = cache.get(1);
    //        assert (value.equals("value1"));
    //        Set<Integer> keys = new HashSet<>();
    //        keys.collect(1);
    //        keys.collect(2);
    //        Map<Integer, String> ret = cache.getAll(keys);
    //        assert (ret.get(1).equals("value1"));
    //        assert (ret.get(2).equals("value2"));
    //    }

    @Override
    public V get(K key) {
        if (null == key)
            return null;
        byte[] keyBytes = generateKey(key);
        byte[] result;
        MonitorUtils.start(monitorKey);
        try {
            result = workCluster.get(keyBytes);
            if (this.flashExpireTime) {
                workCluster.expire(keyBytes, expireTime, expireTimeUnit);
            }
        } catch (Exception e) {
            MonitorUtils.error(monitorKey, e);
            throw e;
        } finally {
            MonitorUtils.end(monitorKey);
        }
        if (result != null) {
            return KryoUtils.deserialize(result);
        } else {
            return null;
        }
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys) {
        if (null == keys || keys.isEmpty()) {
            return new HashMap<>();
        }
        List<byte[]> keyBytes = new ArrayList<>();
        for (K key : keys) {
            keyBytes.add(generateKey(key));
        }
        List<Object> resultObjects;
        MonitorUtils.start(monitorKey);
        PipelineClient pipelineClient = workCluster.pipelineClient();
        try {
            for (byte[] keyByte : keyBytes) {
                pipelineClient.get(keyByte);
                if (flashExpireTime) {
                    pipelineClient.expire(keyByte, expireTime, expireTimeUnit);
                }
            }
            resultObjects = pipelineClient.flushAndReturnAll();
        } catch (Exception e) {
            logger.error("failed to get result from jimdb", e);
            MonitorUtils.error(monitorKey, e);
            return Collections.emptyMap();
        } finally {
            pipelineClient.close();
            MonitorUtils.end(monitorKey);
        }
        Map<K, V> result = new HashMap<>();
        int i = 0;
        for (K key : keys) {
            if (resultObjects.get(i) != null) {
                Object object = resultObjects.get(i);
                if (!(object instanceof Exception)) {
                    V value = KryoUtils.deserialize((byte[]) object);
                    result.put(key, value);
                }
            }
            ++i;
            if (flashExpireTime) {
                ++i;
            }
        }
        return result;
    }

    @Override
    public boolean containsKey(K key) {
        byte[] keyBytes = generateKey(key);
        MonitorUtils.start(monitorKey);
        try {
            return workCluster.exists(keyBytes);
        } catch (Exception e) {
            MonitorUtils.error(monitorKey, e);
            throw e;
        } finally {
            MonitorUtils.end(monitorKey);
        }
    }

    @Override
    public void loadAll(final Set<? extends K> keys, final boolean replaceExistingValues, final CompletionListener
            completionListener) {
    }

    @Override
    public void put(K key, V value) {
        if (null == key || null == value) {
            return;
        }
        byte[] keyBytes = generateKey(key);
        byte[] valueBytes = KryoUtils.serialize(value);
        MonitorUtils.start(monitorKey);
        try {
            workCluster.set(keyBytes, valueBytes);
            if (this.expireEnable)
                workCluster.expire(keyBytes, this.expireTime, this.expireTimeUnit);
        } catch (Exception e) {
            MonitorUtils.error(monitorKey, e);
            throw e;
        } finally {
            MonitorUtils.end(monitorKey);
        }
    }

    @Override
    public V getAndPut(K key, V value) {
        if (key == null || value == null) {
            return null;
        }
        V oldValue = null;
        if (this.containsKey(key)) {
            oldValue = this.get(key);
        }
        this.put(key, value);
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        Map<byte[], byte[]> serializedMap = serializeBatch(map);
        //        for (Cluster cluster : clusters) {
        MonitorUtils.start(monitorKey);
        PipelineClient pipelineClient = null;
        try {
            pipelineClient = workCluster.pipelineClient();
            for (Map.Entry<byte[], byte[]> entry : serializedMap.entrySet()) {
                pipelineClient.set(entry.getKey(), entry.getValue());
                if (this.expireEnable)
                    pipelineClient.expire(entry.getKey(), this.expireTime, this.expireTimeUnit);
            }
            pipelineClient.flushAndReturnAll();
        } catch (Exception e) {
            logger.error("put all to jimdb failed");
            MonitorUtils.error(monitorKey, e);
            throw e;
        } finally {
            if (pipelineClient != null)
                pipelineClient.close();
            MonitorUtils.end(monitorKey);
        }
        //        }
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        if (!this.containsKey(key)) {
            this.put(key, value);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean remove(K key) {
        byte[] keyBytes = generateKey(key);
        if (!workCluster.exists(keyBytes)) {
            return false;
        }
        workCluster.del(keyBytes);
        return true;
    }

    @Override
    public boolean remove(K key, V value) {
        V storedValue = this.get(key);
        if (!value.equals(storedValue)) {
            return false;
        }
        return this.remove(key);
    }

    @Override
    public V getAndRemove(K key) {
        V value = this.get(key);
        this.remove(key);
        return value;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        this.put(key, newValue);
        return true;
    }

    @Override
    public boolean replace(K key, V value) {
        this.put(key, value);
        return true;
    }

    @Override
    public V getAndReplace(K key, V value) {
        V oldValue = this.get(key);
        this.put(key, value);
        return oldValue;
    }

    @Override
    public void removeAll(Set<? extends K> keys) {
        List<byte[]> serializedKeys = new ArrayList<>();
        for (K key : keys) {
            serializedKeys.add(generateKey(key));
        }
        PipelineClient pipelineClient = null;
        try {
            pipelineClient = workCluster.pipelineClient();
            for (byte[] keyByte : serializedKeys) {
                pipelineClient.del(keyByte);
            }

            pipelineClient.flushAndReturnAll();
        } finally {
            if (pipelineClient != null)
                pipelineClient.close();
        }
    }

    @Override
    public void removeAll() {

    }

    @Override
    public void clear() {

    }

    @Override
    public <C extends Configuration<K, V>> C getConfiguration(Class<C> aClass) {
        return null;
    }

    @Override
    public <T> T invoke(K k, EntryProcessor<K, V, T> entryProcessor, Object... objects) throws EntryProcessorException {
        return null;
    }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> set, EntryProcessor<K, V, T> entryProcessor,
                                                         Object... objects) {
        return null;
    }

    @Override
    public String getName() {
        return this.cacheName;
    }

    @Override
    public CacheManager getCacheManager() {
        return this.cacheManager;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> aClass) {
        return null;
    }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {

    }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {

    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        throw new UnsupportedOperationException("jimdb can't support this method");
    }

    private <T> byte[] generateKey(T key) {
        return cachePrefix.concat(this.cacheName).concat("_").concat(key.toString()).getBytes();
    }

    private Map<byte[], byte[]> serializeBatch(Map<? extends K, ? extends V> map) {
        Map<byte[], byte[]> result = new HashMap<>();
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            byte[] key = this.generateKey(entry.getKey());
            byte[] value = KryoUtils.serialize(entry.getValue());
            result.put(key, value);
        }
        return result;
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
                    //                    CacheStats stats = this.cacheWrap.stats();
                    //                    this.statisticsLogger.error("{} cache status: size = {}, hitCount= {}, missCount =
                    // {}, " +
                    //                                    "hitRate = {}",
                    //                            this.cacheName, this.cacheWrap.size(), stats.hitCount(), stats.missCount(),
                    // stats.hitRate());
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
