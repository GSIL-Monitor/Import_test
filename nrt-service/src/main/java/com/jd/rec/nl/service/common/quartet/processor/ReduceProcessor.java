package com.jd.rec.nl.service.common.quartet.processor;

import com.jd.rec.nl.service.base.quartet.Reducer;
import com.jd.rec.nl.service.base.quartet.domain.MapResult;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.experiment.ExperimentVersionHolder;
import com.jd.rec.nl.service.common.quartet.LazyInitializer;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/7/11
 */
public class ReduceProcessor implements LazyInitializer {

    private static final Logger LOGGER = getLogger(ReduceProcessor.class);

    //    private ExecutorService executorService;

    private ExperimentVersionHolder<Serializable, Reducer> versionHolder = new ExperimentVersionHolder();

    private Map<String, Reducer> tempStoreReducers = new HashMap<>();

    public void setReducers(Collection<Reducer> reducers) {
        reducers.forEach(reducer -> versionHolder.register(reducer));
    }

    @Override
    public void init() {
        versionHolder.build();
        //        int reducerNum = 0;
        //        for (Reducer reducer : versionHolder.getAllBranches()) {
        //            reducerNum++;
        //        }
        //
        //        executorService = Executors.newFixedThreadPool(1);
    }

    /**
     * 收集分片数据
     *
     * @param mapResult 分片数据
     */
    public void gatherSharding(MapResult mapResult) {
        Reducer executorReducer;
        if (tempStoreReducers.containsKey(mapResult.getExecutorName())) {
            executorReducer = tempStoreReducers.get(mapResult.getExecutorName());
        } else {
            executorReducer = versionHolder.getAllBranches().stream()
                    .filter(reducer -> mapResult.getExecutorName().equals(reducer.getName())).findFirst().get();
            tempStoreReducers.put(executorReducer.getName(), executorReducer);
        }
        executorReducer.collect(mapResult.getKey(), mapResult.getValue());
    }

    /**
     * 进行汇总统计
     *
     * @return
     */
    public ResultCollection reduce() {
        ResultCollection resultCollection = new ResultCollection();
        if (tempStoreReducers.isEmpty()) {
            resultCollection.finish();
            return resultCollection;
        }
        try {
            this.tempStoreReducers.values().stream().forEach((reducer) -> reducer.reduce(resultCollection));
            resultCollection.finish();
        } finally {
            this.tempStoreReducers.clear();
        }
        return resultCollection;
    }

}
