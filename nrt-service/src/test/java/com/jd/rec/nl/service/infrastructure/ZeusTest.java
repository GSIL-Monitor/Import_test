package com.jd.rec.nl.service.infrastructure;

import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Test;

import java.util.Random;

/**
 * @author linmx
 * @date 2018/7/23
 */
public class ZeusTest {

    @Test
    public void test() {
        Random random = new Random();
        CLUSTER rc = CLUSTER.values()[random.nextInt(3)];
        while (!rc.name().equals("HT")){
            rc = CLUSTER.values()[random.nextInt(3)];
        }
        System.out.println(rc.name());
        System.out.println(rc == CLUSTER.HT);
    }

    @Test
    public void test1() {
        System.out.println( DateFormatUtils.formatUTC(System.currentTimeMillis(), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }
}