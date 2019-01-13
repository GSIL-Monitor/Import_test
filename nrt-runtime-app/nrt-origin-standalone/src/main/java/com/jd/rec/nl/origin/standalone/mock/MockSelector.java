package com.jd.rec.nl.origin.standalone.mock;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.service.infrastructure.Selector;

import java.util.*;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/11/19
 */
@Mock(Selector.class)
@Singleton
public class MockSelector extends Selector {

    @Inject
    public MockSelector() {
        super();
    }

    public Map<String, String> getData(Collection<String> keys, String table) {
        Map<String, String> datas = new HashMap<>();
        keys.stream().forEach(key -> datas.put(key, "mock"));
        return datas;
    }

}
