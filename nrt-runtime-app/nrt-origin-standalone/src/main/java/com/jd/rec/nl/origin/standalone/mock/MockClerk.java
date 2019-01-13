package com.jd.rec.nl.origin.standalone.mock;

import com.google.inject.Singleton;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.service.infrastructure.Clerk;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/6/20
 */
@Mock(Clerk.class)
@Singleton
public class MockClerk extends Clerk {

    public MockClerk() {
        super();
    }

    @Override
    public Map<Long, ItemProfile> getProfiles(Collection<Long> skus) {
        Map<Long, ItemProfile> ret = new HashMap<>();
        skus.forEach(sku -> {
            ItemProfile itemProfile = new ItemProfile();
            itemProfile.setSku(sku);
            ret.put(sku, itemProfile);
        });
        return ret;
    }
}
