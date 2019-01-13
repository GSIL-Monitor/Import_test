package com.jd.rec.nl.service.common.experiment;

import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.core.pubsub.ApplicationListener;
import com.jd.rec.nl.core.pubsub.EventListenerRegister;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/5/17
 */
public class ExperimentGroup<N extends Named> implements ApplicationListener<ParameterChangeEvent> {

    private static final Logger LOGGER = getLogger(ExperimentGroup.class);

    /**
     * 原始的处理器
     */
    private N rawNamed;

    /**
     * 实验信息
     */
    private volatile Map<String, ExperimentInstance> experimentInstances = new HashMap<>();

    /**
     * 基础配置参数
     */
    private Config appParams;

    private Map<String, Config> experimentParams = new HashMap<>();

    /**
     * 是否上线,默认为true
     * 当为false时,仅为命中bucket的事件做处理,一般在上线前或实验阶段时配置使用
     */
    private boolean isOnline = true;

    public ExperimentGroup(N named) {
        this.rawNamed = named;
        String appName = named.getName();
        EventListenerRegister.register(this);
        this.appParams = ConfigBase.getAppConfig(appName);

        if (this.appParams.hasPath("enable") && !this.appParams.getBoolean("enable")) {
            return;
        }
        Config experimentConfig = ConfigBase.getExperimentConfig(appName);
        if (experimentConfig != null && !experimentConfig.isEmpty()) {
            experimentConfig.getConfigList("slots").stream()
                    .filter(expConfig -> !expConfig.hasPath("enable") || expConfig.getBoolean("enable"))
                    .map(expConfig ->
                            new ExperimentInstance(expConfig.withFallback(this.appParams), named))
                    .forEach(experimentInstance -> {
                        this.experimentInstances.put(experimentInstance.getName(), experimentInstance);
                    });
            // 缓存配置的实验参数，后续变更时仅变更缓存的数据，不对原始数据做变化
            experimentConfig.getConfigList("slots").stream()
                    .filter(expConfig -> expConfig.hasPath("placementId") && expConfig.hasPath("expId"))
                    .forEach(expConfig -> {
                        String name = new StringBuilder().append(expConfig.getLong("placementId"))
                                .append(expConfig.getInt("expId")).toString();
                        this.experimentParams.put(name, expConfig);
                    });
        }
        if (appParams.hasPath("isOnline")) {
            isOnline = appParams.getBoolean("isOnline");
        }
        if (isOnline) {
            ExperimentInstance experimentInstance = new ExperimentInstance(this.appParams, named);
            this.experimentInstances.put(experimentInstance.getName(), experimentInstance);
        }
    }

    public String getAppName() {
        return rawNamed.getName();
    }

    public Collection<N> getAllExecutor() {
        List<N> executors = new ArrayList<N>();
        this.experimentInstances.values().stream().map(experimentInstance -> experimentInstance.getExecutor())
                .forEach(executor -> executors.add((N) executor));
        return executors;
    }

    public Collection<N> getMatched(Serializable input) {
        List<N> executors = new ArrayList<N>();
        this.experimentInstances.values().stream().filter(experimentInstance -> experimentInstance.match(input)).map
                (experimentInstance -> experimentInstance.getExecutor())
                .forEach(executor -> executors.add((N) executor));
        return executors;
    }

    @Override
    public void onEvent(ParameterChangeEvent event) {
        if (event.getAppName().equals(this.rawNamed.getName())) {
            LOGGER.warn("{} receive a event : {}", this.getAppName(), event.toString());
            // 说明变更的是本实验组
            if (event.getPlacementId() == 0 && event.getExpId() == 0) {
                // 说明是原始参数发生改变,初始化整个实验组
                this.appParams = event.getParam().withFallback(this.appParams);
                if (this.appParams.hasPath("enable") && !this.appParams.getBoolean("enable")) {
                    this.experimentInstances.clear();
                    return;
                }
                Map<String, ExperimentInstance> newInstance = new HashMap<>();
                this.experimentParams.values().stream()
                        .filter(expConfig -> !expConfig.hasPath("enable") || expConfig.getBoolean("enable"))
                        .map(expConfig -> new ExperimentInstance(expConfig.withFallback(this.appParams), rawNamed))
                        .forEach(experimentInstance -> newInstance
                                .put(experimentInstance.getName(), experimentInstance));

                if (appParams.hasPath("isOnline")) {
                    isOnline = appParams.getBoolean("isOnline");
                }
                if (isOnline) {
                    ExperimentInstance experimentInstance = new ExperimentInstance(this.appParams, rawNamed);
                    newInstance.put(experimentInstance.getName(), experimentInstance);
                }
                this.experimentInstances = newInstance;
            } else {
                Config changedConfig = event.getParam();
                String expName = new StringBuilder(event.getAppName()).append(ExperimentInstance.SEP_TAB).append(event
                        .getPlacementId()).append(ExperimentInstance.SEP_TAB).append(event.getExpId()).toString();
                // 更新实验配置缓存
                String name = new StringBuilder().append(changedConfig.getLong("placementId"))
                        .append(changedConfig.getInt("expId")).toString();
                this.experimentParams.put(name, changedConfig);

                if (changedConfig.hasPath("enable") && !changedConfig.getBoolean("enable")) {
                    // 说明是下线实验
                    this.experimentInstances.remove(expName);
                } else {
                    Map configMap = new HashMap();
                    if (!changedConfig.hasPath("placementId")) {
                        configMap.put("placementId", event.getPlacementId());
                    }
                    if (!changedConfig.hasPath("expId")) {
                        configMap.put("expId", event.getExpId());
                    }
                    if (configMap.size() > 0) {
                        changedConfig = changedConfig.withFallback(ConfigFactory.parseMap(configMap));
                    }
                    // 更新对应的实验实例,直接新建实例,如果是新增实验也能生效
                    ExperimentInstance experimentInstance =
                            new ExperimentInstance(changedConfig.withFallback(this.appParams),
                                    rawNamed);
                    this.experimentInstances.put(expName, experimentInstance);
                }
            }
        }
    }

    @Override
    public Class<ParameterChangeEvent> eventType() {
        return ParameterChangeEvent.class;
    }
}
