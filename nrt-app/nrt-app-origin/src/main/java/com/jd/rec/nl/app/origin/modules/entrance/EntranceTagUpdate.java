package com.jd.rec.nl.app.origin.modules.entrance;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.entrance.template.PoolToCid3ToSKU;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * @author wanlong3
 * @date 2018/12/1
 */
public class EntranceTagUpdate extends PoolToCid3ToSKU {

    private String name = "entrance_tag";

    private String tagPoolToSkuModelName = "nl_entrance_recalled_tags";

    Map<Long, List<Integer>> blackChannelToCid3s = new HashMap<>();

    @Inject
    public EntranceTagUpdate(@Named("blackCid3sFor219") List<Integer> blackCid3sFor219) {
        blackChannelToCid3s.put(219L, blackCid3sFor219);
    }

    public EntranceTagUpdate() {
    }

    @Inject
    @Named("tableName_tag")
    private String tableNameTag;


    @Override
    public String getPoolToCid3ToSkusModelName() {
        return tagPoolToSkuModelName;
    }

    @Override
    public String getTableName() {
        return tableNameTag;
    }


    @Override
    public Map<Long, List<Integer>> getBlackChannelToCid3s() {
        return blackChannelToCid3s;
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
