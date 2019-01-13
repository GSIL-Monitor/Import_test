package com.jd.rec.nl.service.common.quartet.processor;

import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.experiment.MultiVersionHolder;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.service.common.experiment.ExperimentVersionHolder;
import com.jd.rec.nl.service.common.quartet.LazyInitializer;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.quartet.domain.MapperConf;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.domain.RequiredDataInfo;
import com.jd.rec.nl.service.common.quartet.operator.impl.DefaultMapper;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/4
 */
public class MapProcessor implements LazyInitializer {

    private static final Logger LOGGER = getLogger(MapProcessor.class);

    private DefaultMapper mapper;

    private MultiVersionHolder versionHolder = new ExperimentVersionHolder();

    public void setRequiredData(List<RequiredDataInfo> dataInfos) {
        for (RequiredDataInfo dataInfo : dataInfos)
            this.versionHolder.register(dataInfo);
    }

    public MapperContext map(ImporterContext input) throws Exception {
        RequiredDataInfo multiDataInfo = null;
        List<RequiredDataInfo> allRequired = new ArrayList<>();
        Set<String> nodeNames = versionHolder.getNodesName();
        for (String name : nodeNames) {
            Collection<Named> temps = this.versionHolder.getMatchedBranches(name, input);
            temps.forEach(named -> allRequired.add((RequiredDataInfo) named));
        }
        if (allRequired.isEmpty()) {
            return null;
        }

        for (RequiredDataInfo requiredDataInfo : allRequired) {
            if (multiDataInfo == null) {
                multiDataInfo = requiredDataInfo;
            } else {
                multiDataInfo = multiDataInfo.merge(requiredDataInfo);
            }
        }

        MapperConf mapperConf = multiDataInfo.createConfig();

        try {
            return this.mapper.process(mapperConf, input);
        } catch (EnvironmentException e) {
            LOGGER.debug(e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }

    }

    public void init() {
        //        ApplicationService.reInit();
        versionHolder.build();
        mapper = InjectorService.getCommonInjector().getInstance(DefaultMapper.class);
    }

    public Set<String> getMappersName() {
        return this.versionHolder.getNodesName();
    }
}
