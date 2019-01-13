package com.jd.rec.nl.service.common.quartet.domain;

import com.jd.rec.nl.service.infrastructure.domain.DataSource;

import java.io.Serializable;


public class PredictorConf implements Serializable {

    private String key;

    private String tableName;

    private int limit;

    private DataSource source = DataSource.predictor;

    public DataSource getSource() {
        return source;
    }

    public void setSource(DataSource source) {
        this.source = source;
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


