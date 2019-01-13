package com.jd.rec.nl.test.junit5.callback.callback;

import com.jd.rec.nl.core.config.ConfigBase;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/11/8
 */
public class InfrastructureJUnitMock implements BeforeAllCallback {
    
    @Override
    public void beforeAll(ExtensionContext containerExtensionContext) throws Exception {
        Map config = new HashMap();
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.DBProxy", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Clerk", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Predictor", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Jimdb", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.UnifiedOutput", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Zeus", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Behavior", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.UserModel", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.ILogKafka", true);
        com.typesafe.config.Config thread = ConfigFactory.parseMap(config);
        ConfigBase.setThreadConfig(thread);
    }
}
