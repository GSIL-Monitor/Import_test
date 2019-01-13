package com.jd.rec.nl.core.domain;

import java.io.Serializable;

/**
 * Defines a simple key value pair.
 * <p>
 * A Map Entry has considerable additional semantics over and above a simple
 * key-value pair. This interface defines the minimum key value, with just the
 * two get methods.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 * @version $Id: KeyValue.java 1477779 2013-04-30 18:55:24Z tn $
 * @since 3.0
 */
public interface KeyValue<K, V> extends Serializable {

    /**
     * Gets the key from the pair.
     *
     * @return the key
     */
    K getKey();

    /**
     * Gets the value from the pair.
     *
     * @return the value
     */
    V getValue();

}

