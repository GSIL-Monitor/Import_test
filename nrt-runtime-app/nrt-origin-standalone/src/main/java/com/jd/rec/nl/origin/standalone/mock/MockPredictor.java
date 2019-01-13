package com.jd.rec.nl.origin.standalone.mock;

import com.google.inject.Singleton;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.service.infrastructure.Predictor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/6/20
 */
@Mock(Predictor.class)
@Singleton
public class MockPredictor extends Predictor {

    @Override
    public Map<String, Map<Long, Float>> getPredictions(Collection<String> keys, String predictorType, int limit) {
        Map<String, Map<Long, Float>> ret = new HashMap<>();
        keys.forEach(key -> {
            ret.put(key, new HashMap<>());
        });
        return ret;
    }
}
