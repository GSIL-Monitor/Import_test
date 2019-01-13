package com.jd.rec.nl.core.infrastructure;

import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.core.infrastructure.domain.ThriftParameter;
import com.jd.soa.RPCClientBuilder;

/**
 * @author linmx
 * @date 2018/6/12
 */
public interface BaseInfrastructure {

    String CLIENT_ID = "nrt";

    default <I> I createThriftClient(CLUSTER cluster, Class<I> ifaceClass, ThriftParameter parameter) {
        return new RPCClientBuilder<I>()
                .setClientClass(ifaceClass)
                .setZookeepers(parameter.getConnection(cluster))
                .setNamespace(parameter.getJdns())
                .setUseNameSpace(true)
                .setTimeout((int) parameter.getTimeout().toMillis())
                .setMinIdle(parameter.getMinIdle())
                .setMaxIdle(parameter.getMaxIdle())
                .setMaxActive(parameter.getMaxActive())
                .setMaxWaitTime(parameter.getMaxWaitTime().toMillis())
                .setExhaustedAction(Byte.valueOf(parameter.getExhaustedAction())).build();
    }

}
