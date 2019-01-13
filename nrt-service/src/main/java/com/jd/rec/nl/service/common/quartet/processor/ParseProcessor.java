package com.jd.rec.nl.service.common.quartet.processor;

import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.service.common.quartet.LazyInitializer;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.parse.MessageParse;
import com.jd.rec.nl.service.common.parse.MessageParseFactory;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/1
 */
public class ParseProcessor implements LazyInitializer {

    private static final Logger LOGGER = getLogger(ParseProcessor.class);

    MessageParseFactory messageParseFactory;

    public ImporterContext parse(Message input) throws Exception {
        try {
            MessageParse messageParse = messageParseFactory.get(input);
            ImporterContext context = messageParse.parse(input);
            return context;
        } catch (InvalidDataException e) {
            LOGGER.info(e.getMessage(), e);
            return null;
        }
    }

    public void init() {
        messageParseFactory = InjectorService.getCommonInjector().getInstance(MessageParseFactory.class);
    }

}
