package com.jd.rec.nl.service.infrastructure;

import com.google.inject.Inject;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.si.util.ThriftSerialization;
import com.jd.si.venus.core.CommenInfo;
import com.jd.si.venus.core.SkuAttributInfo;
import com.jd.si.venus.core.UserModelServer;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class UserModel implements BaseInfrastructure {

    public static final String COMMON_INFO_DB_NAME = "itemprofile-extend";
    public static final String SKU_ATTR_DB_NAME = "itemprofile";
    private static final Logger LOGGER = getLogger(UserModel.class);
    private List<UserModelServer.Iface> USERMODEL_CLIENTS = new ArrayList<>();
    @Inject
    @ENV("monitor.userModel_key")
    private String monitorKey;
    @Inject
    private DBProxy dbProxy;

    //    @Inject
    //    public UserModel(@ENV("userModel.zookeepers") String zkServers, @ENV("userModel.jdns") String jdns) {
    //        String[] userModelZks = zkServers.split("\\|");
    //        for (String userModelZk : userModelZks) {
    //            USERMODEL_CLIENTS.add(new RPCClientBuilder<UserModelServer.Iface>()
    //                    .setClientClass(UserModelServer.Client.class)
    //                    .setZookeepers(userModelZk)
    //                    .setNamespace(jdns)
    //                    .setUseNameSpace(true)
    //                    .setTimeout(1000)
    //                    .setMinIdle(5)
    //                    .setMaxIdle(15)
    //                    .setMaxActive(1500)
    //                    .setMaxWaitTime(50)
    //                    .setExhaustedAction(Byte.valueOf("0"))
    //                    .build());
    //        }
    //    }

    protected UserModel() {
    }

    public Map<Long, CommenInfo> getUserModelResponse(final Collection<Long> keys, final String table) {
        MonitorUtils.start(monitorKey);
        try {
            Map<String, byte[]> values =
                    dbProxy.query(keys.stream().map(key -> String.valueOf(key).concat("_").concat(table))
                            .collect(Collectors.toList()), COMMON_INFO_DB_NAME);

            Map<Long, CommenInfo> ret = new HashMap<>();
            values.forEach((key, value) -> {
                if (value != null) {
                    try {
                        CommenInfo commenInfo = ThriftSerialization.fromCompactBytes(CommenInfo.class, value);
                        ret.put(Long.valueOf(key.substring(0, key.indexOf("_"))), commenInfo);
                    } catch (Exception e) {
                        LOGGER.debug("{}:{}[{}]", e.getMessage(), value, value.length);
                    }
                }
            });
            return ret;

            //            SkuCustomAttributeRequest request = new SkuCustomAttributeRequest();
            //            request.setClientId(CLIENT_ID);
            //            request.setSkuList(new ArrayList<>(keys));
            //            request.setType(table);
            //
            //            final Map<Long, CommenInfo> results;
            //            final int zkIndex = new Random().nextInt(USERMODEL_CLIENTS.size());
            //            try {
            //                results = USERMODEL_CLIENTS.get(zkIndex).multiGetCustomAttributInfoByCId(request);
            //            } catch (Exception e) {
            //                LOGGER.debug("get extra profiles for SKUs {} error: {}", Arrays.toString(keys.toArray()), e
            // .getMessage());
            //                return null;
            //            }
            //            if (null == results || results.size() <= 0) {
            //                LOGGER.info("UserModel result is null for all keys " + Arrays.toString(keys.toArray()) + ".");
            //                return null;
            //            }
            //
            //            return results;
        } catch (Exception e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug(e.getMessage(), e);
            throw new EnvironmentException("userModel", e);
        } finally {
            MonitorUtils.end(monitorKey);
        }
    }

    public Map<Long, SkuAttributInfo> getUserModelResponse(final Collection<Long> keys) {

//        SkuAttributeRequest request = new SkuAttributeRequest();
//        request.setClientId(CLIENT_ID);
//        request.setSkuList(new ArrayList<>(keys));
//
//        final Map<Long, SkuAttributInfo> results;
//        final int zkIndex = new Random().nextInt(USERMODEL_CLIENTS.size());
//        try {
//            results = USERMODEL_CLIENTS.get(zkIndex).getSkuAttributInfoMapByCId(request);
//        } catch (Exception e) {
//            LOGGER.error("Null result for SKUs: " + Arrays.toString(keys.toArray()), e);
//            return null;
//        }
//        if (null == results || results.size() <= 0) {
//            LOGGER.info("UserModel result is null for all keys " + Arrays.toString(keys.toArray()) + ".");
//            return null;
//        }
//        return results;
        try {
            Map<String, byte[]> values = dbProxy.query(keys.stream().map(String::valueOf).collect(Collectors.toList()),
                    SKU_ATTR_DB_NAME);
            Map<Long, SkuAttributInfo> ret = new HashMap<>();
            values.forEach((k, v) -> {
                if (Objects.nonNull(v)) {
                    try {
                        SkuAttributInfo skuAttributInfo =
                                ThriftSerialization.fromCompactBytes(SkuAttributInfo.class, v);
                        ret.put(Long.valueOf(k), skuAttributInfo);
                    } catch (Exception e) {
                        LOGGER.debug("{}:{}[{}]", e.getMessage(), v, v.length);
                    }

                }
            });
            return ret;
        } catch (Exception e) {
            throw new EnvironmentException("userModel-skuAttributInfo", e);
        }

    }

}

