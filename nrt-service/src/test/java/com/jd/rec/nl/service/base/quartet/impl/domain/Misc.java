package com.jd.rec.nl.service.base.quartet.impl.domain;

/**
 * @author linmx
 * @date 2018/6/13
 */
public class Misc {
    String key;
    String tableName;
    int limit;

    public Misc() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
