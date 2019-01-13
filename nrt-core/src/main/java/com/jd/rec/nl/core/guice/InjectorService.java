package com.jd.rec.nl.core.guice;

import com.google.inject.*;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.jd.rec.nl.core.exception.FrameworkException;
import com.jd.rec.nl.core.guice.interceptor.AppClassMethodMatcher;
import com.jd.rec.nl.core.guice.interceptor.InterceptorConfig;
import com.jd.rec.nl.core.guice.interceptor.InterceptorProvider;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import com.typesafe.config.*;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 准备公共绑定好的inject,并且提供额外注册接口.使用方可以使用公共的injector并增加自己的injector
 *
 * @author linmx
 * @date 2018/5/16
 */
public class InjectorService {

    public static final String initPath = "com.jd.rec.nl";

    private static final Logger LOGGER = getLogger(InjectorService.class);

    private static final List<Module> registerModules = new ArrayList<>();

    private static boolean init = false;

    private static Injector injector;

    public static List<Module> getRegisterModules() {
        return registerModules;
    }

    /**
     * 将config的配置注册为自动参数绑定
     * 目前改为使用typesafe-guice来实现
     * @param config 配置对象
     */
    //    public static Module registerParameters(Config config) {
    //
    //        Module module = binder -> {
    //            for (Map.Entry<String, ConfigValue> configValueEntry : config.entrySet()) {
    //                Object value = configValueEntry.getValue().unwrapped();
    //                Class type = value.getClass();
    //                binder.bind(type).annotatedWith(Names.named(configValueEntry.getKey())).toInstance(value);
    //                if (value instanceof Integer) {
    //                    binder.bind(Long.class).annotatedWith(Names.named(configValueEntry.getKey()))
    //                            .toInstance(((Integer) value).longValue());
    //                }
    //            }
    //        };
    //        return module;
    //    }

    /**
     * 初始化并绑定参数,根据需要初始化的class来明确定义需要绑定的参数
     *
     * @param clazz
     * @param config
     * @param <T>
     * @return
     */
    public static <T> T bindParameters(Class<T> clazz, Config config) {
        Injector injector = InjectorService.getCommonInjector().createChildInjector(binder -> {
            ReflectionUtils.getAllFields(clazz, (field) ->
                    field.isAnnotationPresent(Named.class) && field.isAnnotationPresent(Inject.class))
                    .forEach(field -> {
                        Named named = field.getAnnotation(Named.class);
                        bindParameter(binder, named, config, field.getType(), field.getAnnotatedType().getType());
                    });
            ReflectionUtils.getAllMethods(clazz, method -> method.isAnnotationPresent(Inject.class)).forEach
                    (method -> {
                        Arrays.asList(method.getParameters()).forEach(parameter -> {
                            if (parameter.isAnnotationPresent(Named.class)) {
                                Named named = parameter.getAnnotation(Named.class);
                                bindParameter(binder, named, config, parameter.getType(), parameter.getAnnotatedType()
                                        .getType());
                            }
                        });
                    });
            ReflectionUtils.getAllConstructors(clazz, constructor -> constructor.isAnnotationPresent(Inject.class))
                    .forEach(constructor -> {
                        Arrays.asList(constructor.getParameters()).forEach(parameter -> {
                            if (parameter.isAnnotationPresent(Named.class)) {
                                Named named = parameter.getAnnotation(Named.class);
                                bindParameter(binder, named, config, parameter.getType(),
                                        parameter.getAnnotatedType().getType());
                            }
                        });
                    });
        });
        T instance = injector.getInstance(clazz);
        // just in time binging 需要再inject一次时才能生效
        injector.injectMembers(instance);
        return instance;
    }

    private static void bindParameter(Binder binder, Named named, Config config, Class<?> clazz, Type type) {
        String path = named.value();
        Object value = null;
        if (clazz == Optional.class) {
            Type actualClass = ((ParameterizedType) type).getActualTypeArguments()[0];
            Type actualType = null;
            if (actualClass instanceof ParameterizedType) {
                actualType = ((ParameterizedType) actualClass).getActualTypeArguments()[0];
            }
            value = getConfigValue(config, path, (Class<?>) actualClass, actualType);
            if (value == null) {
                value = Optional.empty();
            } else {
                value = Optional.of(value);
            }
        } else {
            value = getConfigValue(config, path, clazz, type);
        }
        if (value != null) {
            Key<Object> key = (Key<Object>) Key.get(type, named);
            binder.bind(key).toInstance(value);
        }
    }

