package com.jd.rec.nl.service.modules.misc.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.cache.annotation.CacheKeys;
import com.jd.rec.nl.core.cache.annotation.CacheName;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.service.infrastructure.Predictor;
import com.jd.rec.nl.service.infrastructure.Selector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.annotation.CacheResult;
import java.util.*;

@Singleton
public class MiscService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiscService.class);

    @Inject
    Predictor predictor;

    @Inject
    Selector selector;

    public Map<String, Map<Long, Float>> getRelatedWithoutNull(final Collection<String> keys, final String relation,
                                                               final int limit) {
        try {
            Map<String, Map<Long, Float>> related = getRelated(keys, relation, limit);
            for (String key : keys) {
                if (related.containsKey(key) && related.get(key) == null) {
                    related.remove(key);
                }
            }
            return related;
        } catch (EnvironmentException e) {
            LOGGER.debug(e.getMessage());
            return new HashMap<>();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return new HashMap<>();
        }
    }

    @CacheResult(cacheName = "miscService")
    public Map<String, Map<Long, Float>> getRelated(@CacheKeys final Collection<String> keys, @CacheName final String relation,
                                                    final int limit) {
        return predictor.getPredictions(keys, relation, limit);
    }
}
