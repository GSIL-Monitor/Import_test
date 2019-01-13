package com.jd.rec.nl.service.common.quartet.processor;

import com.google.inject.Injector;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.service.base.quartet.Exporter;
import com.jd.rec.nl.service.base.quartet.domain.OutputResult;
import com.jd.rec.nl.service.common.experiment.ExperimentInstance;
import com.jd.rec.nl.service.common.quartet.LazyInitializer;
import com.jd.rec.nl.service.common.quartet.operator.impl.ModelExporter;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/11
 */
public class ExportProcessor implements LazyInitializer {

    private static final Logger LOGGER = getLogger(ExportProcessor.class);

    Exporter defaultExporter;

    private Map<String, Exporter> exporters = new HashMap<>();

    public void setExporters(Collection<Exporter> exporters) {
        exporters.forEach(exporter -> this.exporters.put(exporter.getNamespace(), exporter));
    }

    public void export(OutputResult output) throws Exception {
        try {
            String name = output.getExecutorName();
            if (name.indexOf(ExperimentInstance.SEP_TAB) != -1) {
                name = name.substring(0, name.indexOf(ExperimentInstance.SEP_TAB));
            }
            if (exporters.containsKey(name)) {
                exporters.get(name).export(output);
            } else {
                defaultExporter.export(output);
            }
        } catch (EnvironmentException e) {
            LOGGER.debug(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    public Set<String> getExportersName() {
        Set<String> names = new HashSet<>(this.exporters.keySet());
        names.add(this.defaultExporter.getName());
        return names;
    }

    public void init() {
        Injector injector = InjectorService.getCommonInjector();
        this.exporters = this.exporters.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry ->
                injector.getInstance(entry.getValue().getClass())
        ));
        this.defaultExporter = InjectorService.getCommonInjector().getInstance(ModelExporter.class);
    }
}
