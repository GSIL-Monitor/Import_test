package com.jd.rec.nl.app.origin.modules.promotionalburst;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.DimensionValue;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.HistoryCUV;
import com.jd.rec.nl.service.modules.db.service.DBService;
import com.jd.ump.profiler.proxy.Profiler;
import org.slf4j.Logger;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;
import java.time.Duration;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/8/20
 */
@Singleton
public class BurstCoefficientService {

    public static final String cacheName = "burstHC";

    public static final String namespace = "burst-hc";

    private static final Logger LOGGER = getLogger(BurstCoefficientService.class);

    @Inject
    private DBService dbService;

    public HistoryCUV getHistoryCUV(DimensionValue userDimensionValue, long sku, long current, String version) {
        String key = String.valueOf(sku).concat(".").concat(userDimensionValue.toString()).concat(version);
        String fieldName = userDimensionValue.toString();
        return this.getFromCache(key, fieldName, sku, current, version);
    }

    public void saveNewHistoryCUV(DimensionValue userDimensionValue, long sku, HistoryCUV historyCUV, Duration ttl,
                                  String version) {
        String key = String.valueOf(sku).concat(".").concat(userDimensionValue.toString()).concat(version);
        String fieldName = userDimensionValue.toString();
        this.saveToCache(key, fieldName, sku, historyCUV, ttl, version);
    }

    @CacheResult(cacheName = cacheName)
    public HistoryCUV getFromCache(@CacheKey String key, String fieldName, long sku, long current, String version) {
        HistoryCUV historyValue = null;
        try {
            historyValue = dbService.query(namespace.concat(version), String.valueOf(sku), fieldName);
        } catch (Exception e) {
            Profiler.countAccumulate("burstDB_error");
            LOGGER.debug(e.getMessage(), e);
        }
        if (historyValue == null) {
            historyValue = new HistoryCUV();
            historyValue.setCoefficient(0);
            historyValue.setTimestamp(current);
        }
        return historyValue;
    }

    @CachePut(cacheName = cacheName)
    public void saveToCache(@CacheKey String key, String fieldName, long sku, @CacheValue HistoryCUV historyValue,
                            Duration ttl, String version) {
        try {
            dbService.save(namespace.concat(version), String.valueOf(sku), fieldName, historyValue, ttl);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
