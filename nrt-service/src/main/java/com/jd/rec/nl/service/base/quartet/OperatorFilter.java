package com.jd.rec.nl.service.base.quartet;

import com.jd.rec.nl.core.domain.KeyValue;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * @author linmx
 * @date 2018/6/25
 */
public interface OperatorFilter<K extends KeyValue> extends Predicate<K>, Serializable {
}
