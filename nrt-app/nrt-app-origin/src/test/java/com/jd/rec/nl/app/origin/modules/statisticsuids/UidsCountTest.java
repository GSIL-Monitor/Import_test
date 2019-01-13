package com.jd.rec.nl.app.origin.modules.statisticsuids;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Test;

import java.util.Date;

public class UidsCountTest {
    @Test
    public void test(){
        String BeforeTime = DateFormatUtils.format(new Date(1539337037485L), "yyyyMMdd");
        int AfterTime = Integer.valueOf(BeforeTime);
        System.out.println(AfterTime);
    }

}