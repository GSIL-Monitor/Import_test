package com.jd.rec.nl.app.origin.modules.activityburst;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.jd.rec.nl.app.origin.modules.activityburst.domain.DimensionValue;
import com.jd.rec.nl.app.origin.modules.activityburst.domain.HistoryCUV;
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
public class ActivityBurstCoefficientService {

    public static final String cacheName = "ActBurstHC";

    public static final String namespace = "act-burst-hc";

    private static final Logger LOGGER = getLogger(ActivityBurstCoefficientService.class);

    @Inject
    private DBService dbService;

    public HistoryCUV getHistoryCUV(DimensionValue userDimensionValue, long activityId, long current) {
        String key = String.valueOf(activityId).concat(".").concat(userDimensionValue.toString());
        String fieldName = userDimensionValue.toString();
        return this.getFromCache(key, fieldName, activityId, current);
    }

    public void saveNewHistoryCUV(DimensionValue userDimensionValue, long activityId, HistoryCUV historyCUV, Duration ttl) {
        String key = String.valueOf(activityId).concat(".").concat(userDimensionValue.toString());
        String fieldName = userDimensionValue.toString();
        this.saveToCache(key, fieldName, activityId, historyCUV, ttl);
    }

    @CacheResult(cacheName = cacheName)
    public HistoryCUV getFromCache(@CacheKey String key, String fieldName, long activityId, long current) {
        HistoryCUV historyValue = null;
        try {
            historyValue = dbService.query(namespace, String.valueOf(activityId), fieldName);
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
    public void saveToCache(@CacheKey String key, String fieldName, long activityId, @CacheValue HistoryCUV historyValue,
                            Duration ttl) {
        try {
            dbService.save(namespace, String.valueOf(activityId), fieldName, historyValue, ttl);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
