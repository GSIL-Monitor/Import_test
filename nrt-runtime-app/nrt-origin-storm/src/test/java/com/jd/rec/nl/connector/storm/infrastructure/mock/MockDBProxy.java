package com.jd.rec.nl.connector.storm.infrastructure.mock;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.service.infrastructure.DBProxy;
import com.jd.rec.nl.service.infrastructure.Jimdb;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/20
 */
@Mock(value = DBProxy.class, enable = false)
@Singleton
public class MockDBProxy extends DBProxy {

    public static final Duration defaultTLL = Duration.ofMinutes(10);
    private static final Logger LOGGER = getLogger(MockDBProxy.class);
    @Inject
    private Jimdb jimdb;

    @Override
    public void setValue(CLUSTER clusterType, String key, byte[] value, Duration tll) throws Exception {
        LOGGER.debug("key:{}, value length:{}, timeout:{}", key, value.length, tll.getSeconds());
        if (tll == null) {
            jimdb.getCluster(CLUSTER.HT).setEx(key.getBytes(), value, defaultTLL.getSeconds(), TimeUnit.SECONDS);
        } else {
            jimdb.getCluster(CLUSTER.HT).setEx(key.getBytes(), value, tll.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public byte[] getValue(CLUSTER clusterType, String key) throws Exception {
        byte[] ret = jimdb.getCluster(clusterType).get(key.getBytes());
        LOGGER.debug("key:{}, ret length:{}, value:{}", key, ret == null ? 0 : ret.length, ret);
        return ret;
    }

}
