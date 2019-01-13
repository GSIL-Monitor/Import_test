package com.jd.rec.nl.service.common.dataprovider;

import com.google.protobuf.GeneratedMessageV3;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.typesafe.config.Optional;

/**
 * @author linmx
 * @date 2018/7/24
 */
public class UserProfileInfo {

    /**
     * 别名
     */
    @Optional
    private String name;

    /**
     * 模型id
     */
    private String id;

    /**
     * 使用的模型字段
     */
    private String param;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public Object getValue(MapperContext context) {
        if (context.getUserProfile() == null) {
            return null;
        }
        java.util.Optional<GeneratedMessageV3> modelMessage = context.getUserProfile().getProfile(this.getId());
        if (modelMessage.isPresent()) {
            Object modelValue = modelMessage.get().getAllFields().get(modelMessage.get().getDescriptorForType()
                    .findFieldByName(param));
            return modelValue;
        } else {
            String errorMessage = String.format("user[%s] model[%s] is not present!!", context.getUid(), this.getId());
            throw new InvalidDataException(errorMessage);
        }
    }

}
