package com.jd.rec.nl.core.framework.impl;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * Created by linmx on 2018/5/15.
 */
public class SerialProcessorTest {

    @Test
    public void testForkJoinPool() throws InterruptedException, ExecutionException {
        ForkJoinPool forkJoinPool = new ForkJoinPool(8);
        String[] a = {"11", "22", "33", "44", "55", "66", "77", "88", "99", "100", "111", "122", "133", "144"};
        String[] b = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n"};
        List<List<String>> test = new ArrayList();
        test.add(Arrays.asList(a));
        test.add(Arrays.asList(b));
        ForkJoinTask task = forkJoinPool.submit(() ->
                test.parallelStream().forEach((item) -> {
                            System.out.println("outer:" + Thread.currentThread().getName());
                            item.parallelStream().forEach(s ->
                                    System.out.println("inner:" + Thread.currentThread().getName() + "," + s)
                            );
                        }
                )
        );
//        test.parallelStream().forEach((item) -> {
//            item.parallelStream().forEach(s -> {
//                System.out.println(Thread.currentThread().getName() + "   s" + s);
//            });
//        });
//        Thread.sleep(1000L);
        task.get();
    }

}