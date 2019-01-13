package com.jd.rec.nl.service.modules.db.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.jim.cli.PipelineClient;
import com.jd.rec.nl.core.cache.annotation.CacheKVPairs;
import com.jd.rec.nl.core.cache.annotation.CacheKeys;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.core.utils.KryoUtils;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.rec.nl.service.infrastructure.Jimdb;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/15
 */
@Singleton
public class DBService {

    public static final String cacheName = "dbService";

    private static final Logger LOGGER = getLogger(DBService.class);

    private static final String dbKey = "nrt_db_";
    private final ReentrantLock saveLock = new ReentrantLock();

    //    @Inject
    //    private DBProxy dbProxy;
    private final Condition saveCondition = saveLock.newCondition();
    @Inject
    @ENV("dbservice.default_live_time")
    private Duration defaultLiveTime;
    @Inject
    private Jimdb jimdb;
    @Inject
    @ENV("dbservice.checkpoint_size")
    private int checkpointSize;
    /**
     * 待保存的状态
     */
    private Map<KeyInfo, byte[]> modifiedStates = new ConcurrentHashMap<>();

    private String jimdbCount = "jimDBCount";

    private String dbServiceCount = "dbServiceCount";
    @Inject
    @ENV("dbservice.batchSize")
    private int batchSize;
    @Inject
    @ENV("dbservice.interval_time")
    private long time;

