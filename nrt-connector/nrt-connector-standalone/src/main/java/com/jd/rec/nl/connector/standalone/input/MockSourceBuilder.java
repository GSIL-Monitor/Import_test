package com.jd.rec.nl.connector.standalone.input;

import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.input.Source;
import com.jd.rec.nl.core.input.SourceBuilder;
import com.jd.rec.nl.core.input.SourceInfo;
import com.typesafe.config.Config;

import java.util.Objects;

/**
 * @author linmx
 * @date 2018/6/19
 */
public class MockSourceBuilder implements SourceBuilder<Source> {
    @Override
    public Source build(SourceInfo sourceInfo) throws Exception {
        Config kafkaSourceStyle = null;
        Source mockSource;
        long duration;
        int limit;
        if (ConfigBase.getSystemConfig().hasPath("debug.kafkaSourceStyle")) {
            kafkaSourceStyle = ConfigBase.getSystemConfig().getConfig("debug.kafkaSourceStyle");
        }
        if (Objects.nonNull(kafkaSourceStyle)) {
            limit = kafkaSourceStyle.hasPath("limit") ?
                    kafkaSourceStyle.getInt("limit") : 0;
            duration = kafkaSourceStyle.hasPath("duration") ?
                    kafkaSourceStyle.getLong("duration") : 0;
            mockSource = new MockKafkaSource(sourceInfo.getConfigs(), limit, duration);
        } else {
            mockSource = new MockSource(sourceInfo.getConfigs());
        }
        return mockSource;
    }
}
