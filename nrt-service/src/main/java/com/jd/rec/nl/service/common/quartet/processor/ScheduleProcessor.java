package com.jd.rec.nl.service.common.quartet.processor;

import com.jd.rec.nl.core.experiment.MultiVersionHolder;
import com.jd.rec.nl.service.base.quartet.Schedule;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.LazyInitializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * @author linmx
 * @date 2018/9/25
 */
public class ScheduleProcessor implements LazyInitializer {

    MultiVersionHolder versionHolder;

    ForkJoinPool forkJoinPool;

    private ExecutorService finishExecutor;

    public void setVersionHolder(MultiVersionHolder versionHolder) {
        this.versionHolder = versionHolder;
    }

    public ResultCollection trigger(int interval) {
        ResultCollection results = new ResultCollection();
        Map<String, Schedule> scheduleMap = new HashMap<>();
        versionHolder.getAllBranches().stream().filter(updater -> (updater instanceof Schedule)).forEach(schedule ->
                scheduleMap.put
                        (((Schedule) schedule).getName(), (Schedule) schedule));
        if (scheduleMap.isEmpty()) {
            results.finish();
            return results;
        }
        ForkJoinTask task = forkJoinPool.submit(() -> {
            scheduleMap.entrySet().parallelStream().forEach(entry -> {
                String scheduleName = entry.getKey();
                Schedule schedule = entry.getValue();
                if (interval % schedule.intervalSize() == 0) {
                    schedule.trigger(results);
                }
            });
        });
        results.autoFinish(finishExecutor, task, Duration.ofSeconds(10));
        return results;
    }

    @Override
    public void init() {
        int size = Long.valueOf(
                versionHolder.getAllBranches().stream().filter(updater -> (updater instanceof Schedule)).count
                        ()).intValue();
        if (size > 0) {
            forkJoinPool = new ForkJoinPool(size == 1 ? 2 : (size > 5 ? 5 : size));
            finishExecutor = Executors.newFixedThreadPool(1);
        }
    }
}