    @Inject
    public DBService(@ENV("dbservice.checkpoint_interval") Duration checkpointInterval) {

        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                saveLock.lock();
                try {
                    saveCondition.await();
                    if (!modifiedStates.isEmpty()) {
                        Set<KeyInfo> keys = new HashSet<>(modifiedStates.keySet());
                        //                        LOGGER.debug("save point:\n{}", keys);
                        LOGGER.debug("save value:{}", keys.size());
                        savePoint(keys);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    LOGGER.error(e.getMessage(), e);
                } finally {
                    saveLock.unlock();
                }
            }
        });

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                () -> {
                    if (saveLock.tryLock()) {
                        try {
                            saveCondition.signal();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            saveLock.unlock();
                        }
                    }
                },
                checkpointInterval.getSeconds(), checkpointInterval.getSeconds(), TimeUnit.SECONDS);
    }

    protected static String generateKey(String... keyFields) {
        String key = StringUtils.join(keyFields, "-");
        return dbKey.concat(key);
    }

    /**
     * 保存状态到持久层
     *
     * @param keys
     */
    private void savePoint(Set<KeyInfo> keys) {
        int count = 0;
        PipelineClient client = jimdb.getCluster(CLUSTER.HT).pipelineClient();
        Iterator<KeyInfo> it = keys.iterator();
        try {
            while (it.hasNext()) {
                MonitorUtils.start(jimdbCount);
                KeyInfo keyInfo = it.next();
                try {
                    byte[] value = modifiedStates.remove(keyInfo);
                    if (keyInfo.type == KeyInfo.Type.normal) {
                        client.set(keyInfo.getKey().getBytes(), value);
                    } else if (keyInfo.type == KeyInfo.Type.hash) {
                        client.hSet(keyInfo.getKey().getBytes(), keyInfo.getField().getBytes(), value);
                    }
                    client.expire(keyInfo.getKey().getBytes(), keyInfo.getTtl().getSeconds(), TimeUnit.SECONDS);
                    count++;
                    if (count > batchSize) {
                        client.flushAndReturnAll();
                        client = jimdb.getCluster(CLUSTER.HT).pipelineClient();
                        Thread.sleep(time);
                        count = 0;
                    }
                } catch (Exception e) {
                    MonitorUtils.error(jimdbCount, e);
                } finally {
                    MonitorUtils.end(jimdbCount);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            client.close();
        }

//        PipelineClient client = jimdb.getCluster(CLUSTER.HT).pipelineClient();
//        try {
//            keys.stream().forEach(keyInfo -> {
//
//                MonitorUtils.start(jimdbCount);
//                try {
//                    byte[] value = modifiedStates.remove(keyInfo);
//                    if (keyInfo.type == KeyInfo.Type.normal) {
//                        client.set(keyInfo.getKey().getBytes(), value);
//                    } else if (keyInfo.type == KeyInfo.Type.hash) {
//                        client.hSet(keyInfo.getKey().getBytes(), keyInfo.getField().getBytes(), value);
//                    }
//                    client.expire(keyInfo.getKey().getBytes(), keyInfo.getTtl().getSeconds(), TimeUnit.SECONDS);
//                } catch (Exception e) {
//                    MonitorUtils.error(jimdbCount, e);
//                } finally {
//                    MonitorUtils.end(jimdbCount);
//
//                }
//
//            });
//            client.flushAndReturnAll();
//        } catch (Exception e) {
//            LOGGER.error(e.getMessage(), e);
//
//        } finally {
//            client.close();
//
//        }
    }

    /**
     * 判断是否提前进入savepoint
     */
    private void checkpoint() {
        if (modifiedStates.size() >= checkpointSize) {
            if (saveLock.tryLock()) {
                try {
                    saveCondition.signal();
                } finally {
                    saveLock.unlock();
                }
            }
        }
    }


    /**
     * 保存数据,使用env配置的默认失效时间
     *
     * @param namespace 数据存储空间,逻辑上的表
     * @param key       主键
     * @param value     值
     * @param <T>
     * @throws Exception
     */
    public <T> void save(String namespace, String key, T value) throws Exception {
        // 如果不配置缓存失效时间,则默认是7天失效,由于底层使用jimdb作为持久层,不允许无限期存在的数据
        save(namespace, key, value, defaultLiveTime);
    }

    /**
     * 保存数据
     *
     * @param namespace  数据存储空间,逻辑上的表
     * @param key        主键
     * @param value      值
     * @param timeToLive 有效时间长度
     * @param <T>
     * @throws Exception
     */
    public <T> void save(String namespace, String key, T value, Duration timeToLive) throws Exception {

        MonitorUtils.start(dbServiceCount);
        try {
            String actKey = generateKey(namespace, key);
            byte[] valueBytes = KryoUtils.serialize(value);
            this.setToCache(actKey, valueBytes, timeToLive);
            //        jimdb.setValue(CLUSTER.HT, actKey.getBytes(), valueBytes, timeToLive);
        } catch (Exception e) {
            MonitorUtils.error(dbServiceCount, e);
        } finally {
            MonitorUtils.end(dbServiceCount);
        }
    }

    /**
     * 批量保存数据
     *
     * @param namespace
     * @param values
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> void batchSave(final String namespace, Map<String, T> values, Duration timeToLive) throws Exception {

        MonitorUtils.start(dbServiceCount);
        try {
            Map<String, byte[]> bValues = new HashMap<>();
            values.forEach((key, value) -> bValues.put(generateKey(namespace, key), KryoUtils.serialize(value)));
            this.batchSetToCache(bValues, timeToLive);
            //        jimdb.setValues(CLUSTER.HT, bValues);
        } catch (Exception e) {
            MonitorUtils.error(dbServiceCount, e);
        } finally {
            MonitorUtils.end(dbServiceCount);
        }
    }

    /**
     * 保存数据
     *
     * @param namespace  数据存储空间,逻辑上的表
     * @param key        主键
     * @param field
     * @param value      值
     * @param timeToLive 有效时间长度
     * @param <T>
     * @throws Exception
     */
    public <T> void save(String namespace, String key, String field, T value, Duration timeToLive) throws Exception {

        MonitorUtils.start(dbServiceCount);


        try {
            String dbKey = generateKey(namespace, key);
            String cacheKey = generateKey(namespace, key, field);
            byte[] valueBytes = KryoUtils.serialize(value);
            this.hSetToCache(cacheKey, dbKey, field, valueBytes, timeToLive);
            //        jimdb.hSetValue(CLUSTER.HT, dbKey.getBytes(), field.getBytes(), valueBytes, timeToLive);
        } catch (Exception e) {
            MonitorUtils.error(dbServiceCount, e);
        } finally {
            MonitorUtils.end(dbServiceCount);
        }
    }

    /**
     * 查询数据
     *
     * @param namespace
     * @param key
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> T query(String namespace, String key) throws Exception {

        MonitorUtils.start(dbServiceCount);
        byte[] value = null;
        try {
            String actKey = generateKey(namespace, key);
            value = this.getWithCache(actKey);
            //jimdb.getValue(CLUSTER.HT, generateKey(namespace, key).getBytes());
        } catch (Exception e) {
            MonitorUtils.error(dbServiceCount, e);
        } finally {
            MonitorUtils.end(dbServiceCount);
        }
        return value == null ? null : KryoUtils.deserialize(value);
    }

    /**
     * 查询数据
     *
     * @param namespace
     * @param key
     * @param field
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> T query(String namespace, String key, String field) throws Exception {

        MonitorUtils.start(dbServiceCount);
        byte[] value = null;
        try {
            String cacheKey = generateKey(namespace, key, field);
            String dbKey = generateKey(namespace, key);
            value = this.hGetWithCache(cacheKey, dbKey, field);
            //jimdb.hGetValue(CLUSTER.HT, generateKey(namespace, key).getBytes(), field.getBytes());

        } catch (Exception e) {
            MonitorUtils.error(dbServiceCount, e);
        } finally {
            MonitorUtils.end(dbServiceCount);
        }
        return value == null ? null : KryoUtils.deserialize(value);
    }

    /**
     * 批量查询数据
     *
     * @param namespace
     * @param keys
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> Map<String, T> batchQuery(final String namespace, Collection<String> keys) throws Exception {

        MonitorUtils.start(dbServiceCount);

        Map<String, String> bKeys = new HashMap<>();
        Map<String, byte[]> values = new HashMap<>();
        try {
            keys.stream().forEach(key -> bKeys.put(generateKey(namespace, key), key));
            values = this.batchGetWithCache(new HashSet<>(bKeys.keySet()));
            //jimdb.getValues(CLUSTER.HT, new ArrayList<>(bKeys.keySet()));
            if (values.size() == 0) {
                return new HashMap<>();
            }
        } catch (Exception e) {
            MonitorUtils.error(dbServiceCount, e);
        } finally {
            MonitorUtils.end(dbServiceCount);
        }
        Map<String, T> ret = new HashMap<>();
        values.forEach((key, value) -> ret.put(bKeys.get(key), value == null ? null : KryoUtils.deserialize(value)));
        return ret;
    }

    @CachePut(cacheName = cacheName)
    protected void setToCache(@CacheKey String key, @CacheValue byte[] value, Duration ttl) {
        modifiedStates.put(new KeyInfo(key, null, KeyInfo.Type.normal, ttl), value);
        checkpoint();
    }

    @CachePut(cacheName = cacheName)
    protected void hSetToCache(@CacheKey String cacheKey, String dbKey, String field, @CacheValue byte[] value, Duration ttl) {
        modifiedStates.put(new KeyInfo(dbKey, field, KeyInfo.Type.hash, ttl), value);
        checkpoint();
    }

    @CachePut(cacheName = cacheName)
    protected void batchSetToCache(@CacheKVPairs Map<String, byte[]> keyValues, Duration ttl) {
        keyValues.forEach((key, value) -> modifiedStates.put(new KeyInfo(key, null, KeyInfo.Type.normal, ttl), value));
        checkpoint();
    }

    @CacheResult(cacheName = cacheName)
    protected byte[] getWithCache(@CacheKey String key) {
        byte[] value = modifiedStates.get(new KeyInfo(key, null, KeyInfo.Type.normal, null));
        MonitorUtils.start(jimdbCount);
        try {
            if (value != null) {
                return value;
            }
            value = jimdb.getValue(CLUSTER.HT, key.getBytes());
            if (value == null || value.length == 0 || (value.length == 1 && value[0] == 0)) {
                return null;
            }
        } catch (Exception e) {
            MonitorUtils.error(jimdbCount, e);
        } finally {
            MonitorUtils.end(jimdbCount);
        }
        return value;
    }

    @CacheResult(cacheName = cacheName)
    protected byte[] hGetWithCache(@CacheKey String cacheKey, String dbKey, String field) {
        byte[] value = modifiedStates.get(new KeyInfo(dbKey, field, KeyInfo.Type.hash, null));
        MonitorUtils.start(jimdbCount);
        try {
            if (value != null) {
                return value;
            }
            value = jimdb.hGetValue(CLUSTER.HT, dbKey.getBytes(), field.getBytes());
            if (value == null || value.length == 0 || (value.length == 1 && value[0] == 0)) {
                return null;
            }
        } catch (Exception e) {
            MonitorUtils.error(jimdbCount, e);
        } finally {
            MonitorUtils.end(jimdbCount);
        }
        return value;
    }

    @CacheResult(cacheName = cacheName)
    protected Map<String, byte[]> batchGetWithCache(@CacheKeys Collection<String> keys) {
        List<String> noStoredKeys = new ArrayList<>();
        Map<String, byte[]> values = new HashMap<>();
        MonitorUtils.start(jimdbCount);
        try {
            keys.forEach(key -> {
                byte[] value = modifiedStates.get(new KeyInfo(key, null, KeyInfo.Type.normal, null));
                if (value != null) {
                    values.put(key, value);
                } else {
                    noStoredKeys.add(key);
                }
            });
        } catch (Exception e) {
            MonitorUtils.error(jimdbCount, e);
        } finally {
            MonitorUtils.end(jimdbCount);
        }
        values.putAll(jimdb.getValues(CLUSTER.HT, noStoredKeys));
        return values;
    }

    static class KeyInfo {
        String key;
        String field;
        Duration ttl;
        Type type = Type.normal;

        public KeyInfo(String key, String field, Type type, Duration ttl) {
            this.key = key;
            this.field = field;
            this.type = type;
            this.ttl = ttl;
        }

        public Duration getTtl() {
            return ttl;
        }

        public String getKey() {
            return key;
        }

        public String getField() {
            return field;
        }

        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) + (field == null ? 0 : field.hashCode()) + type.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof KeyInfo)) {
                return false;
            }
            return (key == null ? key == ((KeyInfo) obj).getKey() : key.equals(((KeyInfo) obj).getKey()))
                    && (field == null ? field == ((KeyInfo) obj).getField() : field.equals(((KeyInfo) obj).getField()))
                    && type == ((KeyInfo) obj).getType();
        }

        @Override
        public String toString() {
            return "KeyInfo{" +
                    "key='" + key + '\'' +
                    ", field='" + field + '\'' +
                    ", ttl=" + ttl +
                    ", type=" + type +
                    '}';
        }

        static enum Type {
            normal, hash;
        }
    }
}
