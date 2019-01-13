package com.jd.rec.nl.service.common.quartet.operator.impl;

import com.google.inject.Inject;
import com.jd.rec.nl.service.base.quartet.Exporter;
import com.jd.rec.nl.service.base.quartet.domain.OutputResult;
import com.jd.rec.nl.service.infrastructure.UnifiedOutput;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/11
 */
public class ModelExporter implements Exporter {

    private static final Logger LOGGER = getLogger(ModelExporter.class);

    private String name = "modelExporter";

    @Inject
    private UnifiedOutput unifiedOutput;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void export(OutputResult result) {
        boolean outputFlag = unifiedOutput.output(result.getKey(), result.getType(), result.getSubType(), result.getTtl(),
                result.getValue());
        if (!outputFlag) {
            LOGGER.error("subType[{}] is not register!", result.getSubType());
        }
    }
}
