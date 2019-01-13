package com.jd.rec.nl.service.common.quartet.domain;

import org.junit.Test;

/**
 * @author linmx
 * @date 2018/9/26
 */
public class MapperFieldReflectorTest {

    @Test
    public void test() throws Exception {
        try {
            String fieldReg = "item:cid3";
            MapperFieldReflector fieldReflector = MapperFieldReflector.create(fieldReg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}