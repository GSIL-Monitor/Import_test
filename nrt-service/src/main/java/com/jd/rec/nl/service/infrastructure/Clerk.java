package com.jd.rec.nl.service.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import com.jd.si.clerk.SKUData;
import com.jd.si.clerk.SKUDetails;
import com.jd.si.util.ThriftSerialization;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/5/25
 */
@Singleton
public class Clerk implements BaseInfrastructure {

    private static final Logger LOGGER = getLogger(Clerk.class);

    //    private List<ClerkService.Iface> CLERK_CLIENTS;

    private String[] CLERK_ZOOKEEPERS;

    @Inject
    @ENV("monitor.clerk_key")
    private String monitorKey;

    @Inject
    private DBProxy dbProxy;

    protected Clerk() {
    }

    //    @Inject
    //    public Clerk(@ENV("clerk.zookeepers") String zkServers, @ENV("clerk.jdns") String jdns) {
    //        CLERK_ZOOKEEPERS = zkServers.split("\\|");
    //        CLERK_CLIENTS = new ArrayList<>();
    //        for (String clerkZk : CLERK_ZOOKEEPERS) {
    //            CLERK_CLIENTS.add(new RPCClientBuilder<ClerkService.Iface>()
    //                    .setClientClass(ClerkService.Client.class)
    //                    .setZookeepers(clerkZk)
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

    private ItemProfile getItemProfileFromDetails(final SKUDetails details) {
        final ItemProfile profile = new ItemProfile();
        final long sku = details.getSkuId();
        try {
            profile.setSku(sku);
            String bidStr = details.getBrandId();
            // brandId有比较大概率会不存在
            if (bidStr != null) {
                int bid = Integer.parseInt(bidStr);
                if (bid > 0) {
                    profile.setBrandId(bid);
                }
            }
            if (StringUtils.isNotEmpty(details.getShopId())) {
                long shopId = Long.valueOf(details.getShopId());
                if (shopId <= 0) {
                    LOGGER.debug(String.format("Invalid shopId: %d for SKU: %d", shopId, sku));
                } else {
                    profile.setShopId(shopId);
                }
            }
            int c3 = details.getCategoryLevel3();
            if (c3 <= 0) {
                LOGGER.debug(String.format("Invalid Cid3: %d for SKU: %d", c3, sku));
            } else {
                profile.setCid3(c3);
            }
            long pid = details.getParentId();
            if (pid <= 0) {
                LOGGER.debug(String.format("Invalid ParentId: %d for SKU: %d", pid, sku));
            } else {
                profile.setParentId(pid);
            }

        } catch (Exception e) {
            //            LOGGER.error(e.getMessage());
            throw e;
        }
        return profile;
    }

    public Map<Long, ItemProfile> getProfiles(final Collection<Long> skus) {
        assert (skus != null);

        try {
            //ump
            MonitorUtils.start(monitorKey);
            Map<String, byte[]> detailProfiles = dbProxy.query(skus.stream().map(sku -> String.valueOf(sku)).collect(Collectors
                    .toList()), "iteminfo-detail");
            //            Map<String, byte[]> priceProfiles = dbProxy.query(skus.stream().map(sku -> String.valueOf(sku))
            // .collect(Collectors
            //                    .toList()), "iteminfo-price");

            Map<Long, ItemProfile> ret = new HashMap<>();
            detailProfiles.forEach((key, value) -> {
                ItemProfile profile = null;
                if (value != null) {
                    SKUData skuData = ThriftSerialization.fromCompactBytes(SKUData.class, value);
                    try {
                        profile = getItemProfileFromDetails(skuData.getDetails());
                    } catch (Exception e) {
                        LOGGER.debug("{}:{}[{}]", e.getMessage(), value, value.length);
                    }
                }
                ret.put(Long.valueOf(key), profile);
            });
            return ret;

            //            final GetSKUDataRequest request = new GetSKUDataRequest();
            //            request.setReturnSKUDetails(true);
            //            request.setSkus(new ArrayList<>(skus));
            //            request.setClientId(CLIENT_ID);
            //
            //            final GetSKUDataResult result;
            //            final int zkIndex = new Random().nextInt(CLERK_CLIENTS.size());
            //            result = CLERK_CLIENTS.get(zkIndex).getSKUData(request);
            //
            //            final Map<Long, ItemProfile> profiles = Maps.newHashMap();
            //
            //            final Map<Long, SKUData> skuDatum = result.getSkuData();
            //            if (skuDatum == null || skuDatum.isEmpty()) {
            //                skus.forEach(sku -> profiles.put(sku, null));
            //                return profiles;
            //            }
            //
            //            for (final Map.Entry<Long, SKUData> e : skuDatum.entrySet()) {
            //                final long sku = e.getKey();
            //                final SKUData data = e.getValue();
            //                if (data == null) {
            //                    LOGGER.debug("Null data for SKU: " + sku);
            //                    profiles.put(sku, null);
            //                    continue;
            //                }
            //                final SKUDetails details = data.getDetails();
            //                if (details == null) {
            //                    LOGGER.debug("Null data details for SKU: " + sku);
            //                    profiles.put(sku, null);
            //                    continue;
            //                }
            //                final ItemProfile profile = getItemProfileFromDetails(details);
            //                profiles.put(sku, profile);
            //            }
            //
            //            return profiles;
        } catch (Exception e) {
            MonitorUtils.error(monitorKey, e);
            LOGGER.debug(e.getMessage(), e);
            throw new EnvironmentException("clerk", e);
        } finally {
            MonitorUtils.end(monitorKey);
        }
    }

}
