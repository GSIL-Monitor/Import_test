package com.jd.rec.nl.connector.standalone.diff;

import com.google.inject.Singleton;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @ClassName JimdbSaveInterceptor
 * @Description TODO
 * @Author rocky
 * @Date 2018/12/1 下午3:35
 * @Version 1.0
 */
@Singleton
public class JimdbSaveInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = getLogger("entrance");
    private static Map<String, byte[]> localCache = new ConcurrentHashMap<>();
    private static AtomicInteger allNum = new AtomicInteger(0);
    private static AtomicInteger successNum = new AtomicInteger(0);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            synchronized (localCache) {
                Object[] arguments = invocation.getArguments();
                String key = new String((byte[]) arguments[1]);
                String field = new String((byte[]) arguments[2]);
                byte[] value = (byte[]) arguments[3];
                arguments[4] = Duration.ofHours(1);
                String conKey = key.concat("~~~").concat(field);
                if (localCache.containsKey(conKey)) {
                    byte[] cacheValue = localCache.get(conKey);
                    if (Arrays.equals(value, cacheValue)) {
                        successNum.addAndGet(1);
                        invocation.proceed();
                    } else {
                        LOGGER.debug("diff error :{}", conKey);
                    }
                    localCache.remove(conKey);
                } else {
                    allNum.addAndGet(1);
                    localCache.put(conKey, value);
                }
            }
        } catch (Exception e) {
            LOGGER.error("JimdbSaveInterceptor Error： ", e);
        }
        return null;
    }

    public static String diffRes() {
        DecimalFormat df = new DecimalFormat("##.00%");
        double v = successNum.get() * 1.0 / allNum.get() * 1.0;
        return df.format(v);
    }
}
