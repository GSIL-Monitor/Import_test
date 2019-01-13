package com.jd.rec.nl.origin.standalone.mock;

import com.google.inject.Singleton;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.service.infrastructure.DBProxy;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/6/25
 */
@Mock(DBProxy.class)
@Singleton
public class MockDBProxyStandalone extends DBProxy {

    private Map<CLUSTER, Map<String, Content>> contents = new HashMap<>();

    @Override
    public synchronized void setValue(CLUSTER clusterType, String key, byte[] value, Duration tll) throws Exception {
        Map<String, Content> clusterValues = contents.get(clusterType);
        if (clusterValues == null) {
            clusterValues = new HashMap<>();
            contents.put(clusterType, clusterValues);
        }
        Content content = new Content(key, value, tll.getSeconds());
        clusterValues.put(key, content);
    }

    @Override
    public synchronized void hSetValue(CLUSTER clusterType, String key, String field, byte[] value, Duration ttl) throws Exception {
        Map<String, Content> clusterValues = contents.get(clusterType);
        if (clusterValues == null) {
            clusterValues = new HashMap<>();
            contents.put(clusterType, clusterValues);
        }
        Content content = new Content(key, value, ttl.getSeconds());
        clusterValues.put(field.concat(".").concat(key), content);
    }

    @Override
    public synchronized byte[] hGetValue(CLUSTER clusterType, String key, String field) throws Exception {
        if (contents.containsKey(clusterType)) {
            Content content = contents.get(clusterType).get(field.concat(".").concat(key));
            if (content != null) {
                return content.getValue();
            }
        }
        return null;
    }

    @Override
    public synchronized byte[] getValue(CLUSTER clusterType, String key) throws Exception {
        if (contents.containsKey(clusterType)) {
            Content content = contents.get(clusterType).get(key);
            if (content != null) {
                return content.getValue();
            }
        }
        return null;
    }

    class Content {
        String key;
        byte[] value;
        long timeout;

        public Content(String key, byte[] value, long timeout) {

            this.key = key;
            this.value = value;
            this.timeout = timeout;
        }

        public String getKey() {
            return key;
        }

        public byte[] getValue() {
            return value;
        }

        public long getTimeout() {
            return timeout;
        }
    }
}
