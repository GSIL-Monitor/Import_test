package com.jd.rec.nl.app.origin.modules.entrance;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.entrance.template.ContentUpdate;

/**
 * @author wanlong3
 * @date 2018/12/1
 */
public class EntranceChannelUpdate extends ContentUpdate {

    private String name = "entrance_channel";

    @Inject
    @Named("tableName_channel")
    private String tableNameChannel;

    private String channelToContentModelName = "nl_entrance_channel_to_contents";

    @Override
    public String getContentModelName() {
        return channelToContentModelName;
    }

    @Override
    public String getTableName() {
        return tableNameChannel;
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
