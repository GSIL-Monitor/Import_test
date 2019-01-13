package com.jd.rec.nl.core.cache;

import com.jd.rec.nl.core.guice.InjectorService;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/7/2
 */
public class CacheConfigurationTest {

    @Test
    public void testCache() {
        CacheResultTest test = InjectorService.getCommonInjector().getInstance(CacheResultTest.class);
        System.out.println(test.get("test1", 10));
        System.out.println(test.get("test2", 10));
        System.out.println(test.get("test1", 10));
        Map<String, String> ret = test.getAll(Arrays.asList("test1", "test2"), 10);
        System.out.println(ret);
        ret = test.getAll(Arrays.asList("test", "test2"), 10);
        System.out.println(ret);
        ret = test.getAll(Arrays.asList("test3", "test4"), 10);
        System.out.println(ret);
        ret = test.getAll(Arrays.asList("test3", "test4"), 10);
        System.out.println(ret);
        CacheResultTest1 test1 = InjectorService.getCommonInjector().getInstance(CacheResultTest1.class);
        System.out.println(test1.get("test1", 10));
        System.out.println(test1.get("test2", 10));
        //        System.out.println(test1.get("test1", 11));
        //        System.out.println(test1.get("test2", 11));

        System.out.println(test1.get("test1", 10));
        System.out.println(test1.get("test2", 10));
        //        System.out.println(test1.get("test1", 11));
        //        System.out.println(test1.get("test2", 11));

        ret = test1.getAll(Arrays.asList("test", "test1", "test2"), 10);
        System.out.println(ret);
        //        ret = test1.getAll(Arrays.asList("test1", "test2"), 11);
        //        System.out.println(ret);
    }

    @Test
    public void testCachePut() {
        CacheResultTest test = InjectorService.getCommonInjector().getInstance(CacheResultTest.class);
        //        test.put("test", "test-put");
        //        System.out.println(test.get("test", 10));
        Map<String, String> values = new HashMap<>();
        values.put("test1", "test1-putAll");
        values.put("test2", "test2-putAll");
        values.put("test3", null);

        test.putAll(values);
        System.out.println(test.getAll(Arrays.asList("test3", "test1", "test2"), 10));
        System.out.println(test.getAll(Arrays.asList("test", "test1", "test2"), 10));
        System.out.println(test.get("test3", 10));
    }

    @Test
    public void testNull() {
        CacheResultTest test = InjectorService.getCommonInjector().getInstance(CacheResultTest.class);
        System.out.println(test.get("test", 0));
        System.out.println(test.get("test", 0));
        Map<String, String> ret = test.getAll(Arrays.asList("test", "test2"), 10);
        System.out.println(ret);
    }

    @Test
    public void testDynamic() {
        CacheResultTest1 test1 = InjectorService.getCommonInjector().getInstance(CacheResultTest1.class);
        System.out.println(test1.get("test", 1));
        System.out.println(test1.get("test", 1));
        System.out.println(test1.get("test", 2));
        System.out.println(test1.get("test", 2));

    }

}