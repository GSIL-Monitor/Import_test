package com.jd.rec.nl.service.infrastructure;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.jd.commons.soa.cluster.ZookeeperMonitoredGrpcCluster;
import com.jd.commons.soa.cluster.container.RoundRobinCluster;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.exception.FrameworkException;
import com.jd.rec.nl.core.exception.WrongConfigException;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.core.infrastructure.domain.ThriftParameter;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.uds.proxy.*;
import org.slf4j.Logger;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/15
 */
@Singleton
public class DBProxy implements BaseInfrastructure {


    private static final Logger LOGGER = getLogger(DBProxy.class);

    private static final Charset charset = Charsets.UTF_8;
    private static Duration timeOut = Duration.ofMillis(200);
    private final Map<CLUSTER, ZookeeperMonitoredGrpcCluster<ProxyGrpc.ProxyFutureStub, String>> clusters = new HashMap<>();
    @Inject
    @ENV("dbproxy.dbname")
    private String dbName;
    @Inject
    @ENV("monitor.dbproxy_key")
    private String monitorKey;

    protected DBProxy() {
    }

    @Inject
    public DBProxy(@ENV("dbproxy") ThriftParameter parameter, @ENV("dbproxy.session_timeout") int sessionTimeout) {
        try {
            clusters.put(CLUSTER.LF, new ZookeeperMonitoredGrpcCluster<>(parameter.getConnection(CLUSTER.LF), sessionTimeout,
                    parameter.getJdns(), charset, ProxyGrpc.ProxyFutureStub.class, new RoundRobinCluster<>()));

            clusters.put(CLUSTER.MJQ, new ZookeeperMonitoredGrpcCluster<>(parameter.getConnection(CLUSTER.MJQ), sessionTimeout,
                    parameter.getJdns(), charset, ProxyGrpc.ProxyFutureStub.class, new RoundRobinCluster<>()));

            clusters.put(CLUSTER.HT, new ZookeeperMonitoredGrpcCluster<>(parameter.getConnection(CLUSTER.HT), sessionTimeout,
                    parameter.getJdns(), charset, ProxyGrpc.ProxyFutureStub.class, new RoundRobinCluster<>()));

        } catch (Exception e) {
            LOGGER.error("dbproxy client initialization error", e.getMessage());
            throw new WrongConfigException(e);
        }
    }

    /**
     * 查询不同的表数据(支持负载均衡)
     *
     * @param keys
     * @param tableName
     * @return
     */
    public Map<String, byte[]> query(List<String> keys, String tableName) {
        List<ByteString> inKeys = new ArrayList<>();
        keys.forEach(key -> inKeys.add(ByteString.copyFrom(String.valueOf(key), charset)));
        MultiGetRequest request = MultiGetRequest.newBuilder()
                .setDbName(tableName)
                .setClient(CLIENT_ID)
                .addAllKey(inKeys).build();
        Random random = new Random();
        ZookeeperMonitoredGrpcCluster<ProxyGrpc.ProxyFutureStub, String> cluster = this.clusters.get(CLUSTER.values()[random
                .nextInt(this.clusters.size())]);
        MultiGetResponse response;
        try {
            MonitorUtils.start(monitorKey);
            response = cluster.get("").getStub().multiGet(request).get(timeOut.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug("MultiSetResponse error", e.getMessage());
            throw new EnvironmentException("dbproxy", e);
        } catch (ExecutionException e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug("MultiSetResponse error", e.getMessage());
            throw new EnvironmentException("dbproxy", e);
        } catch (TimeoutException e) {
            throw new EnvironmentException("dbproxy", e);
        } finally {
            MonitorUtils.end(monitorKey);
        }

        if (!response.getError().isEmpty()) {
            LOGGER.debug("MultiGetResponse error: " + response.getError());
            throw new EnvironmentException("dbproxy", response.getError(), new Exception(response.getError()));
        }

        List<ByteString> values = response.getValueList();
        if (values.size() != keys.size()) {
            String message = "response size != request size";
            throw new EnvironmentException("dbproxy", message);
        }
        Map<String, byte[]> ret = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            ByteString value = values.get(i);
            byte[] retValue = (value == null || value.isEmpty()
                    || (value.toByteArray().length == 1 && value.toByteArray()[0] == 0)) ? null : value.toByteArray();
            ret.put(key, retValue);
        }
        return ret;
    }

