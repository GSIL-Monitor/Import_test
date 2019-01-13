package com.jd.rec.nl.core.guice;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author linmx
 * @date 2018/5/28
 */
@TestLog
public class TestInject {

    @Inject(optional = true)
    @Named("test")
    private int test;

    public void test() {
        System.out.println(test);
    }
}
