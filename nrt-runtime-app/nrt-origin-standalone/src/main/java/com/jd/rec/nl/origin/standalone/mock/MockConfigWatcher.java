package com.jd.rec.nl.origin.standalone.mock;

import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.service.infrastructure.ConfigWatcher;

import java.util.function.Consumer;

@Mock(ConfigWatcher.class)
public class MockConfigWatcher extends ConfigWatcher {

    @Override
    public void initClient(String brokers, int conTimeout, int sessionTimeout) {
    }

    @Override
    public void registerAppChangeListener(Consumer<ChangedValue> configChangeConsumer) {

    }

    @Override
    protected void registerExpChangeListener(String appPath, Consumer<ChangedValue> consumer) {

    }

    @Override
    public void close() {

    }
}
