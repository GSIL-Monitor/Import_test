package com.jd.rec.nl.service.common.quartet;

import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.exception.WrongConfigException;
import com.jd.rec.nl.core.guice.ApplicationContext;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.input.SourceInfo;
import com.jd.rec.nl.core.input.domain.SourceConfig;
import com.jd.rec.nl.service.base.quartet.*;
import com.jd.rec.nl.service.base.quartet.domain.KafkaConfig;
import com.jd.rec.nl.service.common.quartet.domain.RequiredDataInfo;
import com.jd.rec.nl.service.common.quartet.processor.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * @author linmx
 * @date 2018/6/4
 */
public class ApplicationLoader implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationLoader.class);

    // 所有涉及的消息来源
//    private Set<SourceConfig> sourceConfigs = new HashSet<>();
    private Map<String, SourceConfig> sourceMap = new HashMap<>();

    private Map<String, Updater> updaters = new HashMap<>();

    private Map<Integer, List<KeyedUpdater>> keyedUpdaters = new HashMap<>();

    private Map<String, WindowCollector> windowCollectors = new HashMap<>();

    private Map<String, Reducer> reducers = new HashMap<>();

    private List<RequiredDataInfo> requiredDataInfoList = new ArrayList<>();

    // 自定义的exporter
    private Map<String, Exporter> exporters = new HashMap<>();

    private boolean hasSchedule = false;

    public boolean hasSchedule() {
        return hasSchedule;
    }

    /**
     * 获取数据来源信息
     *
     * @return
     */
    public SourceInfo getSourceInfo() {
        return new SourceInfo(new HashSet<>(sourceMap.values()));
    }

    /**
     * 获取消息解析处理器
     *
     * @return
     */
    public ParseProcessor getParseProcessor() {
        ParseProcessor parseProcessor = InjectorService.getCommonInjector().getInstance(ParseProcessor.class);
        parseProcessor.init();
        return parseProcessor;
    }

    /**
     * 获取数据准备处理器
     *
     * @return
     */
    public MapProcessor getMapProcessor() {
        MapProcessor mapProcessor = InjectorService.getCommonInjector().getInstance(MapProcessor.class);
        mapProcessor.setRequiredData(requiredDataInfoList);
        mapProcessor.init();
        return mapProcessor;
    }

    /**
     * 获取更新处理器
     *
     * @return
     */
    public UpdateProcessor getUpdateProcessor() {
        UpdateProcessor updateProcessor = InjectorService.getCommonInjector().getInstance(UpdateProcessor.class);
        updateProcessor.setMappers(this.updaters.values());
        updateProcessor.init();
        return updateProcessor;
    }

    public KeyedUpdateProcessor getKeyedUpdateProcessor(int order) {
        KeyedUpdateProcessor keyedUpdateProcessor =
                InjectorService.getCommonInjector().getInstance(KeyedUpdateProcessor.class);
        if (this.keyedUpdaters.get(order) != null) {
            keyedUpdateProcessor.setMappers(this.keyedUpdaters.get(order));
        }
        keyedUpdateProcessor.init();
        return keyedUpdateProcessor;
    }

    public WindowProcessor getWindowProcessor() {
        WindowProcessor windowProcessor = InjectorService.getCommonInjector().getInstance(WindowProcessor.class);
        windowProcessor.setWindowCollectors(this.windowCollectors.values());
        windowProcessor.init();
        return windowProcessor;
    }

    /**
     * 获取统计处理器,跟在窗口处理之后
     *
     * @return
     */
    public ReduceProcessor getReduceProcessor() {
        ReduceProcessor reduceProcessor = InjectorService.getCommonInjector().getInstance(ReduceProcessor.class);
        reduceProcessor.setReducers(this.reducers.values());
        reduceProcessor.init();
        return reduceProcessor;
    }

    /**
     * 获取输出处理器
     *
     * @return
     */
    public ExportProcessor getExportProcessor() {
        ExportProcessor exportProcessor = InjectorService.getCommonInjector().getInstance(ExportProcessor.class);
        exportProcessor.setExporters(exporters.values());
        exportProcessor.init();
        return exportProcessor;
    }

    private void createSourceConfig(Config appConfig) {
        boolean enable = true;
        Config importerConfig = appConfig.getConfig("import");
        if (appConfig.hasPath("enable")) {
            enable = appConfig.getBoolean("enable");
        }
        for (Map.Entry<String, ConfigValue> entry : importerConfig.root().entrySet()) {
            Config kafkaSourceConfig = entry.getValue().atKey(entry.getKey());
            String topic = kafkaSourceConfig.getString(
                    kafkaSourceConfig.root().keySet().iterator().next() + ".topic");
            if (Objects.nonNull(sourceMap.get(topic))) {
                KafkaConfig sourceConfig = (KafkaConfig) sourceMap.get(topic);
                if (!sourceConfig.getEnable() && enable) {
                    sourceConfig.setEnable(enable);
                }
            } else {
                KafkaConfig kafkaConfig = KafkaConfig.parseConfig(entry.getValue().atKey(entry.getKey()), enable);
                sourceMap.put(topic, kafkaConfig);
            }
        }
    }

    /**
     * 加载app定义的updater和exporter
     */
    public void load() {
        loadOperator(this.updaters, Updater.class);
        this.updaters.entrySet().stream().forEach(updater -> {
            String appName = updater.getKey();
            Config appConfig = ConfigBase.getAppConfig(appName);
            if (appConfig.hasPath("import")) {
//                Config importerConfig = appConfig.getConfig("import");
                createSourceConfig(appConfig);
            }
            RequiredDataInfo dataInfo = new RequiredDataInfo();
            dataInfo.setName(appName);
            requiredDataInfoList.add(dataInfo);
        });

        loadKeyedUpdaters();

        loadOperator(this.windowCollectors, WindowCollector.class);

        loadOperator(this.reducers, Reducer.class);

        loadOperator(this.exporters, Exporter.class);
    }

    private void loadKeyedUpdaters() {
        Map<String, List<Integer>> validCheck = new HashMap<>();
        ApplicationContext.getRawDefinedBeans(KeyedUpdater.class).stream().forEach(keyedUpdater -> {
            int tier = keyedUpdater.order();
            if (keyedUpdaters.containsKey(tier)) {
                keyedUpdaters.get(tier).add(keyedUpdater);
            } else {
                List<KeyedUpdater> updaters = new ArrayList<>();
                updaters.add(keyedUpdater);
                keyedUpdaters.put(tier, updaters);
            }
            String appName = keyedUpdater.getName();
            if (validCheck.containsKey(appName)) {
                validCheck.get(appName).add(tier);
            } else {
                List<Integer> tiers = new ArrayList<>();
                tiers.add(tier);
                validCheck.put(appName, tiers);
            }
        });
        // 校验配置是否正确
        validCheck.forEach((appName, tiers) -> {
            int max = tiers.stream().mapToInt(Integer::intValue).max().getAsInt();
            if (max != tiers.size()) {
                throw new WrongConfigException(String.format("app[%s] 多层级分片配置错误，层级数应保证连续：%s", tiers.toString()));
            }
        });
    }

    private <O extends Operator> void loadOperator(Map<String, O> operators, Class<O> type) {
        ApplicationContext.getRawDefinedBeans(type).stream()
                .forEach(operator -> {
                    if (operator instanceof Schedule) {
                        this.hasSchedule = true;
                    }
                    operators.put(operator.getName(), operator);
                });
    }

    public boolean hasWindow() {
        return !this.windowCollectors.isEmpty();
    }

    public boolean hasReduce() {
        return !this.reducers.isEmpty();
    }

    public int keyedUpdaterTier() {
        return this.keyedUpdaters.size();
    }
}
