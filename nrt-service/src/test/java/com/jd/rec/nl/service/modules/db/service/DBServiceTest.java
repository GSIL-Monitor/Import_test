package com.jd.rec.nl.service.modules.db.service;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.service.infrastructure.Jimdb;
import com.jd.rec.nl.service.infrastructure.MockJimdb;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author linmx
 * @date 2018/6/15
 */
public class DBServiceTest {

    @Before
    public void prepare() {
        InjectorService.registerModules(binder -> binder.bind(Jimdb.class).to(MockJimdb.class));
        Map config = new HashMap();
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.DBProxy", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Clerk", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Predictor", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Jimdb", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.UnifiedOutput", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Zeus", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Behavior", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.UserModel", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.ILogKafka", true);

        config.put("dbservice.checkpoint_interval", "20s");
        config.put("dbservice.checkpoint_size", 4000);

        //        config.put("themeBurst.params.featureList", Arrays.asList("1minutes","3minutes","6minutes"));
        //        config.put("activityBurst.params.windowSize", "20seconds");
        com.typesafe.config.Config thread = ConfigFactory.parseMap(config);
        ConfigBase.setThreadConfig(thread);
    }

    @Test
    public void testDer() {
        byte[] result = {20};
        Kryo kryo = new Kryo();
        Input input = new Input(result);
        int i = kryo.readObject(input, Integer.class);
        System.out.println(i);
    }

    @Test
    public void test() throws Exception {

        int limit = 10000;
        DBService dbService = InjectorService.getCommonInjector().getInstance(DBService.class);
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        AtomicInteger index = new AtomicInteger(0);
        int writeInterval = 20;
        Random random = new Random();
        Runnable writer = () -> {
            while (index.intValue() < limit) {
                try {
                    String key = "a".concat("-").concat(String.valueOf(index.getAndIncrement()));
                    String value = key.concat("-value");
                    dbService.save("test", key, value);
                    Thread.sleep(random.nextInt(writeInterval));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        AtomicInteger readIndex = new AtomicInteger(0);
        int readInterval = 20;
        Runnable reader = () -> {
            while (readIndex.intValue() < limit) {
                try {
                    if (readIndex.get() < index.get() - 2 || index.get() == limit) {
                        String key = "a".concat("-").concat(String.valueOf(readIndex.getAndIncrement()));
                        String value = dbService.query("test", key);
                        if (value == null) {
                            System.out.println(key + " is lost, now writer is " + index.get());
                        }
                    } else {
                        Thread.sleep(random.nextInt(readInterval));
                    }
                    //                    System.out.println(dbService.query("test", key));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        AtomicInteger rwIndex = new AtomicInteger(0);
        int rwInterval = 20;
        Runnable rwriter = () -> {
            while (rwIndex.intValue() < limit) {
                try {
                    if (rwIndex.get() < index.get() - 2 || index.get() == limit) {
                        String key = "a".concat("-").concat(String.valueOf(rwIndex.getAndIncrement()));
                        String value = dbService.query("test", key);
                        if (value != null) {
                            value = value.concat("-").concat("read");
                            dbService.save("test", key, value);
                        } else {
                            System.out.println(key + " is lost, now writer is " + index.get());
                        }
                    } else {
                        Thread.sleep(random.nextInt(rwInterval));
                    }
                    //                    System.out.println(dbService.query("test", key));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        executorService.submit(writer);
        executorService.submit(writer);
        Thread.sleep(10000);

        executorService.submit(reader);
        while (index.get() != limit) {
            Thread.sleep(100);
        }

        executorService.submit(rwriter);
        Thread.sleep(1000);
        while (rwIndex.get() != limit) {
            Thread.sleep(100);
        }

        int a = 0;
        while (a < limit) {
            String key = "a".concat("-").concat(String.valueOf(a++));
            String value = dbService.query("test", key);
            if (value == null) {
                System.out.println(key + " is lost");
            } else {
                if (!value.endsWith("read")) {
                    System.out.println(key + " is not read");
                }
            }
        }
    }


    @Test
    public void testBatchQuery() throws Exception {
        InjectorService.registerModules(binder -> binder.bind(Jimdb.class).to(MockJimdb.class));
        DBService dbService = InjectorService.getCommonInjector().getInstance(DBService.class);

        dbService.save("test", "a", "aaa", Duration.ofMinutes(1));
        dbService.save("test", "b", "bbb", Duration.ofMinutes(1));
        dbService.save("test", "c", "ccc", Duration.ofMinutes(1));
        Map<String, String> results = dbService.batchQuery("test", Arrays.asList("a", "b", "c"));
        System.out.println(results);
    }

    @Test
    public void testBatchSet() throws Exception {
        InjectorService.registerModules(binder -> binder.bind(Jimdb.class).to(MockJimdb.class));
        DBService dbService = InjectorService.getCommonInjector().getInstance(DBService.class);

        Map<String, String> values = new HashMap<>();
        values.put("a", "aaa");
        values.put("b", "bbb");
        values.put("c", "ccc");

        dbService.batchSave("test", values, Duration.ofMinutes(1));
        dbService.save("test", "d", "ddd", Duration.ofMinutes(1));

        Map<String, String> results = dbService.batchQuery("test", Arrays.asList("a", "b", "c"));
        System.out.println(results);
        System.out.println(dbService.query("test", "a").toString());
        System.out.println(dbService.query("test", "b").toString());
        System.out.println(dbService.query("test", "c").toString());
        System.out.println(dbService.query("test", "d").toString());
    }

    @Test
    public void testKey() {
        String key = DBService.generateKey("a", "b");
        System.out.println(key);
    }

    @Test
    public void testSet() throws InterruptedException {
        Set<String> set = new HashSet<>();
        set.add("a");
        set.add("b");
        set.add("c");
        Iterator<String> it = set.iterator();
        int count = 0;
       while (it.hasNext()){
           String x = it.next();
           count++;
           System.out.println(x);
          // it.remove();
           if(count>2){
               Thread.sleep(10);
               count=0;
           }
       }
        System.out.println(set.size());
    }

}