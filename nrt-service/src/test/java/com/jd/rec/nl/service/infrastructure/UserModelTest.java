package com.jd.rec.nl.service.infrastructure;

import com.jd.si.util.ThriftSerialization;
import com.jd.si.venus.core.CommenInfo;
import org.junit.Test;

/**
 * @author linmx
 * @date 2018/10/8
 */
public class UserModelTest {

    @Test
    public void testSerial() {
        byte[] value = new byte[]{};
        CommenInfo commenInfo = ThriftSerialization.fromCompactBytes(CommenInfo.class, value);
        System.out.println(commenInfo);
    }

    @Test
    public void test() throws Exception {
        int ret = testFinally();
        System.out.println(ret);
    }

    public int testFinally() throws Exception {
        try {
            System.out.println("run");
            throw new Exception("test");
//            return 1;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            System.out.println("finally");
        }
    }
}