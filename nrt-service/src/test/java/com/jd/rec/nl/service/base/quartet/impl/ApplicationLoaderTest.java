package com.jd.rec.nl.service.base.quartet.impl;

import com.google.inject.Provider;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.input.SourceInfo;
import com.jd.rec.nl.core.input.domain.SourceConfig;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.service.base.quartet.domain.KafkaConfig;
import com.jd.rec.nl.service.common.quartet.domain.RequiredDataInfo;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author linmx
 * @date 2018/6/11
 */
public class ApplicationLoaderTest {

    @Test
    public void test() {
        Config appDefineConfig = ConfigBase.getSystemConfig().getConfig("applicationDefine");
        Set<Map.Entry<String, ConfigValue>> appDefines = appDefineConfig.root().entrySet();
        Set<SourceConfig> sourceConfigs = new HashSet<>();
        for (Map.Entry<String, ConfigValue> appDefine : appDefines) {
            String appName = appDefine.getKey();
            Config importerConfig = appDefineConfig.getConfig(appName).getConfig("importer");
            sourceConfigs.addAll(createSourceConfig(appName, appDefineConfig.getConfig(appName)));
        }
        SourceInfo sourceInfo = new SourceInfo(sourceConfigs);
    }

    private Set<SourceConfig> createSourceConfig(String name, Config appConfig) {
        boolean enable = true;
        Config importerConfig = appConfig.getConfig("import");
        if (appConfig.hasPath("enable")) {
            enable = appConfig.getBoolean("enable");
        }
        Set<SourceConfig> sourceConfigs = new HashSet<>();
        for (Map.Entry<String, ConfigValue> entry : importerConfig.root().entrySet()) {
            KafkaConfig kafkaConfig = KafkaConfig.parseConfig(entry.getValue().atKey(entry.getKey()), enable);
            sourceConfigs.add(kafkaConfig);
        }
        return sourceConfigs;
    }

    @Test
    public void testConfig() {
        InjectorService.getCommonInjector();
        Config app = ConfigBase.getAppConfig("brandRecall");
        RequiredDataInfo requiredDataInfo = InjectorService.bindParameters(RequiredDataInfo.class,  app);
        requiredDataInfo.setName("brandRecall");
        app = ConfigBase.getAppConfig("cid3Blacklist");
        requiredDataInfo = InjectorService.bindParameters(RequiredDataInfo.class, app);
        requiredDataInfo.setName("cid3Blacklist");
    }

//    private <T extends Named> T bindParameters(Class<T> clazz, String name, Config config) {
//        Module appBindModule = binder -> {
//            binder.bind(String.class).annotatedWith(Names.named("name")).toInstance(name);
//            if (config != null) {
//                for (Map.Entry<String, ConfigValue> configValueEntry : config.entrySet()) {
//                    String key = configValueEntry.mapKey();
//                    Object value = configValueEntry.getValue().unwrapped();
//                    binder.bind(Key.get()).annotatedWith(Names.named(key)).toInstance(value);
//                }
//            }
//        };
////        List<Module> registerModules = InjectorService.getRegisterModules();
////        Module[] modules = new Module[registerModules.size() + 1];
////        registerModules.toArray(modules);
////        modules[modules.length - 1] = appBindModule;
//
//        Injector injector = InjectorService.getCommonInjector().createChildInjector(appBindModule);
//        T obj = injector.getInstance(clazz);
//        injector.injectMembers(obj);
//        return obj;
//    }

    private class CustomProvider<T> implements Provider<T> {

        private T value;

        public CustomProvider(T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }
    }

}