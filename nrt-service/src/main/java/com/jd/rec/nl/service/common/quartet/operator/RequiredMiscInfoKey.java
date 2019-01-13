package com.jd.rec.nl.service.common.quartet.operator;

import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author wanlong
 * @date 2018/8/22
 */
public interface RequiredMiscInfoKey {

    Map<String, RequiredMiscInfoKey> storedKeyProviders = new HashMap<>();

    Logger LOGGER = getLogger(RequiredMiscInfoKey.class);


    static RequiredMiscInfoKey getProvider(String className) {
        if (!storedKeyProviders.containsKey(className)) {
            synchronized (storedKeyProviders) {
                if (!storedKeyProviders.containsKey(className)) {
                    try {
                        storedKeyProviders.put(className, (RequiredMiscInfoKey) Class.forName(className).newInstance());
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }
        return storedKeyProviders.get(className);
    }

    Set<String> getKeys(MapperContext mapperContext);
}
