package com.jd.rec.nl.connector.storm.infrastructure.mock;

import com.google.inject.Singleton;
import com.jd.jim.cli.Cluster;
import com.jd.jim.cli.ReloadableJimClientFactory;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.service.infrastructure.Jimdb;

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
}
