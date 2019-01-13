package com.jd.rec.nl.core.utils;

import javassist.CtClass;

import java.util.Arrays;

/**
 * @author linmx
 * @date 2018/7/18
 */
public class RuntimeProxyMethodInvokerTest {

    static {
        CtClass.debugDump = "./src/test/java";
    }

    @org.junit.Test
    public void test() throws Exception {
        RuntimeMethodInvoker invoker = RuntimeMethodInvoker.createInvoker(Test.class, "get",
                String.class, String.class);
        String ret = (String) invoker.invoke(null, "1", "2");
        System.out.println(ret);

        long start = System.currentTimeMillis();
        int i = 0;
        while (i < 10000000) {
            //            invoker.invokeStatic("1", "2");
            Test.get("1", "2");
            i++;
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);

        start = System.currentTimeMillis();
        i = 0;
        while (i < 10000000) {
            //            Test.get("1", "2");
            invoker.invoke(null, "1", "2");
            i++;
        }
        end = System.currentTimeMillis();
        System.out.println(end - start);
    }

    @org.junit.Test
    public void testRuntime() throws Exception {

        RuntimeMethodInvoker invoker = RuntimeMethodInvoker.createInvoker(Test.class, "runGet", String
                .class, String.class);
        Test test = new Test("inner");
        System.out.println(invoker.invoke(test, "1", "2"));

        long start = System.currentTimeMillis();
        int i = 0;
        while (i < 10000000) {
            //            invoker.invokeStatic("1", "2");
            test.runGet("1", "2");
            i++;
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);

        start = System.currentTimeMillis();
        i = 0;
        while (i < 10000000) {
            //            Test.get("1", "2");
            invoker.invoke(test, "1", "2");
            i++;
        }
        end = System.currentTimeMillis();
        System.out.println(end - start);
    }

    @org.junit.Test
    public void testError() throws Exception {
        RuntimeMethodInvoker invoker = RuntimeMethodInvoker.createInvoker(Test.class, "runGet", String
                .class, String.class);
        Test test = new Test("inner");
        System.out.println(invoker.invoke(test, "1", "2", "3"));
    }

    @org.junit.Test
    public void testStaticArray() throws Exception {
        RuntimeMethodInvoker invoker = RuntimeMethodInvoker.createInvoker(Test.class, "get",
                String[].class);
        String[] in = new String[]{"a", "b"};
        //        String[] inT = (String[]) (new Object[]{in});
        System.out.println(in);
//        System.out.println(invoker.invoke(null, in));
    }


    @org.junit.Test
    public void testArray() throws Exception {
        RuntimeMethodInvoker invoker = RuntimeMethodInvoker.createInvoker(Test.class, "runGet",
                String[].class);
        String[] in = new String[]{"a", "b"};
        //        String[] inT = (String[]) (new Object[]{in});
        Test test = new Test("inner");
//        System.out.println(invoker.invoke(test, in));
    }

    @org.junit.Test
    public void testByte() throws Exception {
        Object bb = (new Object[]{"aaa".getBytes()})[0];
        byte[] b = (byte[]) bb;

        RuntimeMethodInvoker invoker = RuntimeMethodInvoker.createInvoker(Test.class, "get",
                byte[].class);
        System.out.println(invoker.invoke(null, "aaa".getBytes()));
    }

    @org.junit.Test
    public void testIntArray() throws Exception {
        RuntimeMethodInvoker invoker = RuntimeMethodInvoker.createInvoker(Test.class, "runGet",
                int[].class);
        Test test = new Test("testInt");
        int[] in = {1, 2};
        System.out.println(invoker.invoke(test, in));
    }

    @org.junit.Test
    public void testInt() throws Exception {
        RuntimeMethodInvoker invoker = RuntimeMethodInvoker.createInvoker(Test.class, "runGet", String.class);
        Test test = new Test("testInt");
        System.out.println(invoker.invoke(test, "aa"));
    }

    public void testPrimitive() {
        int i = 10;
        Object a = (Object) i;
    }

    public static class Test {

        private String inner;

        public Test(String inner) {
            this.inner = inner;
        }

        public static String get(String a, String b) {
            return "test".concat(a).concat(b);
        }

        public static String get(String[] in) {
            return Arrays.stream(in).reduce((s, s2) -> s.concat(s2)).get();
        }

        public static String get(byte[] in) {
            return new String(in);
        }

        public String runGet(String a, String b) {
            return inner.concat(a).concat(b);
        }

        public String runGet(int[] in) {
            String inStr = String.valueOf(Arrays.stream(in).reduce((s, s2) -> s + s2).getAsInt());
            return inner.concat(inStr);
        }

        public String runGet(String[] in) {
            return Arrays.stream(in).reduce((s, s2) -> s.concat(s2)).get();
        }

        public int runGet(String a) {
            return a.hashCode();
        }
    }

    class get {
        public get() {
        }

        public Object invokeStatic(Object... var1) {
            //            return Test.get((String[]) var1[0]);
            return null;
        }
    }
}