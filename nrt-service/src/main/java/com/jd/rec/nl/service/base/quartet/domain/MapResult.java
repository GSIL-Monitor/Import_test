package com.jd.rec.nl.service.base.quartet.domain;

import com.jd.rec.nl.core.domain.KeyValue;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/7/27
 */
public class MapResult<K extends Serializable, V extends Serializable> implements KeyValue<K, V> {

    protected String executorName;

    protected K key;

    protected V value;

    protected boolean forWindow = true;

    public MapResult(String executorName, K key, V value, boolean forWindow) {
        this.executorName = executorName;
        this.key = key;
        this.value = value;
        this.forWindow = forWindow;
    }

    public MapResult(String executorName, K key, V value) {
        this.executorName = executorName;
        this.key = key;
        this.value = value;
    }

    public boolean isForWindow() {
        return forWindow;
    }

    public String getExecutorName() {
        return executorName;
    }

    @Override
    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    @Override
    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public boolean isFin() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("executor:%s, key:%s, value:%s", this.executorName, this.key, this.value);
    }
}
