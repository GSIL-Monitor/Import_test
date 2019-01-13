package com.jd.rec.nl.service.base.quartet;

import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.service.common.experiment.ExperimentInstance;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/8/9
 */
public interface Operator extends Named, Serializable {

    /**
     * 获取app名
     *
     * @return
     */
    @Override
    default String getNamespace() {
        if (this.getName().contains(ExperimentInstance.SEP_TAB)) {
            return this.getName().substring(0, this.getName().indexOf(ExperimentInstance.SEP_TAB));
        } else {
            return getName();
        }
    }
}
