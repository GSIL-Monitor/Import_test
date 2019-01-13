package com.jd.rec.nl.service.common.trace;

import com.google.inject.matcher.Matcher;
import com.jd.rec.nl.core.guice.InjectorService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;

/**
 * @author linmx
 * @date 2018/6/21
 */
public abstract class MethodTraceInterceptor implements MethodInterceptor {

    public static final int deepLimit = 10;

    public static String traceObject(Object object, int deep) throws InvocationTargetException, IllegalAccessException {
        if (object == null) {
            return "null";
        }
        Method toString = null;
        try {
            if (!(object instanceof Collection)) {
                if (!object.getClass().getName().startsWith(InjectorService.initPath)) {
                    toString = object.getClass().getMethod("toString");
                } else {
                    toString = object.getClass().getDeclaredMethod("toString");
                }
            }
        } catch (Exception e) {
        }
        if (toString != null) {
            return toString.invoke(object).toString();
        } else {
            if (object instanceof Collection) {
                StringBuilder sb = new StringBuilder(object.getClass().getName()).append(":[");
                for (Object element : ((Collection) object)) {
                    if (deep <= deepLimit) {
                        sb.append(traceObject(element, ++deep));
                    } else {
                        sb.append(element.toString());
                    }
                }
                sb.append("]");
                return sb.toString();
            } else {
                StringBuilder sb = new StringBuilder(object.getClass().getName()).append(":{");
                for (Field field : object.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(object);
                    if (value != null && !value.getClass().isArray() && !value.getClass().isEnum()
                            && value.getClass().getName().startsWith(InjectorService.initPath) && deep <= deepLimit) {
                        value = traceObject(value, ++deep);
                    }
                    sb.append(field.getName()).append(" : ").append(value).append("   |   ");
                }
                sb.append("}");
                return sb.toString();
            }
        }
    }

    protected abstract void output(StringBuilder sb);

    protected abstract boolean filter(MethodInvocation invocation, Object result, Throwable throwable);

    public abstract Matcher<Class> matchedClass();

    public abstract Matcher<Method> matchedMethod();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object ret = null;
        Throwable throwable = null;
        try {
            ret = invocation.proceed();
        } catch (Throwable e) {
            throwable = e;
        }

        if (filter(invocation, ret, throwable)) {
            StringBuilder sb = new StringBuilder();
            try {
                logMethodInfo(sb, invocation);
                logArguments(sb, invocation);
                logReturn(sb, invocation, ret, throwable);
            } catch (Exception e) {
                sb.append("debug error:\n").append(e.getMessage());
            } finally {
                output(sb);
            }
        }

        if (throwable != null) {
            throw throwable;
        } else {
            return ret;
        }
    }

    protected void logMethodInfo(StringBuilder sb, MethodInvocation invocation) {
        String className = invocation.getThis().getClass().getSimpleName();
        String methodName = invocation.getMethod().getName();
        sb.append("[").append(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS")).append("]").append(className)
                .append("#").append(methodName).append(":\nInput:\n");
    }

    protected void logArguments(StringBuilder sb, MethodInvocation invocation) throws Exception {
        Object[] arguments = invocation.getArguments();
        for (int i = 0; i < arguments.length; i++) {
            sb.append(invocation.getMethod().getParameters()[i].getName()).append("-->").append(traceObject(arguments[i], 0))
                    .append("\n");
        }
    }

    protected void logReturn(StringBuilder sb, MethodInvocation invocation, Object ret, Throwable throwable) {
        if (throwable == null) {
            try {
                if (invocation.getMethod().getReturnType() != Void.TYPE) {
                    sb.append("Output:\n").append(traceObject(ret, 0));
                }
            } catch (Exception e) {
                sb.append("debug error:\n").append(e.getStackTrace());
            }
        } else {
            sb.append("run error:\n").append(throwable.getStackTrace());
        }
    }
}