    public void setValue(CLUSTER clusterType, String key, byte[] value, Duration ttl) throws Exception {

        MultiSetRequest.Builder builder = MultiSetRequest.newBuilder()
                .setDbName(dbName)
                .setClient(CLIENT_ID)
                .addKey(KeyValue.newBuilder().setKey(ByteString.copyFrom(key, charset)).setValue(ByteString.copyFrom(value)));

        if (ttl != null) {
            builder.setTtl(ttl.getSeconds());
        } else {
            throw new FrameworkException("jimdb require ttl!!");
        }

        MultiSetRequest request = builder.build();

        ZookeeperMonitoredGrpcCluster<ProxyGrpc.ProxyFutureStub, String> cluster = this.clusters.get(clusterType);
        MultiSetResponse response;
        try {
            MonitorUtils.start(monitorKey);
            response = cluster.get("").getStub().multiSet(request).get(timeOut.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug("MultiSetResponse error", e.getMessage());
            throw new EnvironmentException("dbproxy", e);
        } catch (ExecutionException e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug("MultiSetResponse error", e.getMessage());
            throw new EnvironmentException("dbproxy", e);
        } catch (TimeoutException e) {
            throw new EnvironmentException("dbproxy", e);
        } finally {
            MonitorUtils.end(monitorKey);
        }

        if (!response.getError().isEmpty()) {
            LOGGER.debug("MultiSetResponse error: " + response.getError());
            throw new EnvironmentException("dbproxy", response.getError(), new Exception(response.getError()));
        }
    }

    public byte[] getValue(CLUSTER clusterType, String key) throws Exception {
        byte[] result = new byte[0];
        MultiGetResponse response;
        try {
            MonitorUtils.start(monitorKey);
            response = clusters.get(clusterType).get("").getStub().multiGet(MultiGetRequest.newBuilder()
                    .setDbName(dbName)
                    .setClient(CLIENT_ID)
                    .addKey(ByteString.copyFrom(key, charset))
                    .build()).get(timeOut.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug("MultiGetResponse error", e.getMessage());
            throw new EnvironmentException("dbproxy", e);
        } catch (ExecutionException e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug("MultiGetResponse error", e.getMessage());
            throw new EnvironmentException("dbproxy", e);
        } catch (TimeoutException e) {
            throw new EnvironmentException("dbproxy", e);
        } finally {
            MonitorUtils.end(monitorKey);
        }

        if (!response.getError().isEmpty()) {
            LOGGER.debug("MultiGetResponse error: " + response.getError());
            throw new EnvironmentException("dbproxy", response.getError(), new Exception(response.getError()));
        }

        ByteString byteString = response.getValue(0);
        if (byteString == null || byteString.isEmpty())
            return result;

        return byteString.toByteArray();
    }

    public void hSetValue(CLUSTER clusterType, String key, String field, byte[] value, Duration ttl) throws Exception {

        MultiHashSetRequest.Builder builder = MultiHashSetRequest.newBuilder()
                .setDbName(dbName)
                .setClient(CLIENT_ID)
                .setKey(ByteString.copyFrom(key, charset))
                .addColumn(KeyValue.newBuilder().setKey(ByteString.copyFrom(field, charset)).setValue(ByteString.copyFrom
                        (value)));

        if (ttl != null) {
            builder.setTtl(ttl.getSeconds());
        } else {
            throw new FrameworkException("jimdb require ttl!!");
        }

        MultiHashSetRequest request = builder.build();

        ZookeeperMonitoredGrpcCluster<ProxyGrpc.ProxyFutureStub, String> cluster = this.clusters.get(clusterType);
        MultiSetResponse response;
        try {
            MonitorUtils.start(monitorKey);
            response = cluster.get("").getStub().multiHashSet(request).get(timeOut.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug("MultiSetResponse error", e.getMessage());
            throw new EnvironmentException("dbproxy", e);
        } catch (ExecutionException e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug("MultiSetResponse error", e.getMessage());
            throw new EnvironmentException("dbproxy", e);
        } catch (TimeoutException e) {
            throw new EnvironmentException("dbproxy", e);
        } finally {
            MonitorUtils.end(monitorKey);
        }

        if (!response.getError().isEmpty()) {
            LOGGER.debug("MultiSetResponse error: " + response.getError());
            throw new EnvironmentException("dbproxy", response.getError(), new Exception(response.getError()));
        }
    }


    public byte[] hGetValue(CLUSTER clusterType, String key, String field) throws Exception {
        byte[] result = new byte[0];
        MultiGetResponse response;
        try {
            MonitorUtils.start(monitorKey);
            response = clusters.get(clusterType).get("").getStub().multiHashGet(MultiHashGetRequest.newBuilder()
                    .setDbName(dbName)
                    .setClient(CLIENT_ID)
                    .setKey(ByteString.copyFrom(key, charset))
                    .addColumn(ByteString.copyFrom(field, charset))
                    .build()).get(timeOut.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug("MultiGetResponse error", e.getMessage());
            throw new EnvironmentException("dbproxy", e);
        } catch (ExecutionException e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug("MultiGetResponse error", e.getMessage());
            throw new EnvironmentException("dbproxy", e);
        } catch (TimeoutException e) {
            throw new EnvironmentException("dbproxy", e);
        } finally {
            MonitorUtils.end(monitorKey);
        }

        if (!response.getError().isEmpty()) {
            LOGGER.debug("MultiGetResponse error: " + response.getError());
            throw new EnvironmentException("dbproxy", response.getError(), new Exception(response.getError()));
        }

        ByteString byteString = response.getValue(0);
        if (byteString == null || byteString.isEmpty())
            return result;

        return byteString.toByteArray();
    }

    public ZookeeperMonitoredGrpcCluster<ProxyGrpc.ProxyFutureStub, String> getCluster(CLUSTER clusterType) {
        return clusters.get(clusterType);
    }

}
