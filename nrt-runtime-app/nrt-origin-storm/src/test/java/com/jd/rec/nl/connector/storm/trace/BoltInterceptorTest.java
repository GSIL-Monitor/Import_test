package com.jd.rec.nl.connector.storm.trace;

import backtype.storm.task.IBolt;
import backtype.storm.tuple.Tuple;
import com.jd.rec.nl.connector.storm.bolt.AppBolt;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author linmx
 * @date 2018/7/6
 */
public class BoltInterceptorTest {

    @Test
    public void test() throws NoSuchMethodException {
        Method method = AppBolt.class.getMethod("execute", Tuple.class);
        Method interfaceMethod = IBolt.class.getMethod("execute", Tuple.class);
        System.out.println(method == interfaceMethod);
    }
}