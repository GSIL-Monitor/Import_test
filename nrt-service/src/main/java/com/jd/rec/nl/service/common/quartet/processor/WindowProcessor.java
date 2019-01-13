package com.jd.rec.nl.service.common.quartet.processor;

import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.experiment.MultiVersionHolder;
import com.jd.rec.nl.service.base.quartet.Schedule;
import com.jd.rec.nl.service.base.quartet.WindowCollector;
import com.jd.rec.nl.service.base.quartet.domain.MapResult;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.experiment.ExperimentVersionHolder;
import com.jd.rec.nl.service.common.quartet.LazyInitializer;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/7/29
 */
public class WindowProcessor implements LazyInitializer {

    private static final Logger LOGGER = getLogger(WindowProcessor.class);

    private ForkJoinPool forkJoinPool;

    private ExecutorService finishExecutor;

    private MultiVersionHolder<MapResult, WindowCollector> versionHolder = new ExperimentVersionHolder<>();

    public void collect(MapResult input) {
        try {
            versionHolder.getAllBranches().stream().filter(collector -> collector.getName().equals(input.getExecutorName()))
                    .findFirst().get().collect(input.getKey(), input.getValue());
        } catch (EnvironmentException e) {
            LOGGER.debug(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    /**
     * 触发操作
     *
     * @return 处理结果
     */

    public ResultCollection shuffle(int interval) {
        ResultCollection triggerResult = new ResultCollection();
//        Map<String, WindowCollector> windowMap = new HashMap<>();
//        versionHolder.getAllBranches().forEach(collector -> windowMap.put(collector.getName(), collector));

        ForkJoinTask task = forkJoinPool.submit(() -> {
            versionHolder.getAllBranches().parallelStream().forEach(collector -> {
                if (interval % collector.windowSize() == 0) {
                    collector.shuffle(triggerResult);
                }
            });
        });

        triggerResult.autoFinish(finishExecutor, task, Schedule.defaultInterval);
        return triggerResult;
    }

    public void init() {
        versionHolder.build();
        int branchTotalNum = versionHolder.getAllBranches().size();
        forkJoinPool = new ForkJoinPool(branchTotalNum == 0 ? 2 : branchTotalNum);
        finishExecutor = Executors.newFixedThreadPool(1);
    }

    public void setWindowCollectors(Collection<WindowCollector> collectors) {
        collectors.forEach(collector -> versionHolder.register(collector));

    }
}
