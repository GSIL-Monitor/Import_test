package com.jd.rec.nl.app.origin.modules.cid3preference;

import com.google.inject.Inject;
import com.jd.rec.nl.app.origin.modules.cid3preference.domain.UserClickInfo;
import com.jd.rec.nl.service.modules.db.service.DBService;
import org.slf4j.Logger;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;
import java.time.Duration;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author wl
 * @date 2018/9/4
 */
public class Cid3CofficientService {

    public static final String cacheName = "cid3";
    public static final String namespace = "cid3Preference";
    private static final Logger LOGGER = getLogger(Cid3CofficientService.class);

    @Inject
    private DBService dbService;

    public UserClickInfo userClickInfo(String namespace, String uid) {
        String key = "cid3Preference_" + uid;
        return this.getFromCache(key, namespace, uid);
    }

    public void saveNewUserClickInfo(String namespace, String uid, UserClickInfo userClickInfo, Duration ttl) {
        String key = "cid3Preference_" + uid;
        this.saveToCache(key, namespace, uid, userClickInfo, ttl);
    }

    @CacheResult(cacheName = cacheName)
    public UserClickInfo getFromCache(@CacheKey String key, String namespace, String uid) {
        UserClickInfo userClickInfo = null;
        try {
            userClickInfo = dbService.query(namespace, uid);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        if (userClickInfo == null) {
            userClickInfo = new UserClickInfo();
        }
        return userClickInfo;
    }

    @CachePut(cacheName = cacheName)
    public void saveToCache(@CacheKey String key, String namespace, String uid, @CacheValue UserClickInfo userClickInfo,
                            Duration ttl) {
        try {
            dbService.save(namespace, uid, userClickInfo, ttl);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