    static Object getConfigValue(Config config, String path, Class<?> clazz, Type type) {
        if (config.hasPath(path)) {
            if (Config.class.isAssignableFrom(clazz)) {
                return config.getValue(path).atKey(path.substring(path.lastIndexOf(".") + 1));
            } else {
                ConfigValue bindValue = config.getValue(path);
                ConfigValueType valueType = bindValue.valueType();
                if (valueType.equals(ConfigValueType.OBJECT) && Map.class.isAssignableFrom(clazz)) {
                    if (type instanceof ParameterizedType) {
                        Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                        Type mapKeyType = actualTypeArguments[0];
                        Type mapValueType = actualTypeArguments[1];
                        Class<?> mapValueClass;
                        if (ParameterizedType.class.isInstance(mapValueType)) {
                            mapValueClass = (Class<?>) ((ParameterizedType) mapValueType).getRawType();
                        } else {
                            mapValueClass = (Class<?>) mapValueType;
                        }
                        Map<String, Object> unwrapped = config.getObject(path).unwrapped();
                        return unwrapped.entrySet().stream()
                                .collect(Collectors.collectingAndThen(Collectors.toMap(
                                        e -> {
                                            if (((Class) mapKeyType).isEnum()) {
                                                return Enum.valueOf((Class) mapKeyType, e.getKey());
                                            } else {
                                                return e.getKey();
                                            }
                                        },
                                        e -> getConfigValue(config, path + "." + e.getKey(), mapValueClass,
                                                mapValueType)
                                ), Collections::unmodifiableMap));
                    } else {
                        return bindValue.unwrapped();
                    }
                } else if (valueType.equals(ConfigValueType.OBJECT)) {
                    return ConfigBeanFactory.create(config.getConfig(path), clazz);
                } else if (valueType.equals(ConfigValueType.LIST) && List.class.isAssignableFrom(clazz)) {
                    if (type instanceof ParameterizedType) {
                        Type listType = ((ParameterizedType) type).getActualTypeArguments()[0];
                        if (((Class<?>) listType).isEnum()) {
                            return config.getStringList(path).stream()
                                    .map(name -> Enum.valueOf((Class) listType, name))
                                    .collect(Collectors.toList());
                        } else if (listType == Duration.class) {
                            return config.getDurationList(path);
                        } else if (listType == ConfigMemorySize.class) {
                            return config.getMemorySizeList(path);
                        } else {
                            ConfigList configList = config.getList(path);
                            return configList.stream().map(configValue -> {
                                if (configValue.valueType() == ConfigValueType.OBJECT) {
                                    return ConfigBeanFactory
                                            .create(((ConfigObject) configValue).toConfig(), (Class) listType);
                                } else {
                                    return configValue.unwrapped();
                                }
                            }).collect(
                                    Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
                        }
                    } else {
                        return bindValue.unwrapped();
                    }
                } else if (valueType.equals(ConfigValueType.STRING)) {
                    if (clazz.isEnum()) {
                        return Enum.valueOf((Class) clazz, (String) bindValue.unwrapped());
                    } else if (clazz == Duration.class) {
                        return config.getDuration(path);
                    } else if (clazz == ConfigMemorySize.class) {
                        return config.getMemorySize(path);
                    } else {
                        return bindValue.unwrapped();
                    }
                } else {
                    return bindValue.unwrapped();
                }
            }
        }
        return null;
    }

    /**
     * 注册自定义的modules
     *
     * @param modules
     */
    public static void registerModules(Module... modules) {
        registerModules.addAll(Arrays.asList(modules));
    }

    /**
     * 将注解了{@link com.google.inject.name.Named} 的类绑定到另一个类上
     *
     * @param bindClass 需要绑定的类,一般为接口
     * @param named     {@link Named#value()} 值
     * @param toClass   目标类
     */
    public static void registerBinder(Class bindClass, String named, Class toClass) {
        Module module = binder -> {
            AnnotatedBindingBuilder bindingBuilder = binder.bind(bindClass);
            if (named != null) {
                bindingBuilder.annotatedWith(Names.named(named)).to(toClass);
            } else {
                bindingBuilder.to(toClass);
            }
        };

        registerModules.add(module);
    }


    /**
     * 将注解了{@link com.google.inject.name.Named} 的类绑定到另一个类上
     *
     * @param bindClass 需要绑定的类
     * @param named     {@link Named#value()} 值
     * @param instance  目标对象
     */
    public static void registerBinder(Class bindClass, String named, Object instance) {
        if (!bindClass.isAssignableFrom(instance.getClass())) {
            LOGGER.warn("{} is not assignable from {}", bindClass.getName(), instance.getClass().getName());
            return;
        }
        Module module = binder -> binder.bind(bindClass).annotatedWith(Names.named(named)).toInstance(instance);
        registerModules.add(module);
    }

    /**
     * 绑定切面注解
     *
     * @param annotation
     */
    public static void registerInterceptor(Class<? extends Annotation> annotation) {
        InterceptorProvider interceptorProvider = annotation.getAnnotation(InterceptorProvider.class);
        if (interceptorProvider == null) {
            throw new FrameworkException("annotation define error!It must be annotated by BindingAnnotation and " +
                    "InterceptorProvider!");
        }
        Module module = binder -> {
            try {
                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(annotation), annotation
                        .getAnnotation(InterceptorProvider.class).value().newInstance());
                List<ElementType> elementTypes = Arrays.asList(annotation.getAnnotation(Target.class).value());
                if (elementTypes.contains(ElementType.TYPE)) {
                    binder.bindInterceptor(Matchers.annotatedWith(annotation), new AppClassMethodMatcher(),
                            annotation.getAnnotation(InterceptorProvider.class).value().newInstance());
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                throw new FrameworkException(e);
            }
        };

        registerModules.add(module);
    }

    /**
     * 绑定切面定义
     *
     * @param interceptorConfig
     */
    public static void registerInterceptor(InterceptorConfig interceptorConfig) {
        Module module = binder -> {
            try {
                binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(interceptorConfig.getAnnotation()),
                        interceptorConfig.getMethodInterceptor());
                List<ElementType> elementTypes =
                        Arrays.asList(((Target) interceptorConfig.getAnnotation().getAnnotation(Target
                                .class)).value());
                if (elementTypes.contains(ElementType.TYPE)) {
                    binder.bindInterceptor(Matchers.annotatedWith(interceptorConfig.getAnnotation()),
                            new AppClassMethodMatcher(),
                            interceptorConfig.getMethodInterceptor());
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                throw new FrameworkException(e);
            }
        };

        registerModules.add(module);
    }

    /**
     * 获取配置好的公共的注射器
     *
     * @return
     */
    public static Injector getCommonInjector() {
        if (!init) {
            synchronized (registerModules) {
                if (!init) {
                    LOGGER.warn("init common injector!");
                    // 初始化参数绑定
                    //                    Module paramModule = getParameterModule();
                    ApplicationContext context = new ApplicationContext();
                    context.initInjector();
                    // 初始化声明的绑定信息
                    //                    List<Module> injectModules = new ArrayList<>(registerModules);
                    //                    injectModules.add(paramModule);
                    injector = Guice.createInjector(Stage.PRODUCTION, registerModules);
                    // 初始化基础设施,提高运行效率
                    //                    initInfrastructure(context.getInfrastructures());
                    //                    registerModules.clear();
                    init = true;
                    LOGGER.warn("init common injector success!");
                }
            }
        }
        return injector;
    }

    private static void initInfrastructure(List<Class<? extends BaseInfrastructure>> infrastructures) {
        infrastructures.forEach(infrastructure -> {
            BaseInfrastructure baseInfrastructure = injector.getInstance(infrastructure);
            LOGGER.warn("init infrastructure:{}", baseInfrastructure.toString());
        });
    }

    /**
     * 重新初始化
     *
     * @throws Exception
     */
    public static void reInit() {
        init = false;
        getCommonInjector();
    }

    /**
     * 初始化系统参数绑定
     */
    //    private static Module getParameterModule() {
    //        Config systemConfig = ConfigBase.getSystemConfig();
    // ... Add your other modules here
        /*registerModules.add(TypesafeConfigModule.fromConfigWithPackage(
                systemConfig, ApplicationContext.initPath))initPath;*/
    //    }

}
