package com.jd.rec.nl.service.common.quartet.filter;

import java.lang.reflect.Method;

/**
 * @author linmx
 * @date 2018/10/10
 */
public interface MethodFilter {

    /**
     * 需要过滤的方法
     *
     * @return
     */
    Method filterMethod();

    /**
     * 过滤类型
     *
     * @return
     */
    FilterType filterType();

    /**
     * 错误信息
     *
     * @return
     */
    String invalidMessage();

    /**
     * 前置过滤判断
     *
     * @param args 方法参数
     * @return 是否通过: true-通过(默认) false-不通过
     */
    default boolean preFilter(Object... args) {
        return true;
    }

    /**
     * 后置过滤判断
     *
     * @param ret  方法返回
     * @param args 方法参数
     * @return 是否通过: true-通过(默认) false-不通过
     */
    default boolean postFilter(Object ret, Object... args) {
        return true;
    }

    /**
     * 过滤类型:
     * pre:前置过滤--方法调用前的过滤
     * post:后置过滤--方法调用后的过滤
     * round:环绕过滤--
     */
    enum FilterType {
        pre, post, round
    }
}
