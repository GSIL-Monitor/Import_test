package com.jd.rec.nl.toolkit.config;

import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.rec.nl.service.infrastructure.ConfigWatcher;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.curator.framework.CuratorFramework;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ZKConfigEditor {

    private static CuratorFramework zkClient =
            InjectorService.getCommonInjector().getInstance(ConfigWatcher.class).zkClient;

    public void refreshAllConfig() throws Exception {
        // 清理
        clearAllConfig();
        ConfigBase.getAppNames().stream().forEach(appName -> {
            try {
                String appPath = ConfigWatcher.appsPath.concat("/").concat(appName);
                String createrIp = MonitorUtils.getHost();
                String createTime = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
                zkClient.create().forPath(appPath, createrIp.concat(":").concat(createTime).getBytes());
                String config = ConfigBase.getAppConfig(appName).root().render(ConfigRenderOptions.concise());
                zkClient.create().forPath(appPath.concat("/0-0"), config.getBytes());
                Config expConfig = ConfigBase.getExperimentConfig(appName);
                if (expConfig != null && !expConfig.isEmpty()) {
                    expConfig.getConfigList("slots").stream().forEach(exp -> {
                        try {
                            long placementId = exp.getLong("placementId");
                            int expId = exp.getInt("expId");
                            String expConfigStr = exp.root().render(ConfigRenderOptions.concise());
                            StringBuilder path =
                                    new StringBuilder(appPath).append("/").append(placementId).append("-")
                                            .append(expId);
                            zkClient.create().forPath(path.toString(), expConfigStr.getBytes());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void clearAllConfig() throws Exception {
        zkClient.delete().deletingChildrenIfNeeded().forPath(ConfigWatcher.appsPath);
        String createrIp = MonitorUtils.getHost();
        String createTime = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        zkClient.create().forPath(ConfigWatcher.appsPath, createrIp.concat(":").concat(createTime).getBytes());
    }

    public void updateAppConfig(String appName) throws Exception {
        String appPath = ConfigWatcher.appsPath.concat("/").concat(appName);
        String createrIp = MonitorUtils.getHost();
        String createTime = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        zkClient.setData().forPath(appPath, createrIp.concat(":").concat(createTime).getBytes());
        String config = ConfigBase.getAppConfig(appName).root().render(ConfigRenderOptions.concise());
        zkClient.setData().forPath(appPath.concat("/0-0"), config.getBytes());
    }

    public void updateExpConfig(String appName, long placement, int expId) {
        String appPath = ConfigWatcher.appsPath.concat("/").concat(appName);
        ConfigBase.getExperimentConfig(appName).getConfigList("slots").stream()
                .filter(config -> config.getLong("placementId") == placement && config.getInt("expId") == expId)
                .forEach(exp -> {
                    String expConfigStr = exp.root().render(ConfigRenderOptions.concise());
                    StringBuilder path =
                            new StringBuilder(appPath).append("/").append(placement).append("-").append(expId);
                    try {
                        zkClient.setData().forPath(path.toString(), expConfigStr.getBytes());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public void changeAppStatus(String appName, boolean enable) throws Exception {
        String appPath = ConfigWatcher.appsPath.concat("/").concat(appName);
        String createrIp = MonitorUtils.getHost();
        String createTime = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        if (zkClient.checkExists().forPath(appPath) == null) {
            zkClient.create().forPath(appPath, createrIp.concat(":").concat(createTime).getBytes());
        } else {
            zkClient.setData().forPath(appPath, createrIp.concat(":").concat(createTime).getBytes());
        }
        changeExpStatus(appName, 0, 0, enable);
    }


    public void changeExpStatus(String appName, long placement, int expId, boolean enable) throws Exception {
        String appPath = ConfigWatcher.appsPath.concat("/").concat(appName);
        Config config;
        if (placement == 0 && expId == 0) {
            config = ConfigBase.getAppConfig(appName);
        } else {
            config = ConfigBase.getExperimentConfig(appName).getConfigList("slots").stream()
                    .filter(exp -> exp.getLong("placementId") == placement && exp.getInt("expId") == expId)
                    .map(exp -> (Config) exp)
                    .findFirst().orElseGet(ConfigFactory::empty);
        }

        if (!config.isEmpty()) {
            Map coverConfig = new HashMap();
            coverConfig.put("enable", enable);
            Config expConfig = ConfigFactory.parseMap(coverConfig).withFallback(config);

            String expConfigStr = expConfig.root().render(ConfigRenderOptions.concise());
            StringBuilder path =
                    new StringBuilder(appPath).append("/").append(placement).append("-").append(expId);
            try {
                if (zkClient.checkExists().forPath(path.toString()) == null) {
                    zkClient.create().forPath(path.toString(), expConfigStr.getBytes());
                } else {
                    zkClient.setData().forPath(path.toString(), expConfigStr.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
