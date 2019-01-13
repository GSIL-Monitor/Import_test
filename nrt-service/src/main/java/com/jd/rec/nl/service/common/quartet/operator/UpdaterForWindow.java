package com.jd.rec.nl.service.common.quartet.operator;

import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/7/31
 */
public interface UpdaterForWindow<K extends Serializable, V extends Serializable> extends Updater {

    K generateKey(MapperContext input);

    V map(MapperContext input);

    @Override
    default void update(MapperContext input, ResultCollection resultCollection) {
        resultCollection.addMapResult(this.getName(), generateKey(input), map(input));
    }
}
