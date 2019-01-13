package com.jd.rec.nl.app.origin.modules.entrance;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.entrance.template.ContentUpdate;

/**
 * @author wanlong3
 * @date 2018/12/1
 */
public class EntranceFloorUpdate extends ContentUpdate {

    private String name = "entrance_floor";

    private String floorToContentModelName = "nl_entrance_recalled_contents";

    @Inject
    @Named("tableName_floor")
    private String tableNameFloor;

    @Override
    public String getContentModelName() {
        return floorToContentModelName;
    }

    @Override
    public String getTableName() {
        return tableNameFloor;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
