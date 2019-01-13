package com.jd.rec.nl.app.origin.modules.themeburst;

import org.junit.Test;

import java.time.Duration;

/**
 * @author linmx
 * @date 2018/10/10
 */
public class ReduceRecallTest {

    @Test
    public void testDuration() {
        Duration windowSize = Duration.ofMinutes(5);
        windowSize = windowSize.plus(windowSize);
        System.out.println(windowSize);
    }

}