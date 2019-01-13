package com.jd.rec.nl.app.origin.modules.promotionalburst.dataprovider;

import com.jd.rec.nl.service.common.dataprovider.UserProfileInfo;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.typesafe.config.Optional;

/**
 * @author wl
 * @date 2018/9/18
 */
public class UserProfileValue extends UserProfileInfo {

    @Optional
    private String className;


    public UserProfileValue getInstance() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        UserProfileValue userProfileValue;
        if (className == null || className.isEmpty()) {
            return this;
        } else {
            userProfileValue = (UserProfileValue) Class.forName(className).newInstance();
            userProfileValue.setId(this.getId());
            userProfileValue.setName(this.getName());
            userProfileValue.setParam(this.getParam());
        }
        return userProfileValue;
    }


    public Object getValue(MapperContext context, long itemId) {
        return super.getValue(context);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
