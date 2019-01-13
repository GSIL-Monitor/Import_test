package com.jd.rec.nl.test.junit5.callback.annontation;

import com.jd.rec.nl.test.junit5.callback.callback.InfrastructureJUnitMock;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author linmx
 * @date 2018/11/8
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(InfrastructureJUnitMock.class)
public @interface MockInfrastructure {
}
