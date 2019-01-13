package com.jd.rec.nl.service.base.quartet.impl.domain;

/**
 * @author linmx
 * @date 2018/6/13
 */
public class Related {

    String tableName;
    int limit;

    public Related() {
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
