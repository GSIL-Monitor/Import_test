package com.jd.rec.nl.origin.standalone;

import com.jd.rec.nl.connector.standalone.StandaloneRuntime;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Test;

import java.util.Date;

/**
 * @author linmx
 * @date 2018/6/20
 */
public class StandaloneRuntimeTest {

    @Test
    public void run() throws Exception {
        StandaloneRuntime runtime = new StandaloneRuntime();
        runtime.run();
    }

    @Test
    public void time() {
        System.out.println(System.currentTimeMillis());
    }

    @Test
    public void testTime() {
        System.out.println(DateFormatUtils.format(new Date(1541348453000L), "yyyy-MM-dd HH:mm:ss"));
    }
}