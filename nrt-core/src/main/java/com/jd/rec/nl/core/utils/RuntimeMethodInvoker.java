package com.jd.rec.nl.core.utils;

import com.jd.rec.nl.core.exception.WrongConfigException;
import javassist.*;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 提供运行时的反射调用,通过javassist实现,效率远好于jdk reflection,与原生调用差不多
 * 此类的实例化消耗时间,因此实例化后需要缓存使用
 *
 * @author linmx
 * @date 2018/7/18
 */
public abstract class RuntimeMethodInvoker {

    private static final Logger LOGGER = getLogger(RuntimeMethodInvoker.class);

    private static volatile Map<String, RuntimeMethodInvoker> cache = new HashMap<>();

    private static String createClassName(String className, String methodName, Class... paramTypeList) {
        StringBuilder name = new StringBuilder("com.jd.rec.nl.proxy.");
        name.append(className.substring(className.lastIndexOf(".") + 1)).append(".");
        name.append(methodName);
        name.append(Arrays.stream(paramTypeList).map(aClass ->
                aClass.isArray() ? ("Array".concat(aClass.getComponentType().getSimpleName())) : aClass.getSimpleName())
                .reduce((s, s2) -> s.concat("X").concat(s2)).get());
        return name.toString().replaceAll("\\$", "X");
    }

    /**
     * 获取方法调用的invoker
     *
     * @param className     目标类名
     * @param methodName    方法名
     * @param paramTypeList 参数类型列表
     * @return
     * @throws Exception
     */
    public static RuntimeMethodInvoker createInvoker(String className, String methodName, Class... paramTypeList) throws
            Exception {
        return createInvoker(Class.forName(className), methodName, paramTypeList);
    }

    /**
     * 获取方法调用的invoker
     *
     * @param target        目标类
     * @param methodName    方法名
     * @param paramTypeList 方法参数类型列表
     * @return
     * @throws Exception
     */
    public static RuntimeMethodInvoker createInvoker(Class target, String methodName, Class... paramTypeList) throws
            Exception {
        String className = target.getName();
        String newName = createClassName(className, methodName, paramTypeList);
        if (cache.containsKey(newName)) {
            return cache.get(newName);
        }
        synchronized (cache) {
            if (cache.containsKey(newName)) {
                return cache.get(newName);
            }
            try {
                Class<RuntimeMethodInvoker> invokerClass = (Class<RuntimeMethodInvoker>) Thread.currentThread()
                        .getContextClassLoader().getParent().loadClass(newName);
                RuntimeMethodInvoker invoker = invokerClass.newInstance();
                cache.put(newName, invoker);
                return invoker;
            } catch (ClassNotFoundException e) {
            }
            LOGGER.info("create method invoker:{}.{}:{}", target.getName(), methodName, paramTypeList.toString());
            Method rawMethod = target.getMethod(methodName, paramTypeList);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(rawMethod.getModifiers());
            ClassPool pool = ClassPool.getDefault();
            CtClass proxy = pool.makeClass(newName);
            CtClass supperClass = pool.getCtClass(RuntimeMethodInvoker.class.getName());
            proxy.setSuperclass(supperClass);

            CtMethod superMethod = proxy.getMethod("invoke", supperClass.getDeclaredMethod("invoke").getSignature());
            CtMethod method = CtNewMethod.copy(superMethod, proxy, null);
            StringBuilder body = new StringBuilder("{return ");
            if (rawMethod.getReturnType().isPrimitive()) {
                body.append("new ").append(getBoxedObjectName(rawMethod.getReturnType())).append("(");
            }
            if (isStatic)
                body.append(className).append(".").append(methodName).append("(");
            else
                body.append("((").append(className).append(")$1).").append(methodName).append("(");

            if (paramTypeList.length == 1 && paramTypeList[0].isArray()) {
                body.append("(").append(paramTypeList[0].getTypeName()).append(")$2");
                if (paramTypeList[0].getComponentType().isPrimitive()) {
                    // 由于invokeStatic方法使用了数组语法糖,参数数组如果是原生类型则会包装在object[]中,如果不是原生类型,则会打散到object[]中
                    // 因此对于原生类型,需要增加取第一个元素object[0]
                    body.append("[0]");
                }
            } else {
                int i = 0;
                for (Class paramType : paramTypeList) {
                    body.append("(").append(paramType.getTypeName()).append(")$2[").append(i).append("]");
                    i++;
                    if (i < paramTypeList.length) {
                        body.append(",");
                    }
                }
            }
            body.append(")");
            if (rawMethod.getReturnType().isPrimitive()) {
                body.append(")");
            }
            body.append(";}");
            method.setBody(body.toString());
            method.setModifiers(superMethod.getModifiers() & ~Modifier.ABSTRACT);
            proxy.addMethod(method);

            proxy.setModifiers(proxy.getModifiers() & ~Modifier.ABSTRACT);


            if (cache.containsKey(newName)) {
                return cache.get(newName);
            } else {
                RuntimeMethodInvoker invoker = (RuntimeMethodInvoker) proxy.toClass().newInstance();
                cache.put(newName, invoker);
                return invoker;
            }
        }
    }

    public static String getBoxedObjectName(Class primitiveClass) {
        if (primitiveClass == int.class) {
            return Integer.class.getTypeName();
        } else if (primitiveClass == long.class) {
            return Long.class.getTypeName();
        } else if (primitiveClass == short.class) {
            return Long.class.getTypeName();
        } else if (primitiveClass == byte.class) {
            return Byte.class.getTypeName();
        } else if (primitiveClass == double.class) {
            return Double.class.getTypeName();
        } else if (primitiveClass == float.class) {
            return Float.class.getTypeName();
        } else if (primitiveClass == boolean.class) {
            return Boolean.class.getTypeName();
        } else {
            throw new WrongConfigException("not a primitive class");
        }
    }

    /**
     * 调用普通方法
     *
     * @param instance
     * @param args
     * @return
     */
    public abstract Object invoke(Object instance, Object... args);
}
