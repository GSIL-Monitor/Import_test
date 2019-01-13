package com.jd.rec.nl.origin.standalone.mock;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.service.infrastructure.UserModel;
import com.jd.si.venus.core.CommenInfo;
import com.jd.si.venus.core.SkuAttributInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author linmx
 * @date 2018/9/11
 */
@Mock(UserModel.class)
@Singleton
public class MockUserModel extends UserModel {

    @Inject
    public MockUserModel() {
        super();
    }

    @Override
    public Map<Long, CommenInfo> getUserModelResponse(Collection<Long> keys, String table) {
        return keys.stream().collect(Collectors.toMap(key -> key, key -> {
            CommenInfo commenInfo = new CommenInfo();
            commenInfo.setCustomAttribute(new ArrayList<>(Arrays.asList("168.0", "-1", "-1", "-1", "0.566")));
            return commenInfo;
        }));
    }

    @Override
    public Map<Long, SkuAttributInfo> getUserModelResponse(Collection<Long> keys) {
        return keys.stream().collect(Collectors.toMap(key -> key, key -> new SkuAttributInfo()));
    }
}
