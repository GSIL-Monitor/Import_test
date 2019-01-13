package com.jd.rec.nl.connector.standalone.config;

import com.google.inject.Module;
import com.google.inject.matcher.Matchers;
import com.jd.rec.nl.connector.standalone.trace.MethodLogInterceptor;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.guice.config.Configuration;
import com.jd.rec.nl.core.guice.interceptor.MethodMatcher;
import com.jd.rec.nl.core.guice.interceptor.NoSyntheticMethodMatcher;
import com.jd.rec.nl.core.infrastructure.annotation.InfrastructureGet;
import com.jd.rec.nl.core.infrastructure.annotation.InfrastructureSet;
import com.jd.rec.nl.service.base.quartet.Reducer;
import com.jd.rec.nl.service.base.quartet.WindowCollector;
import com.jd.rec.nl.service.common.quartet.processor.MapProcessor;
import com.jd.rec.nl.service.common.quartet.processor.ParseProcessor;
import com.jd.rec.nl.service.common.quartet.processor.UpdateProcessor;
import com.jd.rec.nl.service.infrastructure.Predictor;
import com.jd.rec.nl.connector.standalone.diff.JimdbQueryInterceptor;
import com.jd.rec.nl.connector.standalone.diff.JimdbSaveInterceptor;
import com.jd.rec.nl.service.modules.db.service.DBService;

/**
 * @author linmx
 * @date 2018/6/21
 */
@Configuration
public class DebugConfiguration {

    public Module processorIOLog() {
        if (ConfigBase.getSystemConfig().hasPath("debug.traceProcessorIO.enable")
                && ConfigBase.getSystemConfig().getBoolean("debug.traceProcessorIO.enable")) {
            return (binder) -> {
                binder.bindInterceptor(Matchers.subclassesOf(ParseProcessor.class), new MethodMatcher("parse").and
                                (NoSyntheticMethodMatcher.INSTANCE),
                        new MethodLogInterceptor());
                binder.bindInterceptor(Matchers.subclassesOf(MapProcessor.class), new MethodMatcher("map").and
                                (NoSyntheticMethodMatcher.INSTANCE),
                        new MethodLogInterceptor());
                binder.bindInterceptor(Matchers.subclassesOf(UpdateProcessor.class), new MethodMatcher("update").and
                                (NoSyntheticMethodMatcher.INSTANCE),
                        new MethodLogInterceptor());

                binder.bindInterceptor(Matchers.subclassesOf(WindowCollector.class),
                        NoSyntheticMethodMatcher.INSTANCE.and(new MethodMatcher("collect").or(new MethodMatcher("shuffle"))),
                        new MethodLogInterceptor());

                binder.bindInterceptor(Matchers.subclassesOf(Reducer.class),
                        NoSyntheticMethodMatcher.INSTANCE.and(new MethodMatcher("collect").or(new MethodMatcher
                                ("reduce"))), new MethodLogInterceptor());

                binder.bindInterceptor(Matchers.subclassesOf(DBService.class),
                        NoSyntheticMethodMatcher.INSTANCE.and(new MethodMatcher("save").or(new MethodMatcher("query"))),
                        new MethodLogInterceptor());

                binder.bindInterceptor(Matchers.subclassesOf(Predictor.class),
                        NoSyntheticMethodMatcher.INSTANCE.and(new MethodMatcher("getPredictions")), new MethodLogInterceptor());
            };
        } else {
            return null;
        }
    }

    public Module isDiff() {
        if (ConfigBase.getSystemConfig().hasPath("debug.diff.enable")
                && ConfigBase.getSystemConfig().getBoolean("debug.diff.enable")) {
            return binder -> {
                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(InfrastructureSet.class), new JimdbSaveInterceptor());
                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(InfrastructureGet.class), new JimdbQueryInterceptor());
            };
        } else {
            return null;
        }
    }
}
