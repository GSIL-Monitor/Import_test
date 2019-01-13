package com.jd.rec.nl.service.common.parse;

import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;

/**
 * @author linmx
 * @date 2018/6/1
 */
public interface MessageParse<I extends Message> {

    String getMessageSource();

    ImporterContext parse(I message) throws Exception;

}
