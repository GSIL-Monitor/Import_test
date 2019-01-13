package com.jd.rec.nl.service.base.quartet.impl;

import com.jd.rec.nl.core.guice.config.Configuration;
import com.jd.rec.nl.service.base.quartet.domain.MapResult;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author linmx
 * @date 2018/6/11
 */
@Configuration
public class TestConfiguration {

    @Test
    public void test() throws InterruptedException, ExecutionException {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        List<String> ret = new ArrayList<>();
        ret.add("1");
        ret.add("2");
        ret.add("3");
        ret.add("4");
        ResultCollection resultCollection = new ResultCollection();
        ForkJoinTask task = forkJoinPool.submit(() -> {
            ret.parallelStream().forEach(s -> {
                try {
                    Thread.sleep(Integer.valueOf(s) * 100);
                    resultCollection.addMapResult(Thread.currentThread().getName(), s, "result:".concat(s));
                    System.out.println(s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            try {
                task.get();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                resultCollection.finish();
            }
        });

        try {
            while (true) {
                while (resultCollection.size() == 0) {
                    if (resultCollection.isFinish()) {
                        return;
                    }
                }
                MapResult result = resultCollection.getResults().pollFirst();
                System.out.println(String.format("runner:%s, key:%s, value:%s", result.getExecutorName(), result.getKey(),
                        result
                                .getValue()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
