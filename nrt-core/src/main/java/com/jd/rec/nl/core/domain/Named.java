package com.jd.rec.nl.core.domain;

/**
 * 可标识的对象
 *
 * @author linmx
 * @date 2018/5/28
 */
public interface Named {

    default String getNamespace() {
        return "default";
    }

    String getName();

    void setName(String name);
}
