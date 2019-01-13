package com.jd.rec.nl.service.common.quartet.operator.impl;

import com.google.inject.Inject;
import com.jd.jim.cli.PipelineClient;
import com.jd.jim.cli.driver.types.DataType;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.exception.WrongConfigException;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.core.utils.MonitorUtils;
import com.jd.rec.nl.service.base.quartet.Exporter;
import com.jd.rec.nl.service.base.quartet.domain.OutputResult;
import com.jd.rec.nl.service.infrastructure.Jimdb;
import com.jd.ump.profiler.proxy.Profiler;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static org.slf4j.LoggerFactory.getLogger;

public class JimDbExporter implements Exporter {

    private static final Logger LOGGER = getLogger(JimDbExporter.class);

    @Inject
    Jimdb jimdb;

    ExecutorService executorService;

    private String name;

    @Inject
    @ENV("dbservice.default_live_time")
    private Duration defaultLiveTime;

    private String hostName;

    private LinkedBlockingDeque<OutputResult> waitToSave = new LinkedBlockingDeque<>();

    private int maxSize = 500;

    private int batchSize = 200;

    public JimDbExporter() {
        hostName = MonitorUtils.getHostIP();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void export(OutputResult result) {
        prepare();

        if (result.getSubType() == 4 || result.getSubType() == 5 || result.getSubType() == 12) {
            waitToSave.addLast(result);
        } else {
            throw new WrongConfigException("The setting of subtype is wrong");
        }
    }

    private void prepare() {
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(3);
            Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(() -> {
                while (waitToSave.size() > maxSize) {
                    int i = 0;
                    Map<String, OutputResult> temp = new HashMap<>();
                    while (temp.size() < batchSize) {
                        OutputResult result = waitToSave.pollFirst();
                        if (result == null) {
                            break;
                        }
                        String key = result.getKey().concat("-").concat(String.valueOf(result.getSubType()));
                        temp.put(key, result);
                    }
                    if (!temp.isEmpty()) {
                        List<Future> futureList = new ArrayList<>();
                        for (CLUSTER cluster : CLUSTER.values()) {
                            futureList.add(executorService.submit(() -> {
                                PipelineClient pipelineClient = null;
                                try {
                                    pipelineClient = jimdb.getCluster(cluster).pipelineClient();

                                    for (OutputResult result : temp.values()) {
                                        String subType;
                                        if (result.getSubType() == 4) {
                                             subType = "nl_cid3_preference";
                                        } else if (result.getSubType() == 5) {
                                            subType = "nl_prc_grade";
                                        }else {
                                             subType = "nl_rbcid_blacklist";
                                        }
                                        pipelineClient.hSet(result.getKey().getBytes(), subType.getBytes(), result.getValue());
                                        String info = hostName.concat(":").concat(DateFormatUtils.format(new Date(),
                                                "MMdd-HHmmss"));
                                        pipelineClient.hSet(result.getKey().getBytes(), "platform_editInfo".getBytes(), info
                                                .getBytes());

                                        Duration ttl = result.getTtl() == null ? defaultLiveTime : result.getTtl();
                                        pipelineClient.expire(result.getKey().getBytes(), ttl.getSeconds(), TimeUnit.SECONDS);
                                    }
                                    pipelineClient.flushAndReturnAll();
                                } catch (Exception e) {
                                    LOGGER.error(e.getMessage());
                                } finally {
                                    if (pipelineClient != null) {
                                        pipelineClient.close();
                                    }
                                }
                            }));
                        }
                        for (Future future : futureList) {
                            if (!future.isDone()) {
                                try {
                                    future.get();
                                } catch (Exception e) {
                                    LOGGER.error(e.getMessage());
                                }
                            }
                        }
                    }
                }
            }, 50L, 50L, TimeUnit.MILLISECONDS);
            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                if (waitToSave.size() > maxSize)
                    LOGGER.error("jimdb exporter cache size:{}", waitToSave.size());
            }, 5, 5, TimeUnit.MINUTES);
        }
    }

    private void exportData(OutputResult result, String subType) {
        try {
            for (CLUSTER cluster : CLUSTER.values()) {
                if (jimdb.getCluster(cluster).type(result.getKey().getBytes()) != DataType.HASH
                        && jimdb.getCluster(cluster).type(result.getKey().getBytes()) != DataType.NONE) {
                    Profiler.countAccumulate("nrt-jimdb-wrongType");
                    jimdb.getCluster(cluster).del(result.getKey().getBytes());
                }
                jimdb.getCluster(cluster).hSet(result.getKey().getBytes(), subType.getBytes(), result.getValue());
                String info = hostName.concat(":").concat(DateFormatUtils.format(new Date(), "MMdd-HHmmss"));
                jimdb.getCluster(cluster).hSet(result.getKey().getBytes(), "platform_editInfo".getBytes(), info.getBytes());
                Duration ttl = result.getTtl() == null ? defaultLiveTime : result.getTtl();
                jimdb.getCluster(cluster).expire(result.getKey().getBytes(), ttl.getSeconds(), TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            throw new EnvironmentException(e.getMessage(), e);
        }
    }
}

