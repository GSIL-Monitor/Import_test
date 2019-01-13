package com.jd.rec.nl.connector.standalone.diff;

import com.google.inject.Singleton;
import com.jd.jim.cli.Cluster;
import com.jd.jim.cli.ReloadableJimClientFactory;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.infrastructure.domain.CLUSTER;
import com.jd.rec.nl.service.infrastructure.Jimdb;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.util.Arrays;
import java.util.Objects;

/**
 * @ClassName JimdbQueryInterceptor
 * @Description TODO
 * @Author rocky
 * @Date 2018/11/30 下午3:58
 * @Version 1.0
 */
@Singleton
public class JimdbQueryInterceptor implements MethodInterceptor {

    private static Cluster cluster;


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object res = invocation.proceed();
        if (Objects.nonNull(res)) {
            return res;
        } else {
            if (Objects.isNull(cluster)) {
                ReloadableJimClientFactory factory = new ReloadableJimClientFactory();
                factory.setJimUrl("jim://2643801683164162311/3759");
                cluster = factory.getClient();
            }
            try {
                Object[] arguments = invocation.getArguments();
                byte[] key = (byte[]) arguments[1];
                byte[] field = (byte[]) arguments[2];
                byte[] value = cluster.hGet(key, field);
                return value;
            } catch (Exception e) {
                throw new EnvironmentException("jimdb", e);
            }
        }
    }
}