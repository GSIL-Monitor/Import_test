package com.jd.rec.nl.connector.standalone.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @ClassName ThreadPoolRunner
 * @Description TODO
 * @Author rocky
 * @Date 2018/11/23 下午7:49
 * @Version 1.0
 */
public class ThreadPoolRunner<T> {
    private static Logger logger = LoggerFactory.getLogger(ThreadPoolRunner.class);


    /**
     * 线程池的执行函数
     *
     * @param thread_num        线程池大小
     * @param dataGetterSet     用于执行取数操作的对象集合
     * @param maxWaitTimeMillis 本次并发执行的线程组，最大等待时间，以毫秒为单位
     * @return List对象
     */
    public List<T> run(int thread_num, Set<Callable<T>> dataGetterSet, long maxWaitTimeMillis) {
        ExecutorService pool = Executors.newFixedThreadPool(thread_num);
        return run(pool, dataGetterSet, maxWaitTimeMillis);
    }

    public List<T> run(ExecutorService pool, Set<Callable<T>> dataGetterSet, long maxWaitTimeMillis) {
        if (null == pool || null == dataGetterSet || dataGetterSet.isEmpty()) {
            return null;
        }
        List<T> result = new ArrayList<>();

        CompletionService<T> cmp = new ExecutorCompletionService<>(pool);
        for (Callable handler : dataGetterSet) {
            cmp.submit(handler);
        }
        int size = dataGetterSet.size();
        int count = 0;
        long st = System.currentTimeMillis();
        while (count < size) {
            try {
                Future<T> future = cmp.poll();
                if (null == future) {
                    long time = System.currentTimeMillis() - st;
                    if (time < maxWaitTimeMillis) {
                        TimeUnit.MILLISECONDS.sleep(1L);
                    } else {
                        break;
                    }
                } else {
                    count++;
                    if (count % 1000 == 0)
                        logger.info("{} callable returned", count);
                    T tmp = future.get();
                    if (null != tmp) {
                        result.add(tmp);
                    }
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            } catch (ExecutionException e) {
                logger.error(e.getMessage(), e);
            }

        }
        try {
            pool.shutdown();
            if (!pool.awaitTermination(maxWaitTimeMillis, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            // awaitTermination方法被中断的时候也中止线程池中全部的线程的执行。
            logger.error("awaitTermination interrupted: ", e);
            pool.shutdownNow();
        }
        return result;
    }
}
