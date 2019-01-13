package com.jd.rec.nl.core.debug;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author linmx
 * @date 2018/6/25
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Mock {

    /**
     * mock target
     *
     * @return
     */
    Class value();

    boolean enable() default true;
}
