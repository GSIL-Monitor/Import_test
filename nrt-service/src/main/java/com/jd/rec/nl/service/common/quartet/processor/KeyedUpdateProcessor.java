package com.jd.rec.nl.service.common.quartet.processor;

import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.core.experiment.MultiVersionHolder;
import com.jd.rec.nl.service.base.quartet.KeyedUpdater;
import com.jd.rec.nl.service.base.quartet.OperatorFilter;
import com.jd.rec.nl.service.base.quartet.Schedule;
import com.jd.rec.nl.service.base.quartet.domain.MapResult;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.experiment.ExperimentVersionHolder;
import com.jd.rec.nl.service.common.quartet.LazyInitializer;
import org.slf4j.Logger;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 对按key分片后的数据的更新，用于处理那些非用户维度但又不是窗口操作的计算
 */
public class KeyedUpdateProcessor implements LazyInitializer {

    private static final Logger LOGGER = getLogger(KeyedUpdateProcessor.class);

    protected ForkJoinPool forkJoinPool;

    protected ExecutorService finishExecutor;

    protected MultiVersionHolder<Serializable, KeyedUpdater> versionHolder = new ExperimentVersionHolder();

    protected boolean hasSchedule = false;

    protected ScheduleProcessor scheduleProcessor = new ScheduleProcessor();

    public ResultCollection update(MapResult mapResult) {
        ResultCollection resultCollection = new ResultCollection();

        ForkJoinTask task = forkJoinPool.submit(() ->
                versionHolder.getAllBranches().parallelStream()
                        .filter(updater -> updater.getName().equals(mapResult.getExecutorName()))
                        .forEach(updater -> {
                            try {
                                // 进行本身个性化过滤处理
                                if (updater instanceof OperatorFilter && !((OperatorFilter) updater).test(mapResult)) {
                                    return;
                                }
                                updater.update(mapResult, resultCollection);
                            } catch (InvalidDataException e) {
                                LOGGER.debug(String.format("%s update message error -->[%s]%s", updater.getName(),
                                        mapResult.getKey(), mapResult.toString()), e);
                                return;
                            } catch (Exception e) {
                                LOGGER.error(String.format("%s update message error -->[%s]%s", updater.getName(),
                                        mapResult.getKey(), mapResult.toString()), e);
                                return;
                            }
                        })
        );

        resultCollection.autoFinish(finishExecutor, task, Duration.ofSeconds(10));
        return resultCollection;
    }

    public boolean hasSchedule() {
        return hasSchedule;
    }

    public void setMappers(Collection<KeyedUpdater> updaters) {
        updaters.stream().forEach(updater -> {
            versionHolder.register(updater);
            if (updater instanceof Schedule) {
                hasSchedule = true;
            }
        });
    }


    public ResultCollection scheduleTrigger(int interval) {
        return this.scheduleProcessor.trigger(interval);
    }

    public void init() {
        this.versionHolder.build();
        int branchTotalNum = 0;
        Set<String> nodeNames = versionHolder.getNodesName();
        for (String name : nodeNames) {
            branchTotalNum += versionHolder.getBranchNum(name);
        }
        forkJoinPool = new ForkJoinPool(branchTotalNum == 0 ? 2 : branchTotalNum);
        finishExecutor = Executors.newFixedThreadPool(1);
        if (hasSchedule) {
            scheduleProcessor.setVersionHolder(this.versionHolder);
            scheduleProcessor.init();
        }
    }
}
