package com.jd.rec.nl.service.common.trace;

import com.jd.rec.nl.service.common.monitor.domain.InvokeInformation;
import org.junit.Test;

/**
 * @author linmx
 * @date 2018/9/29
 */
public class RuntimeTraceTest {

    @Test
    public void testLog() {
        RuntimeTrace trace = new RuntimeTrace();
        InvokeInformation information = new InvokeInformation("testExt");
        information.end();
        trace.info("test", information);
    }

}