package com.jd.rec.nl.core.experiment.sharding;

import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.core.guice.ApplicationContext;
import com.typesafe.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author linmx
 * @date 2018/5/18
 */
public abstract class ShardingFactory {

    static List<Sharding> shardings = new ArrayList<>(ApplicationContext.getRawDefinedBeans(Sharding.class));

    public static List<Sharding> getShardings(Config config, Named named) {
        List<Sharding> matched = shardings.stream()
                .filter(sharding -> sharding.matchConfig(config, named))
                .map(sharding -> sharding.newInstance(config, named)).collect(Collectors.toList());
        return matched;
    }
}
