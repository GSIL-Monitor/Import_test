package com.jd.rec.nl.connector.standalone;

import com.jd.rec.nl.connector.standalone.diff.JimdbSaveInterceptor;
import com.jd.rec.nl.connector.standalone.input.MockSourceBuilder;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.input.Source;
import com.jd.rec.nl.core.input.SourceInfo;
import com.jd.rec.nl.core.pubsub.EventPublisher;
import com.jd.rec.nl.service.base.quartet.WindowCollector;
import com.jd.rec.nl.service.base.quartet.domain.MapResult;
import com.jd.rec.nl.service.base.quartet.domain.OutputResult;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.experiment.ParameterChangeEvent;
import com.jd.rec.nl.service.common.quartet.ApplicationLoader;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.processor.*;
import com.jd.rec.nl.service.infrastructure.ConfigWatcher;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/5/16
 */
public class StandaloneRuntime {

    public static final String eventSource = "standalone-watcher";
    private static final Logger LOGGER = getLogger(StandaloneRuntime.class);
    private static final String debugFilePath = "./debugParams.conf";
    private static int windowNum = 0;
    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    Object windowLock = new Object();
    Object updateLock = new Object();
    private Source source;
    private ParseProcessor parseProcessor;
    private MapProcessor mapProcessor;
    private UpdateProcessor updateProcessor;
    private Map<Integer, KeyedUpdateProcessor> keyedUpdateProcessors = new HashMap<>();
    private ExportProcessor exportProcessor;
    private WindowProcessor windowProcessor;
    private ReduceProcessor reduceProcessor;

    //    public static void main(String[] args) throws Exception {
//        ConfigWatcher configWatcher = InjectorService.getCommonInjector().getInstance(ConfigWatcher.class);
//        Consumer<ChangedValue> consumer =
//                changedValue -> {
//                    System.out.println(String.format("%s : %s --> %s",
//                            changedValue.getType().toString(), changedValue.getPath(), changedValue.getValue()));
//                };
//        configWatcher.registerAppChangeListener(consumer, consumer);
//        System.out.println("register succeed");
//        Thread.sleep(3600000);
//        configWatcher.close();
//    }
    private long eventInterval = 100;

    public StandaloneRuntime() throws Exception {
        prepare();
    }

    public static void main(String[] args) throws Exception {
        StandaloneRuntime runtime = null;
        try {
            envPrepare();
            runtime = new StandaloneRuntime();
            LOGGER.debug("nrt init success!!");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(1);
            return;
        }
        runtime.run();
    }

