package com.jd.rec.nl.core.cache.guava;


import javax.cache.CacheManager;
import javax.cache.configuration.OptionalFeature;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.util.Properties;

public class GuavaCacheProvider implements CachingProvider {

    private static final Object _lock = new Object();
    private static GuavaCacheManager storedCacheManager = null;

    // 缓存的属性
    private Properties properties;

    @Override
    public CacheManager getCacheManager(URI uri, ClassLoader classLoader, Properties properties) {
        return getCacheManager();
    }

    @Override
    public ClassLoader getDefaultClassLoader() {
        return getClass().getClassLoader();
    }

    /**
     * 不对多class loader进行支持，统一返回null
     *
     * @return null
     */
    @Override
    public URI getDefaultURI() {
        return null;
    }

    @Override
    public Properties getDefaultProperties() {
        return this.properties;
    }

    /**
     * 因为不对多class loader进行支持，因此只会返回一个cachemanager，内部调用无参的getCacheManager方法
     *
     * @return CacheManager
     */
    @Override
    public CacheManager getCacheManager(URI uri, ClassLoader classLoader) {
        return getCacheManager();
    }

    /**
     * 获取缓存的cacheManager，如果不存在则根据配置生成
     *
     * @return CacheManager
     */
    @Override
    public CacheManager getCacheManager() {
        if (storedCacheManager == null) {
            synchronized (_lock) {
                if (storedCacheManager == null) {
                    storedCacheManager = new GuavaCacheManager(this, this.properties);
                }
            }
        }
        return storedCacheManager;
    }

    @Override
    public void close() {
        storedCacheManager = null;
    }

    /**
     * 因为不对多class loader进行支持，因此内部调用无参的close方法
     *
     * @param classLoader 类加载器
     */
    @Override
    public void close(ClassLoader classLoader) {
        this.close();
    }

    /**
     * 因为不对多class loader进行支持，因此内部调用无参的close方法
     *
     * @param classLoader 类加载器
     */
    @Override
    public void close(URI uri, ClassLoader classLoader) {
        this.close();
    }

    @Override
    public boolean isSupported(OptionalFeature optionalFeature) {
        return optionalFeature.equals(OptionalFeature.STORE_BY_REFERENCE);
    }

}
