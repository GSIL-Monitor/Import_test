package com.jd.rec.nl.core.input;

/**
 * @author linmx
 * @date 2018/5/31
 */
public interface SourceBuilder<T> {

    T build(SourceInfo sourceInfo) throws Exception;
}