    private static void envPrepare() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            System.setProperty("user.home", "/home/recpro/zhangweibin1/nrt-platform");
        }
        System.setProperty("JM.LOG.PATH", "/home/recpro/zhangweibin1/nrt-platform/logs");
    }

    public void run() throws InterruptedException {
        Message message;
        while (true) {
            while ((message = source.get()) != null) {
                try {
                    ImporterContext importerContext = parseProcessor.parse(message);
                    if (importerContext == null) {
                        continue;
                    }
                    MapperContext mapperContext = mapProcessor.map(importerContext);
                    if (mapperContext == null) {
                        continue;
                    }
                    ResultCollection result;
                    synchronized (updateLock) {
                        result = updateProcessor.update(mapperContext);
                        while (!result.isFinish()) {
                            //                        Thread.sleep(500);
                        }
                    }

                    if (result.size() == 0) {
                        continue;
                    }

                    List<OutputResult> outputs = new ArrayList<>();
                    result.getResults().stream().filter(mapResult -> mapResult.isFin())
                            .forEach(mapResult -> outputs.add((OutputResult) mapResult));

                    List<MapResult> keyedResults = new ArrayList<>();
                    result.getResults().stream().filter(mapResult -> !mapResult.isFin() && !mapResult.isForWindow())
                            .forEach(mapResult -> keyedResults.add(mapResult));

                    List<MapResult> forWindowResult = new ArrayList<>();
                    result.getResults().stream().filter(mapResult -> !mapResult.isFin() && mapResult.isForWindow())
                            .forEach(mapResult -> forWindowResult.add(mapResult));

                    // 循环处理keyed数据
                    List<MapResult> keyeds = new ArrayList<>(keyedResults);
                    synchronized (updateLock) {
                        for (int i = 1; i <= this.keyedUpdateProcessors.size(); i++) {
                            List<MapResult> newKeyeds = new ArrayList<>();
                            for (MapResult mapResult : keyeds) {
                                ResultCollection resultCollection = this.keyedUpdateProcessors.get(i).update(mapResult);
                                while (!resultCollection.isFinish()) {
                                }
                                resultCollection.getResults().stream().forEach(keyed -> {
                                    if (keyed.isFin()) {
                                        outputs.add((OutputResult) keyed);
                                    } else {
                                        if (keyed.isForWindow()) {
                                            forWindowResult.add(keyed);
                                        } else {
                                            newKeyeds.add(keyed);
                                        }
                                    }
                                });
                            }
                            keyeds = newKeyeds;
                        }
                    }
                    if (keyeds.size() > 0) {
                        throw new Exception("keyedUpdater 配置错误，存在输出数据无处理器处理的情况：" + keyeds.toString());
                    }

                    synchronized (windowLock) {
                        forWindowResult.stream()
                                .forEach(mapResult -> {
                                    try {
                                        serializeCheck(mapResult);
                                        windowProcessor.collect(mapResult);
                                    } catch (Exception e) {
                                        LOGGER.error(e.getMessage(), e);
                                    }
                                });
                    }

                    outputs.stream().forEach(outputResult -> {
                        try {
                            serializeCheck(outputResult);
                            exportProcessor.export(outputResult);
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    });

                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                } finally {
                    Thread.sleep(eventInterval);
                }
            }
            LOGGER.error("diff resout: " + JimdbSaveInterceptor.diffRes());
            Thread.sleep(5000L);
        }

    }

    /**
     * 做序列化校验
     *
     * @param obj
     * @param <T>
     * @throws IOException
     */
    private <T> void serializeCheck(T obj) throws IOException {
        try {
            OutputStream os = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(obj);
            os.close();
            ((ByteArrayOutputStream) os).toByteArray();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }

    private void prepareDebug() {
        File debugParamFile = new File(debugFilePath);
        if (debugParamFile.exists()) {
            Config debugConfig = ConfigFactory.parseFile(debugParamFile);
            if (debugConfig.hasPath("debug.apps")) {
                List<String> enableApps = debugConfig.getStringList("debug.apps");
                Set<String> allApps = ConfigBase.getAppNames();
                Map appConfig = allApps.stream()
                        .filter(appName -> !enableApps.contains(appName))
                        .collect(Collectors.toMap(name -> name,
                                name -> {
                                    Map<String, Boolean> value = new HashMap<>();
                                    value.put("enable", false);
                                    return value;
                                }));
                debugConfig = ConfigFactory.parseMap(appConfig).withFallback(debugConfig);
            }
            if (debugConfig.hasPath("debug.event_interval")) {
                eventInterval = debugConfig.getLong("debug.event_interval");
            }
            ConfigBase.setThreadConfig(debugConfig);
        }
    }

    private void prepare() throws Exception {
//        TimeUnit.SECONDS.sleep(4);
        prepareDebug();
        ApplicationLoader loader = new ApplicationLoader();
        loader.load();
        SourceInfo sourceInfo = loader.getSourceInfo();
        MockSourceBuilder builder = new MockSourceBuilder();
        source = builder.build(sourceInfo);
        parseProcessor = loader.getParseProcessor();

        mapProcessor = loader.getMapProcessor();

        updateProcessor = loader.getUpdateProcessor();

        for (int i = 1; i <= loader.keyedUpdaterTier(); i++) {
            this.keyedUpdateProcessors.put(i, loader.getKeyedUpdateProcessor(i));
        }

        exportProcessor = loader.getExportProcessor();

        windowProcessor = loader.getWindowProcessor();

        reduceProcessor = loader.getReduceProcessor();


        LOGGER.error("hasWindow={}", loader.hasWindow());
        executorService.scheduleAtFixedRate(() -> {
            int thisWindowNum = windowNum++;

            if (loader.hasSchedule()) {
                try {
                    LOGGER.debug("trigger schedule!");
                    ResultCollection results;
                    synchronized (updateLock) {
                        results = updateProcessor.scheduleTrigger(thisWindowNum);
                        while (!results.isFinish()) {
                            Thread.sleep(500L);
                        }
                    }
                    Collection<OutputResult> outputResults = results.getResults().stream()
                            .filter(mapResult -> mapResult.isFin()).map(mapResult -> (OutputResult) mapResult)
                            .collect(Collectors.toList());


                    results.getResults().stream().filter(mapResult -> (mapResult.isFin()) == false)
                            .forEach(mapResult -> windowProcessor.collect(mapResult));

                    outputResults.forEach(outputResult -> {
                        try {
                            this.exportProcessor.export(outputResult);
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    });

                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            if (loader.hasWindow()) {
                try {
                    LOGGER.debug("trigger reduce!");
                    ResultCollection results;
                    synchronized (windowLock) {
                        results = windowProcessor.shuffle(thisWindowNum);
                        while (!results.isFinish()) {
                            Thread.sleep(500L);
                        }
                    }
                    Collection<OutputResult> outputResults = results.getResults().stream()
                            .filter(mapResult -> mapResult.isFin()).map(mapResult -> (OutputResult) mapResult)
                            .collect(Collectors.toList());


                    results.getResults().stream().filter(mapResult -> (mapResult.isFin()) == false)
                            .forEach(mapResult -> reduceProcessor.gatherSharding(mapResult));
                    ResultCollection reduceResults = reduceProcessor.reduce();
                    while (!reduceResults.isFinish()) {
                        Thread.sleep(500L);
                    }

                    outputResults.addAll(reduceResults.getResults().stream().filter(mapResult -> mapResult instanceof
                            OutputResult).map(mapResult -> (OutputResult) mapResult).collect(Collectors.toList()));

                    Collection<MapResult> errorTypeResults = reduceResults.getResults().stream()
                            .filter(mapResult -> (mapResult.isFin()) == false).collect(Collectors.toList());
                    if (errorTypeResults.size() != 0) {
                        LOGGER.error("reducer return error result:{}", errorTypeResults.toString());
                    }
                    outputResults.forEach(outputResult -> {
                        try {
                            this.exportProcessor.export(outputResult);
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    });

                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }, 2, WindowCollector.defaultInterval.getSeconds(), TimeUnit.SECONDS);
        LOGGER.debug("init thread");

        ConfigWatcher configWatcher = InjectorService.getCommonInjector().getInstance(ConfigWatcher.class);
        EventPublisher eventPublisher = InjectorService.getCommonInjector().getInstance(EventPublisher.class);
        configWatcher.registerAppChangeListener(changedValue -> {
            ConfigWatcher.ChangeType changeType = changedValue.getType();
            String path = changedValue.getPath();
            String data = changedValue.getValue();
            int version = changedValue.getVersion();
            LOGGER.debug("{} : {} --> {} , {}", changeType, path, data, version);
            try {
                if (changeType == ConfigWatcher.ChangeType.disableApp) {
                    String appName = path.lastIndexOf("/") < 0 ? path : path.substring(path.lastIndexOf("/"));
                    Map configMap = new HashMap();
                    configMap.put("placementId", 0);
                    configMap.put("expId", 0);
                    configMap.put("enable", false);
                    ParameterChangeEvent event =
                            new ParameterChangeEvent(eventSource, appName, 0, 0, ConfigFactory.parseMap(configMap), -1);
                    eventPublisher.publishEvent(event);
                } else if (changeType == ConfigWatcher.ChangeType.enableExp ||
                        changeType == ConfigWatcher.ChangeType.experimentChange) {
                    String[] paths = path.split("/");
                    if (paths.length < 2) {
                        LOGGER.error("change value is wrong:{}", changedValue.toString());
                        return;
                    }
                    String appName = paths[paths.length - 2];
                    String[] node = paths[paths.length - 1].split("-");
                    ParameterChangeEvent event = new ParameterChangeEvent(eventSource, appName, Long.parseLong(node[0]),
                            Integer.parseInt(node[1]), ConfigFactory.parseString(data), version);
                    eventPublisher.publishEvent(event);
                } else if (changeType == ConfigWatcher.ChangeType.disableExp) {
                    String[] paths = path.split("/");
                    if (paths.length < 2) {
                        LOGGER.error("change value is wrong:{}", changedValue.toString());
                        return;
                    }
                    String appName = paths[paths.length - 2];
                    String[] node = paths[paths.length - 1].split("-");
                    Map configMap = new HashMap();
                    configMap.put("placementId", Long.parseLong(node[0]));
                    configMap.put("expId", Integer.parseInt(node[1]));
                    configMap.put("enable", false);
                    ParameterChangeEvent event = new ParameterChangeEvent(eventSource, appName, Long.parseLong(node[0]),
                            Integer.parseInt(node[1]), ConfigFactory.parseMap(configMap), -1);
                    eventPublisher.publishEvent(event);
                }
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }
}
