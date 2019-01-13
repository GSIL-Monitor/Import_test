package com.jd.rec.nl.service.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode.BUILD_INITIAL_CACHE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/11/15
 */
@Singleton
public class ConfigWatcher implements BaseInfrastructure {

    private static final Logger LOGGER = getLogger(ConfigWatcher.class);

    public static String nameSpace = "NRT/config";

    public static String appsPath = "/app";

    public CuratorFramework zkClient;

    public Map<String, PathChildrenCache> listeners = new ConcurrentHashMap<>();

    @Inject
    public void initClient(@ENV("zkWatcher.brokers") String brokers,
                           @ENV("zkWatcher.connect_timeout") int conTimeout,
                           @ENV("zkWatcher.session_timeout") int sessionTimeout) {
        RetryPolicy rp = new ExponentialBackoffRetry(1000, 5);// 重试机制
        CuratorFrameworkFactory.Builder builder =
                CuratorFrameworkFactory.builder().connectString(brokers).connectionTimeoutMs(conTimeout)
                        .sessionTimeoutMs(sessionTimeout).retryPolicy(rp);
        builder.namespace(nameSpace);
        CuratorFramework zclient = builder.build();
        zkClient = zclient;
        zkClient.start();
    }

    /**
     * 注册对app参数的监控
     *
     * @param configChangeConsumer 对app参数的监控，包括实验参数
     */
    public void registerAppChangeListener(final Consumer<ChangedValue> configChangeConsumer) {
        try {
            PathChildrenCache cache = getListener(appsPath);
            // 监听app根节点,针对app上下线情况
            PathChildrenCacheListener appsListener = (client, event) -> {

                switch (event.getType()) {
                    case CHILD_ADDED: {
                        LOGGER.debug("appNode add: {}", event.toString());
                        String changedPath = event.getData().getPath();
                        // 注册实验的变化
                        registerExpChangeListener(changedPath, configChangeConsumer);
                        break;
                    }

                    case CHILD_UPDATED: {
                        LOGGER.debug("appNode changed: {}", event.toString());
                        //                    ChangedValue changedValue = new ChangedValue(ChangeType.enableApp, new String(event.getData().getData()), event.getData().getPath());
                        //                    consumer.accept(changedValue);
                        break;
                    }

                    case CHILD_REMOVED: {
                        LOGGER.debug("appNode removed: {}", event.toString());
                        ChangedValue changedValue = new ChangedValue(ChangeType.disableApp);
                        String changedPath = event.getData().getPath();
                        changedValue.setPath(changedPath);
                        // 关闭对实验的listener
                        if (listeners.containsKey(changedPath)) {
                            listeners.remove(changedPath).close();
                        }
                        configChangeConsumer.accept(changedValue);
                        break;
                    }
                }
            };
            // 注册监听
            cache.getListenable().addListener(appsListener);

            // 对app中的配置增加监控
            zkClient.getChildren().forPath(appsPath).stream().forEach(
                    childPath -> {
                        String path = childPath.startsWith("/") ? appsPath.concat(childPath) :
                                appsPath.concat("/").concat(childPath);
                        registerExpChangeListener(path, configChangeConsumer);
                    }
            );
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private PathChildrenCache getListener(String appPath) throws Exception {
        if (listeners.containsKey(appPath)) {
            return listeners.get(appPath);
        } else {
            PathChildrenCache cache = new PathChildrenCache(zkClient, appPath, true);
            listeners.put(appPath, cache);
            cache.start(BUILD_INITIAL_CACHE);
            return cache;
        }
    }

    protected void registerExpChangeListener(final String appPath, final Consumer<ChangedValue> consumer) {
        LOGGER.debug("register app [{}] experiment listener!", appPath);
        try {
            PathChildrenCache cache = getListener(appPath);
            // 监听app节点,针对app的实验的上下线情况
            PathChildrenCacheListener appsListener = (client, event) -> {

                switch (event.getType()) {
                    case CHILD_ADDED: {
                        LOGGER.debug("exp add: {}", event.toString());
                        ChangedValue changedValue = new ChangedValue(ChangeType.enableExp);
                        String changedPath = event.getData().getPath();
                        changedValue.setPath(changedPath);
                        changedValue.setValue(new String(zkClient.getData().forPath(changedPath)));
                        changedValue.setVersion(event.getData().getStat().getVersion());
                        consumer.accept(changedValue);
                        break;
                    }

                    case CHILD_UPDATED: {
                        LOGGER.debug("exp changed: {}", event.toString());
                        ChangedValue changedValue = new ChangedValue(ChangeType.experimentChange);
                        String changedPath = event.getData().getPath();
                        changedValue.setPath(changedPath);
                        changedValue.setValue(new String(zkClient.getData().forPath(changedPath)));
                        changedValue.setVersion(event.getData().getStat().getVersion());
                        consumer.accept(changedValue);
                        break;
                    }

                    case CHILD_REMOVED: {
                        LOGGER.debug("exp removed: {}", event.toString());
                        ChangedValue changedValue = new ChangedValue(ChangeType.disableExp);
                        String changedPath = event.getData().getPath();
                        changedValue.setPath(changedPath);
                        // 关闭对实验的listener
                        if (listeners.containsKey(changedPath)) {
                            listeners.remove(changedPath).close();
                        }
                        consumer.accept(changedValue);
                        break;
                    }
                }
            };
            // 注册监听
            cache.getListenable().addListener(appsListener);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void close() {
        listeners.values().stream().forEach(listener -> {
            try {
                listener.close();
            } catch (IOException e) {
            }
        });
    }

    public enum ChangeType {
        enableApp, disableApp, enableExp, disableExp, experimentChange
    }

    public class ChangedValue {
        ChangeType type;

        String value;

        String path;

        int version;

        public ChangedValue(ChangeType type) {
            this.type = type;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public ChangeType getType() {
            return type;
        }

        public void setType(ChangeType type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

}
