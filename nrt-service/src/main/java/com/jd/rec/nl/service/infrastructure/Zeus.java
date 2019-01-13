package com.jd.rec.nl.service.infrastructure;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.core.infrastructure.domain.ThriftParameter;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.si.zeus.*;
import com.jd.zeus.lib.client.ZeusClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author linmx
 * @date 2018/7/19
 */
@Singleton
public class Zeus implements BaseInfrastructure {

    private static final String ID = "search_man";
    private static final String PASSWORD = "search_man";
    private static final String SERVICE_NAME = "portal_pin";
    private static final Logger LOGGER = LoggerFactory.getLogger(Zeus.class);
    private int maxRecords = 1;
    private String type = "df";
    private volatile Map<CLUSTER, ZeusClient> clients = new HashMap<>();

    @Inject
    @ENV("monitor.zeus_key")
    private String monitorKey;

    protected Zeus() {
    }

    @Inject
    public Zeus(@ENV("zeus") ThriftParameter zeusConfig) {
        //        LOGGER.debug(zeusConfig.toString());
        clients.put(CLUSTER.LF, new ZeusClient(zeusConfig.getConnection(CLUSTER.LF), zeusConfig.getJdns()));
        clients.put(CLUSTER.MJQ, new ZeusClient(zeusConfig.getConnection(CLUSTER.MJQ), zeusConfig.getJdns()));
        clients.put(CLUSTER.HT, new ZeusClient(zeusConfig.getConnection(CLUSTER.HT), zeusConfig.getJdns()));
        LOGGER.debug(clients.toString());
    }

    public Map<String, ByteBuffer> getUserModels(String uid, Collection<String> models) {
        LookupRequest lookupRequest = new LookupRequest();
        lookupRequest.setHeader(buildRequestHeader(uid));
        lookupRequest.setData(buildLookupRequestData(models));
        lookupRequest.setClientId(CLIENT_ID);
        LookupResponse lookupResponse = this.lookup(lookupRequest);
        Map<String, ByteBuffer> byteBufferMap = getByteBufferMap(lookupResponse);
        return byteBufferMap;
    }

    private LookupResponse lookup(LookupRequest lookupRequest) {
        MonitorUtils.start(monitorKey);
        LookupResponse result = null;
        try {
            Random random = new Random();
            ZeusClient client = clients.get(CLUSTER.values()[random.nextInt(3)]);
            result = client.Lookup(lookupRequest);
        } catch (Exception e) {
            MonitorUtils.error(monitorKey, e);
            throw new EnvironmentException("zeus", e);
        } finally {
            MonitorUtils.end(monitorKey);
        }
        return result;
    }


    private RequestHeader buildRequestHeader(String uid) {
        RequestHeader header = new RequestHeader();
        UserKeyType user = new UserKeyType();
        user.setId(uid);
        user.setServiceName(SERVICE_NAME);
        header.setUser(user);
        header.setAppId(ID);
        header.setAppPassword(PASSWORD);
        return header;
    }


    private List<LookupRequestData> buildLookupRequestData(Collection<String> models) {
        List<LookupRequestData> list = new ArrayList<>();
        if (models.size() == 0) {
            return list;
        }
        for (String model : models) {
            if (Strings.isNullOrEmpty(model)) {
                continue;
            }
            LookupRequestData data = new LookupRequestData();
            data.setType(type);
            data.setSubtype(model);
            data.setMaxRecords(maxRecords);
            list.add(data);
        }
        return list;
    }

    private Map<String, ByteBuffer> getByteBufferMap(LookupResponse response) {
        if (response == null || response.getData() == null) {
            return ImmutableMap.of();
        }
        return response.getData().stream()
                .filter(data -> data.getResponseCode().equals(DataResponseCode.DRC_SUCCESS))
                .filter(data -> data.getItem() != null && data.getItem().size() > 0)
                .map(data -> data.getItem().get(0))
                .collect(Collectors.toMap(
                        LookupResponseDataItem::getSubtype,
                        item -> ByteBuffer.wrap(item.getValue())
                ));
    }
}
