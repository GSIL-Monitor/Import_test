package com.jd.rec.nl.core.framework;

import java.util.Collections;
import java.util.List;

/**
 * Created by linmx on 2018/5/14.
 */
public class ResultMergeTest {

    @org.junit.Test
    public void testCast() {
        Test a = new Test("1", 1);
        Test b = new Test();
        b.setC(Collections.singletonList("c"));
        Test c = a.getClass().cast(b);
        System.out.println(b);
        System.out.println(c);
    }

    class Test {
        private String a;
        private int b;
        private List<String> c;

        public Test() {
        }

        public Test(String a, int b) {
            this.a = a;
            this.b = b;
        }

        public List<String> getC() {
            return c;
        }

        public void setC(List<String> c) {
            this.c = c;
        }
    }
}