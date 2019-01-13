package com.jd.rec.nl.core.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 统一管理配置文件,作为配置的统一出口
 * 优先级:threadLoad > experiment > business params > system params
 *
 * @author linmx
 * @date 2018/5/16
 */
public class ConfigBase {

    private static final Logger LOGGER = getLogger(ConfigBase.class);

    /**
     * 系统参数
     */
    private static Config systemConfig = ConfigFactory.load().withoutPath("business").resolve();

    /**
     * 业务参数,一般就是executor的配置参数
     */
    private static Config businessConfig = ConfigFactory.load().hasPath("business") ? ConfigFactory.load().getConfig
            ("business") : ConfigFactory.empty();

    /**
     * 实验参数
     */
    private static Config experimentConfig = ConfigFactory.load("modules/experiment");

    private static ThreadLocal<Config> templeConfig = new InheritableThreadLocal<>();

    //    static {
    //        URL modules = Thread.currentThread().getContextClassLoader().getResource("modules/");
    //        if (modules != null) {
    //            String protocol = modules.getProtocol();
    //            String pkgPath = modules.getPath();
    //            if ("file".equals(protocol)) {
    //                File file = new File(modules.getPath());
    //                LOGGER.info("init module configs in {}", file.getPath());
    //                if (file.exists()) {
    //                    for (File child : file.listFiles()) {
    //                        LOGGER.info("init config from {}", child.getName());
    //                        businessConfig = businessConfig.withFallback(ConfigFactory.parseFile(child).resolve());
    //                    }
    //                }
    //            } else {
    //                businessConfig = businessConfig.withFallback(ConfigFactory.load(modules));
    //            }
    //        }
    //    }

    private static Config addThreadLocal(Config config) {
        if (templeConfig.get() != null) {
            return templeConfig.get().withFallback(config);
        } else {
            return config;
        }
    }

    public static void setThreadConfig(Config config) {
        if (templeConfig.get() == null) {
            templeConfig.set(config);
        }
    }

    public static Config getSystemConfig() {
        return addThreadLocal(systemConfig);
    }

    /**
     * 查询定义的所有app的name
     *
     * @return
     */
    public static Set<String> getAppNames() {
        return businessConfig.withoutPath("source").root().keySet();
    }

    /**
     * 获取某个app的配置,不包含系统参数,不涉及实验参数
     *
     * @param appName
     * @return
     */
    public static Config getAppConfig(String appName) {
        Config exeConfig;
        if (businessConfig.hasPath(appName)) {
            exeConfig = businessConfig.getConfig(appName);
        } else {
            exeConfig = ConfigFactory.empty();
        }
        if (templeConfig.get() != null && templeConfig.get().hasPath(appName)) {
            exeConfig = templeConfig.get().getConfig(appName).withFallback(exeConfig);
        }
        return exeConfig;
    }

    /**
     * 获取某个executor的实验配置
     *
     * @param executorName
     * @return
     */
    public static Config getExperimentConfig(String executorName) {
        Config expConfig = ConfigFactory.empty();
        if (experimentConfig.hasPath(executorName)) {
            expConfig = experimentConfig.getConfig(executorName);
        }
        if (templeConfig.get() != null && templeConfig.get().hasPath("experiment.".concat(executorName))) {
            expConfig = templeConfig.get().getConfig("experiment.".concat(executorName)).withFallback(expConfig);
        }
        return expConfig;
    }
}
