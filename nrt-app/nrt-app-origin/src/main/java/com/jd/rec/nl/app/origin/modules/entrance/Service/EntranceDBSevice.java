package com.jd.rec.nl.app.origin.modules.entrance.Service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jd.jim.cli.PipelineClient;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.service.infrastructure.Jimdb;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import p13.nearline.NrtQrqmEntrance;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 专门为entrance服务的dbService
 *
 * @author wanlong3
 * @date 2018/11/29
 */
@Singleton
public class EntranceDBSevice {

    private static final Logger LOGGER = getLogger(EntranceDBSevice.class);

    private static final String dbKey = "nl_nrtup_";

    public static final String cacheName = "entranceDBSevice";

    private Map<KeyInfo, byte[]> modifyStatus = new ConcurrentHashMap<>();

    private final ReentrantLock saveLock = new ReentrantLock();

    private final Condition saveCondition = saveLock.newCondition();

    @Inject
    private Jimdb jimdb;

    public EntranceDBSevice() {
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                saveLock.lock();
                try {
                    saveCondition.await();
                    if (!modifyStatus.isEmpty()) {
                        Set<KeyInfo> keys = new HashSet<>(modifyStatus.keySet());
                        LOGGER.debug("save value:{}", keys.size());
                        savePoint(keys);
                    }
                } catch (InterruptedException e) {
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
                10, 10, TimeUnit.SECONDS);
    }

