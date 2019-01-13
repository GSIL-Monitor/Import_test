package com.jd.rec.nl.service.common.quartet.processor;

import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.core.experiment.MultiVersionHolder;
import com.jd.rec.nl.service.base.quartet.OperatorFilter;
import com.jd.rec.nl.service.base.quartet.Schedule;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.experiment.ExperimentVersionHolder;
import com.jd.rec.nl.service.common.quartet.LazyInitializer;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/19
 */
public class UpdateProcessor implements LazyInitializer {

    private static final Logger LOGGER = getLogger(UpdateProcessor.class);

    private ForkJoinPool forkJoinPool;

    private ExecutorService finishExecutor;

    private MultiVersionHolder versionHolder = new ExperimentVersionHolder();
    private boolean hasSchedule = false;
    private ScheduleProcessor scheduleProcessor = new ScheduleProcessor();

    public boolean hasSchedule() {
        return hasSchedule;
    }

    public void setMappers(Collection<Updater> updaters) {
        updaters.forEach(updater -> {
            versionHolder.register(updater);
            if (updater instanceof Schedule) {
                hasSchedule = true;
            }
        });

    }

    public ResultCollection update(MapperContext context) {
        ResultCollection resultCollection = new ResultCollection();
        List<Updater> updaters = new ArrayList<>();
        Set<String> nodeNames = versionHolder.getNodesName();
        for (String name : nodeNames) {
            updaters.addAll(versionHolder.getMatchedBranches(name, context));
        }

        ForkJoinTask task = forkJoinPool.submit(() ->
                updaters.parallelStream().forEach(mapper -> {
                    try {
                        // 进行本身个性化过滤处理
                        if (mapper instanceof OperatorFilter && !((OperatorFilter) mapper).test(context)) {
                            return;
                        }
                        mapper.update(context, resultCollection);
                    } catch (InvalidDataException e) {
                        LOGGER.debug(String.format("%s mapValue event from %s[%s] error!", mapper.getName(),
                                context.getEventContent().getSource(), context.getEventContent().getUid()), e);
                        return;
                    } catch (Exception e) {
                        LOGGER.error(String.format("%s mapValue event from %s[%s] error!", mapper.getName(),
                                context.getEventContent().getSource(), context.getEventContent().getUid()), e);
                    }
                })
        );

        resultCollection.autoFinish(finishExecutor, task, Duration.ofSeconds(10));
        return resultCollection;
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
