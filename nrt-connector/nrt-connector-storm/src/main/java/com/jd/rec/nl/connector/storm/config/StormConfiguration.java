package com.jd.rec.nl.connector.storm.config;

import com.google.inject.Module;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matchers;
import com.jd.rec.nl.connector.storm.trace.ProcessorInterceptor;
import com.jd.rec.nl.connector.storm.trace.SpecialUIDMonitor;
import com.jd.rec.nl.core.guice.config.Configuration;
import com.jd.rec.nl.core.guice.interceptor.MethodMatcher;
import com.jd.rec.nl.core.guice.interceptor.NoSyntheticMethodMatcher;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.operator.SimpleUpdater;
import com.jd.rec.nl.service.common.quartet.processor.ExportProcessor;
import com.jd.rec.nl.service.common.quartet.processor.MapProcessor;
import com.jd.rec.nl.service.common.quartet.processor.ParseProcessor;
import com.jd.rec.nl.service.common.quartet.processor.UpdateProcessor;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author linmx
 * @date 2018/6/28
 */
@Configuration
public class StormConfiguration {

    //    public Module dbTrace() {
    //        if (ConfigBase.getSystemConfig().hasPath("trace.uid") && !ConfigBase.getSystemConfig().getStringList("trace.uid")
    //                .isEmpty()) {
    //            return binder -> {
    //                DBTraceInterceptor dbTrace = new DBTraceInterceptor();
    //                binder.bindInterceptor(dbTrace.matchedClass(), dbTrace.matchedMethod(), dbTrace);
    //            };
    //        } else {
    //            return null;
    //        }
    //    }

    public Module boltDebug() {
        return binder -> {
            binder.bindInterceptor(Matchers.subclassesOf(MapProcessor.class),
                    NoSyntheticMethodMatcher.INSTANCE.and(new MethodMatcher("map")), new ProcessorInterceptor());
            binder.bindInterceptor(Matchers.subclassesOf(UpdateProcessor.class),
                    NoSyntheticMethodMatcher.INSTANCE.and(new MethodMatcher("update")), new ProcessorInterceptor());
            binder.bindInterceptor(Matchers.subclassesOf(ExportProcessor.class),
                    NoSyntheticMethodMatcher.INSTANCE.and(new MethodMatcher("export")), new ProcessorInterceptor());
        };
    }

    public Module specialUIDMonitor() {
        return binder -> {
            binder.bindInterceptor(Matchers.subclassesOf(ParseProcessor.class),
                    NoSyntheticMethodMatcher.INSTANCE.and(new MethodMatcher("parse")), new SpecialUIDMonitor());
            binder.bindInterceptor(Matchers.subclassesOf(MapProcessor.class),
                    NoSyntheticMethodMatcher.INSTANCE.and(new MethodMatcher("map")), new SpecialUIDMonitor());
            binder.bindInterceptor(Matchers.subclassesOf(SimpleUpdater.class),
                    NoSyntheticMethodMatcher.INSTANCE.and(new AbstractMatcher<Method>() {
                        @Override
                        public boolean matches(Method method) {
                            try {
                                Method updateM = SimpleUpdater.class.getMethod("update", MapperContext.class);
                                boolean match = updateM.getName().equals(method.getName()) &&  method.getReturnType() != null
                                        && Arrays.stream(updateM.getParameterTypes()).anyMatch(type -> Arrays.asList(method
                                        .getParameterTypes()).contains(type));
                                return match;
                            } catch (NoSuchMethodException e) {
                                return false;
                            }
                        }
                    }), new SpecialUIDMonitor());
        };
    }

}
