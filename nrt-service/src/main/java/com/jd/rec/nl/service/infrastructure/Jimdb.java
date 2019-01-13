package com.jd.rec.nl.service.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.jim.cli.Cluster;
import com.jd.jim.cli.PipelineClient;
import com.jd.jim.cli.ReloadableJimClientFactory;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import com.jd.rec.nl.core.infrastructure.annotation.InfrastructureGet;
import com.jd.rec.nl.core.infrastructure.annotation.InfrastructureSet;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author linmx
 * @date 2018/6/25
 */
@Singleton
public class Jimdb implements BaseInfrastructure {

    @Inject
    @ENV("monitor.jimdb_key")
    public String monitorKey;

    /**
     * 集群
     * 内部使用时,LF用于缓存,HT用于nrt内部的数据持久层
     */
    private Map<CLUSTER, Cluster> clusters = new HashMap<>();

    protected Jimdb() {
    }

    @Inject
    public Jimdb(@ENV("jimdb.url_lf") String url_lf, @ENV("jimdb.url_mjq") String url_mjq,
                 @ENV("jimdb.url_ht") String url_ht) {
        ReloadableJimClientFactory factory = new ReloadableJimClientFactory();
        factory.setJimUrl(url_mjq);
        clusters.put(CLUSTER.MJQ, factory.getClient());

        factory = new ReloadableJimClientFactory();
        factory.setJimUrl(url_lf);
        clusters.put(CLUSTER.LF, factory.getClient());

        factory = new ReloadableJimClientFactory();
        factory.setJimUrl(url_ht);
        clusters.put(CLUSTER.HT, factory.getClient());
    }

    public Cluster getCluster(CLUSTER clusterType) {
        return this.clusters.get(clusterType);
    }

    public void setValue(CLUSTER clusterType, byte[] key, byte[] value, Duration ttl) {
        try {
            Cluster cluster = getCluster(clusterType);
            cluster.set(key, value);
            cluster.expire(key, ttl.getSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new EnvironmentException("jimdb", e);
        }
    }

    /**
     * 批量更新
     *
     * @param clusterType
     * @param values
     */
    public void setValues(CLUSTER clusterType, Map<byte[], byte[]> values, Duration ttl) {
        PipelineClient pipelineClient = null;
        try {
            pipelineClient = getCluster(clusterType).pipelineClient();
            final PipelineClient client = pipelineClient;
            values.forEach((key, value) -> {
                client.set(key, value);
                client.expire(key, ttl.getSeconds(), TimeUnit.SECONDS);
            });
            List<Object> resultObjects = pipelineClient.flushAndReturnAll();
            //            int i = 0;
            //            for (Object result : resultObjects) {
            //                if (result != null && !(result instanceof Exception)) {
            //                    results.put(keys.get(i), (byte[]) result);
            //                }
            //                i++;
            //            }
            //            return results;
        } catch (Exception e) {
            throw new EnvironmentException("jimdb", e);
        } finally {
            if (pipelineClient != null) {
                pipelineClient.close();
            }
        }
    }

    public byte[] getValue(CLUSTER clusterType, byte[] key) {
        try {
            return getCluster(clusterType).get(key);
        } catch (Exception e) {
            throw new EnvironmentException("jimdb", e);
        }
    }

    /**
     * 批量查询
     *
     * @param clusterType 集群
     * @param keys
     * @return
     */
    public Map<String, byte[]> getValues(CLUSTER clusterType, List<String> keys) {
        PipelineClient pipelineClient = null;
        try {
            pipelineClient = getCluster(clusterType).pipelineClient();
            final PipelineClient client = pipelineClient;
            keys.forEach(key -> client.get(key.getBytes()));
            List<Object> resultObjects = pipelineClient.flushAndReturnAll();
            Map<String, byte[]> results = new HashMap<>();
            int i = 0;
            for (Object result : resultObjects) {
                if (result != null && !(result instanceof Exception)) {
                    results.put(keys.get(i), (byte[]) result);
                } else {
                    results.put(keys.get(i), null);
                }
                i++;
            }
            return results;
        } catch (Exception e) {
            throw new EnvironmentException("jimdb", e);
        } finally {
            if (pipelineClient != null) {
                pipelineClient.close();
            }
        }
    }

    @InfrastructureSet
    public void hSetValue(CLUSTER clusterType, byte[] key, byte[] field, byte[] value, Duration ttl) {
        try {
            Cluster cluster = getCluster(clusterType);
            cluster.hSet(key, field, value);
            cluster.expire(key, ttl.getSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new EnvironmentException("jimdb", e);
        }
    }

    @InfrastructureGet
    public byte[] hGetValue(CLUSTER clusterType, byte[] key, byte[] field) {
        try {
            Cluster cluster = getCluster(clusterType);
            return cluster.hGet(key, field);
        } catch (Exception e) {
            throw new EnvironmentException("jimdb", e);
        }
    }

}
