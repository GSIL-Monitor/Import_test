package com.jd.rec.nl.service.infrastructure;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.si.diviner.MultiGetPredictionsRequest;
import com.jd.si.diviner.MultiGetPredictionsResult;
import com.jd.si.diviner.PredictionItem;
import com.jd.si.diviner.PredictorService;
import com.jd.soa.RPCClientBuilder;
import org.slf4j.Logger;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class Predictor implements BaseInfrastructure {

    private static final Logger LOGGER = getLogger(Predictor.class);

    private String[] CLERK_ZOOKEEPERS;

    private List<PredictorService.Iface> predictors;

    @Inject
    @ENV("monitor.predictor_key")
    private String monitorKey;

    protected Predictor() {
    }

    @Inject
    public Predictor(@ENV("clerk.zookeepers") String zkServers, @ENV("predictor.jdns") String jdns) {
        this.CLERK_ZOOKEEPERS = zkServers.split("\\|");
        predictors = new ArrayList<>();
        for (String CLERK_ZOOKEEPER : CLERK_ZOOKEEPERS) {
            predictors.add(new RPCClientBuilder<PredictorService.Iface>()
                    .setClientClass(PredictorService.Client.class)
                    .setZookeepers(CLERK_ZOOKEEPER)
                    .setNamespace(jdns)
                    .setUseNameSpace(true)
                    .setTimeout(1000)
                    .setMinIdle(5)
                    .setMaxIdle(15)
                    .setMaxActive(1500)
                    .setMaxWaitTime(50)
                    .setExhaustedAction(Byte.valueOf("0"))
                    .build());
        }
    }


    public Map<String, Map<Long, Float>> getPredictions(Collection<String> keys, String predictorType, int limit) {
        //ump
        MonitorUtils.start(monitorKey);

        try {
            final MultiGetPredictionsRequest request = new MultiGetPredictionsRequest();
            request.setKeys(new ArrayList<>(keys));
            request.setLimit(limit);
            request.setPredictorType(predictorType);
            request.setClientId(CLIENT_ID);

            final MultiGetPredictionsResult result;
            final int zkIndex = new Random().nextInt(predictors.size());
            result = predictors.get(zkIndex).multiGetPredictions(request);

            if (result == null) {
                LOGGER.debug("predictor null");
                return Collections.emptyMap();
            }

            Map<String, Map<Long, Float>> predictions = Maps.newHashMap();
            Map<String, List<PredictionItem>> itemMap = result.getPredictionsMap();
            if (itemMap == null || itemMap.isEmpty()) {
                Map<Long, Float> tmpValue = Maps.newHashMap();
                for (String key : keys) {
                    predictions.put(key, tmpValue);
                }
                return predictions;
            }

            List<String> noPredictionKeys = new ArrayList<>();
            for (String key : keys) {
                if (itemMap.containsKey(key)) {
                    List<PredictionItem> predictionItems = itemMap.get(key);
                    Map<Long, Float> tmpValue = Maps.newHashMap();
                    for (PredictionItem predictionItem : predictionItems) {
                        float itemWeight = (float) predictionItem.getWeight();
                        long recallItem = predictionItem.getSkuData().getSku();
                        tmpValue.put(recallItem, itemWeight);
                    }
                    predictions.put(key, tmpValue);
                } else {
                    noPredictionKeys.add(key);
                    Map<Long, Float> tmpValue = Maps.newHashMap();
                    predictions.put(key, tmpValue);
                }
            }

            return predictions;
        } catch (Exception e) {
            MonitorUtils.error(monitorKey, e);
            //            LOGGER.warn(e.getMessage());
            throw new EnvironmentException("predictor", e);
        } finally {
            MonitorUtils.end(monitorKey);
        }
    }
}
