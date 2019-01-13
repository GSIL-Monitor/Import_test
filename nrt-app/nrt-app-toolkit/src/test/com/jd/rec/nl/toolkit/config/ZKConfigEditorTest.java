package com.jd.rec.nl.toolkit.config;

import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigRenderOptions;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;

class ZKConfigEditorTest {

    @Test
    public void testConfig() {
        ConfigBase.getAppNames().stream().forEach(appName -> {
            try {
                String appPath = "/".concat(appName);
                String createrIp = MonitorUtils.getHost();
                String createTime = DateFormatUtils.format(new Date(), "yyyyMMdd HHmmss");
                System.out
                        .println(appPath.concat("---->").concat(createrIp).concat(":").concat(createTime));
                String config =  ConfigBase.getAppConfig(appName).root().render(ConfigRenderOptions.concise());
                System.out.println(appPath.concat("/0-0").concat("---->").concat(config));
                Config expConfig = ConfigBase.getExperimentConfig(appName);
                if (expConfig != null && !expConfig.isEmpty()) {
                    ConfigBase.getExperimentConfig(appName).getConfigList("slots").stream().forEach(exp -> {
                        try {
                            long placementId = exp.getLong("placementId");
                            int expId = exp.getInt("expId");
                            String expConfigStr = exp.root().render(ConfigRenderOptions.concise());
                            StringBuilder path =
                                    new StringBuilder(appPath).append("/").append(placementId).append("-")
                                            .append(expId);
                            System.out.println(path.toString().concat("---->").concat(expConfigStr));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                System.out.println("");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}