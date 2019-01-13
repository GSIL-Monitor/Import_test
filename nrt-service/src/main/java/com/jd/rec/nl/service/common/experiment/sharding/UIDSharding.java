package com.jd.rec.nl.service.common.experiment.sharding;

import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.core.domain.UserEvent;
import com.jd.rec.nl.core.experiment.sharding.Sharding;
import com.typesafe.config.Config;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/5/18
 */
public class UIDSharding implements Sharding<Serializable> {

    PercentSharding percentSharding = new PercentSharding();

    @Override
    public boolean match(Serializable in) {
        if (in instanceof UserEvent) {
            String uid = ((UserEvent) in).getUid();
            return percentSharding.match(uid);
        }
        return false;
    }

    @Override
    public boolean matchConfig(Config config, Named named) {
        return percentSharding.matchConfig(config, named);
    }

    @Override
    public UIDSharding newInstance(Config config, Named named) {
        UIDSharding uidSharding = new UIDSharding();
        uidSharding.percentSharding = percentSharding.newInstance(config, named);
        return uidSharding;
    }

}
