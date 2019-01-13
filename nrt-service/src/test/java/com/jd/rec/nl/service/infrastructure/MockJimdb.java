package com.jd.rec.nl.service.infrastructure;

import com.google.inject.Singleton;
import com.jd.jim.cli.Cluster;
import com.jd.jim.cli.ReloadableJimClientFactory;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author linmx
 * @date 2018/7/2
 */
@Mock(Jimdb.class)
@Singleton
public class MockJimdb extends Jimdb {

    Cluster cluster;

    public MockJimdb() {
        ReloadableJimClientFactory factory = new ReloadableJimClientFactory();
        factory.setJimUrl("jim://1803528671997086613/2");
        cluster = factory.getClient();
    }

    @Override
    public Cluster getCluster(CLUSTER clusterType) {
        return cluster;
    }

    @Test
    public void testHashTTL() throws InterruptedException {
        MockJimdb jimdb = new MockJimdb();
        Cluster cluster = jimdb.getCluster(CLUSTER.HT);
        Duration ttl = Duration.ofSeconds(20);
        String key = "nrt-test";
        String field = "testField";
        String value = "testValue";
        cluster.hSet(key.getBytes(), field.getBytes(), value.getBytes());
        cluster.expire(key.getBytes(), ttl.getSeconds(), TimeUnit.SECONDS);
        long ttlS = cluster.ttl(key.getBytes());
        System.out.println(ttlS);
        ttlS = cluster.ttl(key);
        System.out.println(ttlS);

        Thread.sleep(1000L);
        String field2 = "testField2";
        cluster.hSet(key.getBytes(), field2.getBytes(), value.getBytes());
        ttlS = cluster.ttl(key.getBytes());
        System.out.println(ttlS);
    }
}
