package com.jd.rec.nl.app.origin.modules.entrance.Service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.service.infrastructure.Jimdb;
import com.jd.rec.nl.test.mock.infrastructure.MockJimdb;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;



public class EntranceDBSeviceTest {

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
        com.typesafe.config.Config thread = ConfigFactory.parseMap(config);
        ConfigBase.setThreadConfig(thread);
    }

    @Test
    public void test() throws InterruptedException, InvalidProtocolBufferException {
        int limit = 1000;
        EntranceDBSevice entranceDBSevice = InjectorService.getCommonInjector().getInstance(EntranceDBSevice.class);
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        AtomicInteger index = new AtomicInteger(0);
        int writeInterval = 5;
        Random random = new Random();
        Runnable writer = () -> {
            while (index.intValue() < limit) {
                try {
                    String key = "a".concat("-").concat(String.valueOf(index.getAndIncrement()));
                    String field = key.concat("-value");
                    Map<Long, Set<Long>> value = new HashMap<>();
                    Set<Long> set = new HashSet<>();
                    set.add(Long.valueOf(1)+index.getAndIncrement());
                    value.put(Long.valueOf(1000)+index.getAndIncrement(),set);
                    entranceDBSevice.setPoolToContentIds(key,field,value);
                    Thread.sleep(random.nextInt(writeInterval));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        AtomicInteger readIndex = new AtomicInteger(0);
        int readInterval = 5;
        Runnable reader = () -> {
            while (readIndex.intValue() < limit) {
                try {
                    if (readIndex.get() < index.get() - 2 || index.get() == limit) {
                        String key = "a".concat("-").concat(String.valueOf(readIndex.getAndIncrement()));
                        String field = key.concat("-value");
                        Map<Long,Set<Long>> value = entranceDBSevice.getPoolToContentIds(key,field);
                        if (value == null) {
                            System.out.println(key + " is lost, now writer is " + index.get());
                        }
                    } else {
                        Thread.sleep(random.nextInt(readInterval));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        AtomicInteger rwIndex = new AtomicInteger(0);
        int rwInterval = 5;
        Runnable rwriter = () -> {
            while (rwIndex.intValue() < limit) {
                try {
                    if (rwIndex.get() < index.get() - 2 || index.get() == limit) {
                        String key = "a".concat("-").concat(String.valueOf(rwIndex.getAndIncrement()));
                        String field = key.concat("-value");
                        Map<Long,Set<Long>> value = entranceDBSevice.getPoolToContentIds(key,field);
                        if (value != null) {
                            Set<Long> set = new HashSet<>();
                            field = field+"-read";
                            set.add(Long.valueOf(1)+index.getAndIncrement());
                            value.put(Long.valueOf(1000)+index.getAndIncrement(),set);
                            entranceDBSevice.setPoolToContentIds(key,field,value);
                        } else {
                            System.out.println(key + " is lost, now writer is " + index.get());
                        }
                    } else {
                        Thread.sleep(random.nextInt(rwInterval));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        executorService.submit(writer);
        executorService.submit(writer);
        Thread.sleep(100);

        executorService.submit(reader);
        while (index.get() != limit) {
            Thread.sleep(10);
        }

        executorService.submit(rwriter);
        Thread.sleep(10);
        while (rwIndex.get() != limit) {
            Thread.sleep(10);
        }

        int a = 0;
        while (a < limit) {
            String key = "a".concat("-").concat(String.valueOf(a++));
            String field = key.concat("-value");
            Map<Long,Set<Long>> value = entranceDBSevice.getPoolToContentIds(key,field);
            if (value == null) {
                System.out.println(key + " is lost");
            } else {
                field = field+"-read";
                value = entranceDBSevice.getPoolToContentIds(key,field);
                System.out.println("filed+read:"+value);
            }
        }
    }

    @Test
    public void testHget() throws InvalidProtocolBufferException {
        InjectorService.registerModules(binder -> binder.bind(Jimdb.class).to(MockJimdb.class));
        EntranceDBSevice entranceDBSevice = InjectorService.getCommonInjector().getInstance(EntranceDBSevice.class);
        String key1= "uid1";
        String field1 = "uid1-value";
        Map<Long, Set<Long>> value1= new HashMap<>();
        Set<Long> set = new HashSet<>();
        set.add(1l);
        set.add(2l);
        value1.put(100l,set);
        entranceDBSevice.setPoolToContentIds(key1,field1,value1);
        String key2= "uid2";
        String field2 = "uid2-value";
        Map<Long, Set<Long>> value2= new HashMap<>();
        Set<Long> set2 = new HashSet<>();
        set2.add(3l);
        set2.add(4l);
        value2.put(200l,set2);
        entranceDBSevice.setPoolToContentIds(key2,field2,value2);
        Map<Long,Set<Long>> map = entranceDBSevice.getPoolToContentIds(key1,field1);
        System.out.println(map);
        Map<Long,Set<Long>> map2 = entranceDBSevice.getPoolToContentIds(key2,field2);
        System.out.println(map2);
    }

    @Test
    public void test2() throws InterruptedException {
        InjectorService.registerModules(binder -> binder.bind(Jimdb.class).to(MockJimdb.class));
        EntranceDBSevice entranceDBSevice = InjectorService.getCommonInjector().getInstance(EntranceDBSevice.class);
        for(int i=1;i<600;i++){
            String key = "uid"+i;
            String field = key.concat("-value");
            Map<Long, Set<Long>> value= new HashMap<>();
            Set<Long> set = new HashSet<>();
            set.add(Long.valueOf(100)+i);
            value.put(Long.valueOf(1),set);
            entranceDBSevice.setPoolToContentIds(key,field,value);
            //Thread.sleep(10000);
        }
    }
}