    /**
     * 保存状态到持久层
     *
     * @param keys
     */
    private void savePoint(Set<KeyInfo> keys) {

        Map<KeyInfo, byte[]> temp = new HashMap<>();
        for (KeyInfo keyInfo : keys) {
            temp.put(keyInfo, modifyStatus.remove(keyInfo));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(CLUSTER.values().length);
        Arrays.stream(CLUSTER.values()).forEach(cluster -> {
            executorService.submit(() -> {
                PipelineClient client = jimdb.getCluster(cluster).pipelineClient();
                try {
                    int count =0;
                    Iterator<KeyInfo> it = keys.iterator();
                    while (it.hasNext()) {
                        KeyInfo keyInfo = it.next();
                        byte[] value = temp.get(keyInfo);
                        client.hSet(keyInfo.getKey().getBytes(), keyInfo.getField().getBytes(), value);
                        client.expire(keyInfo.getKey().getBytes(), keyInfo.getTtl().getSeconds(), TimeUnit.SECONDS);
                        count ++;
                        if(count>100){
                            client.flushAndReturnAll();
                            count = 0;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                } finally {
                    client.close();
                }
            });
        });
    }

    /**
     * 判断是否提前进入savepoint
     */
    private void checkpoint() {
        if (modifyStatus.size() >= 500) {
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
     * 得到contentId的方法
     *
     * @param uid
     * @param field
     * @return
     * @throws InvalidProtocolBufferException
     */
    public Map<Long, Set<Long>> getPoolToContentIds(String uid, String field){
        String getKey = generateKey(uid, field);
        byte[] value = this.hGetWithCache(getKey, generateKey(uid), field);
        return deserializationPoolToContentIds(value);
    }

    /**
     * 反序列化pool对应着的contentIds
     *
     * @param values
     * @return
     * @throws InvalidProtocolBufferException
     */
    public Map<Long, Set<Long>> deserializationPoolToContentIds(byte[] values){
        Map<Long, Set<Long>> poolToContents = new HashMap<>();
        if (values != null && values.length != 0) {
            NrtQrqmEntrance.NrtEntranceProto nrtEntrance;
            try {
                nrtEntrance = NrtQrqmEntrance.NrtEntranceProto.parseFrom(values);
            } catch (InvalidProtocolBufferException e) {
                return poolToContents;
            }

            List<NrtQrqmEntrance.RecalledItems> recalledItems = nrtEntrance.getRItemsList();
            for (NrtQrqmEntrance.RecalledItems recalledItem : recalledItems) {
                long channelId = recalledItem.getChannelId();
                List<NrtQrqmEntrance.ItemsWithRule> itemsWithRules = recalledItem.getIRuleList();

                Set<Long> items = poolToContents.get(channelId);
                if (items == null) {
                    items = new LinkedHashSet<>();
                }
                for (NrtQrqmEntrance.ItemsWithRule itemsWithRule : itemsWithRules) {
                    long skuid = itemsWithRule.getSkuId();
                    items.add(skuid);
                }
                poolToContents.put(channelId, items);
            }
        }
        return poolToContents;
    }

    @CacheResult(cacheName = cacheName)
    protected byte[] hGetWithCache(@CacheKey String cacheKey, String key, String field) {
        //byte[] value = jimdb.hGetValue(CLUSTER.LF, key.getBytes(), field.getBytes());
        byte[] value = modifyStatus.get(new KeyInfo(key, field, null));
        if (value != null) {
            return value;
        }
        value = jimdb.hGetValue(CLUSTER.LF, key.getBytes(), field.getBytes());
        if (value == null || value.length == 0 || (value.length == 1 && value[0] == 0)) {
            return null;
        }
        return value;
    }

    /**
     * 将pool对应着contentId存储进去
     *
     * @param uid
     * @param field
     * @param value
     */
    public void setPoolToContentIds(String uid, String field, Map<Long, Set<Long>> value) {
        String getKey = generateKey(uid, field);
        byte[] valueBytes = serializeRecalledContent(uid, value);
        this.hSetToCache(getKey, generateKey(uid), field, valueBytes);

    }

    /**
     * 内容进行序列化操作
     *
     * @param uid
     * @param recalledItemResult
     * @return
     */
    private byte[] serializeRecalledContent(String uid, Map<Long, Set<Long>> recalledItemResult) {
        NrtQrqmEntrance.NrtEntranceProto.Builder nep = NrtQrqmEntrance.NrtEntranceProto.newBuilder();
        nep.setUid(uid);
        for (Map.Entry<Long, Set<Long>> entry : recalledItemResult.entrySet()) {
            NrtQrqmEntrance.RecalledItems.Builder ri = NrtQrqmEntrance.RecalledItems.newBuilder();
            Long channelID = entry.getKey();
            ri.setChannelId(channelID);
            Set<Long> itemMap = entry.getValue();
            for (long sku : itemMap) {
                NrtQrqmEntrance.ItemsWithRule.Builder ir = NrtQrqmEntrance.ItemsWithRule.newBuilder();
                ir.setSkuId(sku);
                ri.addIRule(ir);
            }
            nep.addRItems(ri);
        }
        NrtQrqmEntrance.NrtEntranceProto result = nep.build();
        return result.toByteArray();
    }

    @CachePut(cacheName = cacheName)
    protected void hSetToCache(@CacheKey String cacheKey, String getKey, String field, @CacheValue byte[] value) {
        modifyStatus.put(new KeyInfo(getKey, field, Duration.ofHours(168)), value);
        checkpoint();
//        ExecutorService executorService = Executors.newFixedThreadPool(CLUSTER.values().length);
//        Arrays.stream(CLUSTER.values()).forEach(cluster -> {
//            executorService.submit(() -> {
//              jimdb.hSetValue(cluster, getKey.getBytes(), field.getBytes(), value, Duration.ofHours(168));
//            });
//        });
    }


    /**
     * 得到pool对应的cid3和skus
     *
     * @param uid
     * @param field
     * @return
     * @throws InvalidProtocolBufferException
     */
    public Map<Long, Map<Integer, Set<Long>>> getPoolToCid3ToSkus(String uid, String field){
        String getKey = generateKey(uid, field);
        byte[] value = this.hGetWithCache(getKey, generateKey(uid), field);
        return deserializationPoolToCid3ToSkus(value);
    }


    /**
     * 反序列化pool对应的cid3和skus
     *
     * @param values
     * @return
     * @throws InvalidProtocolBufferException
     */
    public Map<Long, Map<Integer, Set<Long>>> deserializationPoolToCid3ToSkus(byte[] values) {
        Map<Long, Map<Integer, Set<Long>>> recalledItemResult = new HashMap<>();
        if (values != null && values.length != 0) {
            NrtQrqmEntrance.NrtEntranceProto nrtEntrance;
            try {
                nrtEntrance = NrtQrqmEntrance.NrtEntranceProto.parseFrom(values);
            } catch (InvalidProtocolBufferException e) {
                return recalledItemResult;
            }

            List<NrtQrqmEntrance.RecalledItems> recalledItems = nrtEntrance.getRItemsList();
            for (NrtQrqmEntrance.RecalledItems recalledItem : recalledItems) {
                long channelId = recalledItem.getChannelId();
                List<NrtQrqmEntrance.ItemsWithRule> itemsWithRules = recalledItem.getIRuleList();

                Map<Integer, Set<Long>> cid3ToSkus = recalledItemResult.get(channelId);
                if (cid3ToSkus == null) {
                    cid3ToSkus = new LinkedHashMap<>();
                }
                for (NrtQrqmEntrance.ItemsWithRule itemsWithRule : itemsWithRules) {
                    long skuid = itemsWithRule.getSkuId();
                    int cid3 = itemsWithRule.getCid3();
                    Set<Long> skus = cid3ToSkus.get(cid3);
                    if (skus == null) {
                        skus = new LinkedHashSet<>();
                    }
                    skus.add(skuid);
                    cid3ToSkus.put(cid3, skus);
                }
                recalledItemResult.put(channelId, cid3ToSkus);
            }
        }
        return recalledItemResult;
    }


    /**
     * 将pool对应着cid3和skus存储进去
     *
     * @param uid
     * @param field
     * @param value
     */
    public void setPoolToCid3ToSkus(String uid, String field, Map<Long, Map<Integer, Set<Long>>> value) {
        String getKey = generateKey(uid, field);
        byte[] valueBytes = serializeRecalledSku(uid, value);
        this.hSetToCache(getKey, generateKey(uid), field, valueBytes);

    }


    /**
     * 对pool对应着的cid3对应着的skus进行序列化操作
     *
     * @param uid
     * @param recalledItemResult
     * @return
     */
    public byte[] serializeRecalledSku(String uid, Map<Long, Map<Integer, Set<Long>>> recalledItemResult) {
        NrtQrqmEntrance.NrtEntranceProto.Builder nep = NrtQrqmEntrance.NrtEntranceProto.newBuilder();
        nep.setUid(uid);
        for (Map.Entry<Long, Map<Integer, Set<Long>>> entry : recalledItemResult.entrySet()) {
            NrtQrqmEntrance.RecalledItems.Builder ri = NrtQrqmEntrance.RecalledItems.newBuilder();
            Long channelID = entry.getKey();
            ri.setChannelId(channelID);
            Map<Integer, Set<Long>> cid3ToSkus = entry.getValue();
            for (Map.Entry<Integer, Set<Long>> cid3ToSkusEntry : cid3ToSkus.entrySet()) {
                int cid3 = cid3ToSkusEntry.getKey();
                Set<Long> skus = cid3ToSkusEntry.getValue();
                for (Long sku : skus) {
                    NrtQrqmEntrance.ItemsWithRule.Builder ir = NrtQrqmEntrance.ItemsWithRule.newBuilder();
                    ir.setSkuId(sku);
                    ir.setCid3(cid3);
                    ri.addIRule(ir);
                }
            }
            nep.addRItems(ri);
        }
        NrtQrqmEntrance.NrtEntranceProto result = nep.build();
        return result.toByteArray();
    }

    protected static String generateKey(String... keyFields) {
        String key = StringUtils.join(keyFields, "-");
        return dbKey.concat(key);
    }


    static class KeyInfo {
        String key;
        String field;
        Duration ttl;

        public KeyInfo(String key, String field, Duration ttl) {
            this.key = key;
            this.field = field;
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

        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) + (field == null ? 0 : field.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof KeyInfo)) {
                return false;
            }
            return (key == null ? key == ((KeyInfo) obj).getKey() : key.equals(((KeyInfo) obj).getKey()))
                    && (field == null ? field == ((KeyInfo) obj).getField() : field.equals(((KeyInfo) obj).getField()));
        }

        @Override
        public String toString() {
            return "KeyInfo{" +
                    "key='" + key + '\'' +
                    ", field='" + field + '\'' +
                    ", ttl=" + ttl +
                    '}';
        }
    }
}
