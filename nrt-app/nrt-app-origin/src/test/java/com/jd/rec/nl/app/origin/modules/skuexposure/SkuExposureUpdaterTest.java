package com.jd.rec.nl.app.origin.modules.skuexposure;

import org.junit.Test;
import p13.nearline.NlEntranceSkuExposure;

/**
 * @author linmx
 * @date 2018/8/9
 */
public class SkuExposureUpdaterTest {

    @Test
    public void test() {
        NlEntranceSkuExposure.SkuExposureList.Builder list = NlEntranceSkuExposure.SkuExposureList.newBuilder();
        byte[] ret = list.build().toByteArray();
        System.out.println(ret.length);
    }
}