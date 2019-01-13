package com.jd.rec.nl.core.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * @author linmx
 * @date 2018/5/28
 */
public class ConfigBaseTest {

    @Test
    public void testLoadConfig() {
        File file = new File(Thread.currentThread().getContextClassLoader().getResource("modules/").getPath());
        Config test = ConfigFactory.load();
        for (File child : file.listFiles()) {
            test = test.withFallback(ConfigFactory.parseFile(child));
        }
        System.out.println(test.hasPath("test"));
    }

    @Test
    public void testEqual() {
        Config config = ConfigFactory.load("test");
//        Config test1 = config.getConfig("test1");
//        Config test2 = config.getConfig("test2");
        Config test3 = config.getConfig("test3");
        List<? extends Config> skuExposure = config.getConfigList("skuExposure.619182");
        System.out.println(test3.equals(skuExposure.get(0).getConfig("params")));
//        System.out.println(test1.equals(test2));
//        System.out.println(test1.equals(test3));
//        Config v1 = test3.getConfig("v1");
//        v1 = v1.withFallback(test1);
//        System.out.println(v1.toString());
    }
}