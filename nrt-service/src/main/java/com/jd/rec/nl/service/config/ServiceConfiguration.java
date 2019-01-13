package com.jd.rec.nl.service.config;

import com.google.inject.Module;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matchers;
import com.jd.rec.nl.core.cache.CacheDefine;
import com.jd.rec.nl.core.cache.guava.GuavaCacheDefine;
import com.jd.rec.nl.core.experiment.sharding.Sharding;
import com.jd.rec.nl.core.guice.config.Configuration;
import com.jd.rec.nl.core.guice.config.Initializer;
import com.jd.rec.nl.core.guice.interceptor.MethodMatcher;
import com.jd.rec.nl.core.guice.interceptor.NoSyntheticMethodMatcher;
import com.jd.rec.nl.service.base.quartet.Exporter;
import com.jd.rec.nl.service.base.quartet.WindowCollector;
import com.jd.rec.nl.service.common.cache.Jimdb.JimdbCacheDefine;
import com.jd.rec.nl.service.common.cache.ehcache.EhcacheDefine;
import com.jd.rec.nl.service.common.cache.levelII.LevelIICacheDefine;
import com.jd.rec.nl.service.common.experiment.sharding.SourceSharding;
import com.jd.rec.nl.service.common.experiment.sharding.UIDSharding;
import com.jd.rec.nl.service.common.monitor.ExporterMonitor;
import com.jd.rec.nl.service.common.monitor.JvmMonitor;
import com.jd.rec.nl.service.common.monitor.WindowMonitor;
import com.jd.rec.nl.service.common.quartet.filter.MapperFilterInterceptor;
import com.jd.rec.nl.service.common.quartet.filter.MethodFilterInterceptor;
import com.jd.rec.nl.service.modules.db.service.DBService;
import com.jd.rec.nl.service.modules.user.service.UserService;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author linmx
 * @date 2018/7/2
 */
@Configuration
public class ServiceConfiguration {

    /**
     * 用户画像缓存
     *
     * @return
     */
    public CacheDefine userProfileCache() {
        GuavaCacheDefine guavaCacheDefine = new GuavaCacheDefine() {
            @Override
            public String getCacheName() {
                return UserService.userProfileCacheName;
            }
        };
        guavaCacheDefine.setStatisticsInterval(3600000);
        guavaCacheDefine.setMaximumSize(500000);
        guavaCacheDefine.setExpireAfterWrite(12);
        guavaCacheDefine.setExpireTimeUnit(TimeUnit.HOURS);
        return guavaCacheDefine;
    }

    /**
     * 商品画像缓存
     *
     * @return
     */
    public CacheDefine itemProfileCache() {
        EhcacheDefine ehcacheDefine = new EhcacheDefine() {
            @Override
            public String getCacheName() {
                return "itemProfile";
            }
        };
        ehcacheDefine.setOffHepapSize(500);
        ehcacheDefine.setExpireAfterWrite(5);
        ehcacheDefine.setExpireTimeUnit(TimeUnit.HOURS);
        return ehcacheDefine;
    }

    /**
     * user model 缓存(实际保存的是商品画像)
     *
     * @return
     */
    public CacheDefine userModelCache() {
        GuavaCacheDefine guavaCacheDefine = new GuavaCacheDefine() {
            @Override
            public boolean isDynamicCreate() {
                return true;
            }            @Override
            public String getCacheName() {
                return "userModel";
            }


        };
        guavaCacheDefine.setStatisticsInterval(3600000);
        guavaCacheDefine.setMaximumSize(500000);
        return guavaCacheDefine;
    }


    /**
     * 状态缓存
     *
     * @return
     */
    public CacheDefine dbServiceCache() {
        GuavaCacheDefine guavaCacheDefine = new GuavaCacheDefine() {
            @Override
            public String getCacheName() {
                return DBService.cacheName;
            }
        };
        guavaCacheDefine.setStatisticsInterval(3600000);
        guavaCacheDefine.setMaximumSize(100000);
        return guavaCacheDefine;
    }


    /**
     * 杂项缓存
     *
     * @return
     */
    public CacheDefine miscCache() {
        LevelIICacheDefine levelIICacheDefine = new LevelIICacheDefine() {
            @Override
            public boolean isDynamicCreate() {
                return true;
            }            @Override
            public String getCacheName() {
                return "miscService";
            }


        };
        levelIICacheDefine.setStatisticsInterval(3600000);
        levelIICacheDefine.setMaximumSize(50000);
        levelIICacheDefine.setExpireAfterWrite(12);
        levelIICacheDefine.setExpireTimeUnit(TimeUnit.HOURS);
        JimdbCacheDefine jimdbCacheDefine = new JimdbCacheDefine() {
            @Override
            public boolean isDynamicCreate() {
                return true;
            }            @Override
            public String getCacheName() {
                return "miscService_LevelII";
            }


        };
        jimdbCacheDefine.setExpireAfterWrite(3);
        jimdbCacheDefine.setExpireTimeUnit(TimeUnit.HOURS);
        levelIICacheDefine.setLevelIIDefine(jimdbCacheDefine);
        return levelIICacheDefine;
    }

    /**
     * 用户行为数据缓存
     *
     * @return
     */
    public CacheDefine userBehaviorCache() {
        GuavaCacheDefine guavaCacheDefine = new GuavaCacheDefine() {
            @Override
            public String getCacheName() {
                return UserService.behaviorCacheName;
            }
        };
        guavaCacheDefine.setStatisticsInterval(3600000);
        guavaCacheDefine.setMaximumSize(100000);
        guavaCacheDefine.setExpireAfterWrite(1);
        guavaCacheDefine.setExpireTimeUnit(TimeUnit.HOURS);
        return guavaCacheDefine;
    }

    /**
     * 窗口操作的监控
     *
     * @return
     */
    public Module windowMonitor() {
        return binder -> {
            binder.bindInterceptor(Matchers.subclassesOf(WindowCollector.class),
                    NoSyntheticMethodMatcher.INSTANCE.and(new MethodMatcher("shuffle")), new WindowMonitor());
        };
    }

    /**
     * exporter监控
     *
     * @return
     */
    public Module exporterMonitor() {
        return binder -> {
            binder.bindInterceptor(Matchers.subclassesOf(Exporter.class),
                    NoSyntheticMethodMatcher.INSTANCE.and(new MethodMatcher("export")), new ExporterMonitor());
        };
    }

    /**
     * jvm监控
     *
     * @return
     */
    public Initializer jvmMonitor() {
        return new JvmMonitor();
    }

    /**
     * 根据用户分片
     *
     * @return
     */
    public Sharding UIDSharding() {
        return new UIDSharding();
    }

    /**
     * 根据来源进行分片，用于将特定topic定位到对应的app上使用，而不是所有app都处理
     *
     * @return
     */
    public Sharding sourceSharding() {
        return new SourceSharding();
    }

    public Module mapperFilter() {
        MethodFilterInterceptor interceptor = new MapperFilterInterceptor();
        Method targetMethod = interceptor.interceptMethod();
        Class targetClass = targetMethod.getDeclaringClass();
        return binder -> binder
                .bindInterceptor(Matchers.subclassesOf(targetClass), NoSyntheticMethodMatcher.INSTANCE.and
                        (new AbstractMatcher<Method>() {
                            @Override
                            public boolean matches(Method method) {
                                return method.getDeclaringClass() == targetClass &&
                                        (Arrays.stream(targetMethod.getParameterTypes())
                                                .allMatch(paramClass -> Arrays.asList(method.getParameterTypes())
                                                        .contains(paramClass)));
                            }
                        }), interceptor);
    }
}
