package com.jd.rec.nl.connector.storm.trace;

import com.jd.rec.nl.core.debug.DebugInfoThreadLocal;
import com.jd.rec.nl.core.domain.UserEvent;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/7/6
 */
public class ProcessorInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = getLogger(ProcessorInterceptor.class);

    private boolean check = false;

    private int userInfoIndex = -1;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (!check) {
            checkMethod(invocation);
        }
        if (this.userInfoIndex != -1) {
            String uid = ((UserEvent) invocation.getArguments()[userInfoIndex]).getUid();
            DebugInfoThreadLocal.init(uid);
        }
        try {
            return invocation.proceed();
        } finally {
            DebugInfoThreadLocal.release();
        }
    }

    private void checkMethod(MethodInvocation invocation) {
        for (int i = 0; i < invocation.getArguments().length; i++) {
            Object arg = invocation.getArguments()[i];
            if (arg instanceof UserEvent) {
                userInfoIndex = i;
                break;
            }
        }
        check = true;
    }
}
