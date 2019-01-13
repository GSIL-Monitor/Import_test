package com.jd.rec.nl.service.common.experiment.sharding;

import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.core.experiment.sharding.Sharding;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.domain.RequiredDataInfo;
import com.typesafe.config.Config;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class SourceSharding implements Sharding<Serializable> {

    private Set<String> sources = new HashSet<>();

    @Override
    public boolean match(Serializable in) {
        if (in instanceof ImporterContext) {
            return sources.contains(((ImporterContext) in).getSource());
        } else if (in instanceof MapperContext) {
            return sources.contains(((MapperContext) in).getEventContent().getSource());
        }
        return false;
    }

    @Override
    public boolean matchConfig(Config config, Named named) {
        return config.hasPath("import") && ((named instanceof RequiredDataInfo) || (named instanceof Updater));
    }

    @Override
    public Sharding newInstance(Config config, Named named) {
        SourceSharding sourceSharding = new SourceSharding();
        config.getConfig("import").entrySet().stream().filter(entry -> entry.getKey().indexOf("topic") != -1)
                .map(entry -> (String) entry.getValue().unwrapped())
                .forEach(value -> sourceSharding.sources.add(value));
        return sourceSharding;
    }
}
