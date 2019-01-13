package com.jd.rec.nl.test.mock.infrastructure;

import com.google.inject.Singleton;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.service.infrastructure.ILogKafka;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/10/11
 */
@Singleton
@Mock(ILogKafka.class)
public class MockILogKafka extends ILogKafka {

    private static final Logger LOGGER = getLogger(MockILogKafka.class);

    @Override
    public void initProducer(String clientId, String brokerHost) {
    }

    @Override
    public void trace(List<byte[]> result) {
        result.forEach(value -> LOGGER.debug(new String(value)));
    }
}
