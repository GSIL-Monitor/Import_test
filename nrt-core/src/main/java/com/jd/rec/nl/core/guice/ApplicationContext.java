package com.jd.rec.nl.core.guice;

import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.cache.CacheDefine;
import com.jd.rec.nl.core.cache.CacheFactory;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.core.exception.WrongConfigException;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.guice.config.Configuration;
import com.jd.rec.nl.core.guice.config.Initializer;
import com.jd.rec.nl.core.guice.interceptor.InterceptorConfig;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import com.typesafe.config.Config;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

import static com.jd.rec.nl.core.guice.InjectorService.initPath;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/5/28
 */
@Singleton
public class ApplicationContext implements Serializable {

    private static final Logger LOGGER = getLogger(ApplicationContext.class);
    public static Reflections reflections = new Reflections(new ConfigurationBuilder()
            .filterInputsBy(new FilterBuilder().includePackage(initPath))
            .setUrls(ClasspathHelper.forPackage(initPath))
            .addScanners(
                    new TypeAnnotationsScanner(),
                    new MethodParameterScanner(),
                    new MethodAnnotationsScanner(),
                    new FieldAnnotationsScanner())
    );
    private static Map<Class, Object> storedConfig = null;
    //    private static Reflections envReflections = new Reflections(new ConfigurationBuilder()
    //            .filterInputsBy(new FilterBuilder().includePackage(initPath))
    //            .setUrls(ClasspathHelper.forPackage(initPath))
    //            .setScanners(
    //                    new TypeAnnotationsScanner(),
    //                    new MethodParameterScanner(),
    //                    new MethodAnnotationsScanner(),
    //                    new FieldAnnotationsScanner()
    //            ));
    private List<Class<? extends BaseInfrastructure>> infrastructures = new ArrayList<>();

    public ApplicationContext() {
        initConfig();
    }

    private static void initConfig() {
        if (storedConfig == null) {
            synchronized (reflections) {
                if (storedConfig == null) {
                    storedConfig = new HashMap<>();
                    Set<Class<?>> configClasses = reflections.getTypesAnnotatedWith(Configuration.class);
                    for (Class configClass : configClasses) {
                        Object config = Guice.createInjector().getInstance(configClass);
                        storedConfig.put(configClass, config);
                    }
                }
            }
        }
    }

    /**
     * 获得原始的bean定义
     *
     * @param type
     * @param <T>
     * @return
     */
    public static <T> Set<T> getRawDefinedBeans(Class<T> type) {
        initConfig();
        Set<T> beans = new HashSet<T>();
        for (Object config : storedConfig.values()) {
            Set<Method> methods = ReflectionUtils.getAllMethods(config.getClass(),
                    ReflectionUtils.withReturnTypeAssignableTo(type));
            for (Method method : methods) {
                T bean;
                try {
                    bean = (T) method.invoke(config);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    continue;
                }
                if (bean != null)
                    beans.add(bean);
            }
        }
        return beans;
    }

    protected List<Class<? extends BaseInfrastructure>> getInfrastructures() {
        return infrastructures;
    }

    protected void initInjector() {
        try {
            initEnvParams();
            initCache();
            initInterceptor();
            initCustomModule();
            initInfrastructure();
            initializer();
        } catch (Exception e) {
            throw new WrongConfigException(e);
        }
    }

    private void initializer() {
        Set<Initializer> initializers = getRawDefinedBeans(Initializer.class);
        initializers.forEach(initializer -> initializer.initialize());
    }

    private void initEnvParams() {
        Config systemConfig = ConfigBase.getSystemConfig();
        InjectorService.registerModules((Module) binder -> {
            List<Parameter> needBindParams = new ArrayList<>();
            Set<Constructor> annotatedConstructors = reflections.getConstructorsWithAnyParamAnnotated(ENV.class);
            for (Constructor c : annotatedConstructors) {
                Parameter[] params = c.getParameters();
                Arrays.stream(params).filter((param) -> param.isAnnotationPresent(ENV.class)).forEach(parameter ->
                        needBindParams.add(parameter));
            }

            Set<Method> annotatedMethods = reflections.getMethodsWithAnyParamAnnotated(ENV.class);
            for (Method m : annotatedMethods) {
                Parameter[] params = m.getParameters();
                Arrays.stream(params).filter((param) -> param.isAnnotationPresent(ENV.class)).forEach(parameter ->
                        needBindParams.add(parameter));
            }

            needBindParams.forEach(parameter -> {
                Object value = InjectorService.getConfigValue(systemConfig, parameter.getAnnotation(ENV.class).value(),
                        parameter.getType(), parameter.getAnnotatedType().getType());
                if (value != null) {
                    Key<Object> key = (Key<Object>) Key.get(parameter.getAnnotatedType().getType(),
                            parameter.getAnnotation(ENV.class));
                    binder.bind(key).toInstance(value);
                }
            });

            Set<Field> annotatedFields = reflections.getFieldsAnnotatedWith(ENV.class);
            annotatedFields.forEach(field -> {
                Object value = InjectorService.getConfigValue(systemConfig, field.getAnnotation(ENV.class).value(),
                        field.getType(), field.getAnnotatedType().getType());
                if (value != null) {
                    Key<Object> key = (Key<Object>) Key.get(field.getAnnotatedType().getType(),
                            field.getAnnotation(ENV.class));
                    binder.bind(key).toInstance(value);
                }
            });
        });
    }

    private void initCache() {
        Set<CacheDefine> cacheDefines = getRawDefinedBeans(CacheDefine.class);
        cacheDefines.forEach(cacheDefine -> {
            CacheFactory.loadCache(cacheDefine);
        });
    }

    private void initInfrastructure() {
        boolean mockFlag = false;
        Config mockConfig = null;
        if (ConfigBase.getSystemConfig().hasPath("debug.mock.infrastructure")) {
            mockFlag = true;
            mockConfig = ConfigBase.getSystemConfig().getConfig("debug.mock.infrastructure");
        }

        Set<Class<? extends BaseInfrastructure>> infrastructures = reflections.getSubTypesOf(BaseInfrastructure.class);
        for (Class<? extends BaseInfrastructure> infrastructure : infrastructures) {
            Mock mock = infrastructure.getAnnotation(Mock.class);
            if (mock != null) {
                if (mockFlag && mockConfig.hasPath(mock.value().getName()) && mockConfig.getBoolean(mock.value().getName())
                        && mock.enable()) {
                    if (mock.value().isAssignableFrom(infrastructure))
                        InjectorService.registerBinder(mock.value(), null, infrastructure);
                    else {
                        LOGGER.error("{} is not in instanceof {}", infrastructure.getName(), mock.value().getName());
                        continue;
                    }
                }
            } else {
                this.infrastructures.add(infrastructure);
            }
        }
    }

    private void initInterceptor() throws IllegalAccessException, InvocationTargetException {
        Set<InterceptorConfig> interceptorConfigs = getRawDefinedBeans(InterceptorConfig.class);
        interceptorConfigs.stream().forEach(interceptorConfig -> {
            LOGGER.error("register interceptor : {}", interceptorConfig.getClass().getName());
            InjectorService.registerInterceptor(interceptorConfig);
        });
    }

    private void initCustomModule() throws IllegalAccessException, InvocationTargetException {
        Set<Module> modules = getRawDefinedBeans(Module.class);
        modules.forEach(module -> {
            InjectorService.registerModules(module);
        });

    }
}
