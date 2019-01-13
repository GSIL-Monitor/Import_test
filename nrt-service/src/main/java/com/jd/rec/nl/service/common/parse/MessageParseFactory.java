package com.jd.rec.nl.service.common.parse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.guice.ApplicationContext;
import com.jd.rec.nl.core.guice.InjectorService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/6/1
 */
@Singleton
public class MessageParseFactory implements Serializable {

    private Map<String, MessageParse> storedParser = new HashMap<>();

    @Inject
    public MessageParseFactory() {
        for (MessageParse messageParse : ApplicationContext.getRawDefinedBeans(MessageParse.class)) {
            InjectorService.getCommonInjector().injectMembers(messageParse);
            storedParser.put(messageParse.getMessageSource(), messageParse);
        }
    }

    public <M extends Message> MessageParse get(M message) {
        String topic = message.getMessageSource();
        return storedParser.get(topic);
    }
}
