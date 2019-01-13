package com.jd.rec.nl.core.experiment.sharding;

import com.jd.rec.nl.core.domain.Named;
import com.typesafe.config.Config;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/5/17
 */
public interface Sharding<I extends Serializable> {

    boolean match(I in);

    boolean matchConfig(Config config, Named named);

    Sharding newInstance(Config config, Named named);
}
