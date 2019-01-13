package com.jd.rec.nl.service.common.cache.levelII;

import com.jd.rec.nl.core.cache.CacheDefine;
import com.jd.rec.nl.core.cache.guava.GuavaConfiguration;

public class LevelIICacheConfiguration<K, V> extends GuavaConfiguration<K, V> {

    /**
     * 二级缓存的定义
     */
    protected CacheDefine levelIICacheDefine;


    public CacheDefine getLevelIICacheDefine() {
        return levelIICacheDefine;
    }

    public void setLevelIICacheDefine(CacheDefine levelIICacheDefine) {
        this.levelIICacheDefine = levelIICacheDefine;
    }
}
