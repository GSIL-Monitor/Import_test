package com.jd.rec.nl.service.infrastructure;

import com.google.inject.Inject;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.infrastructure.BaseInfrastructure;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/11/19
 */
public class Selector implements BaseInfrastructure {
    private static final Logger LOGGER = getLogger(Selector.class);
    public static final String DB_NAME = "selection";
    @Inject
    @ENV("monitor.selector_key")
    private String monitorKey;
    @Inject
    private DBProxy dbProxy;

    public Map<String, String> getData(Collection<String> keys, String table) {
        Map<String, String> datas = new HashMap<>();
        if(table==null){
            LOGGER.error("table is null   ");
            return datas;
        }
        if(keys.isEmpty()){
            LOGGER.warn("keys is empty "+keys);
            return datas;
        }
           Map<String, byte[]> values = dbProxy.query(keys.stream().map(key -> String.valueOf(table).concat("~~~").concat(key)).collect(Collectors.toList()), DB_NAME);
        for (Map.Entry<String, byte[]> entry : values.entrySet()) {
            String key = entry.getKey();
            byte[] value = entry.getValue();
            if (value != null && key != null) {
                try {
                    String str = new String(value);
                    datas.put(key, str);
                } catch (Exception e) {
                    LOGGER.debug("{}:{}[{}]", e.getMessage(), value, value.length);
                }
            }
        }
        return datas;
    }

}
