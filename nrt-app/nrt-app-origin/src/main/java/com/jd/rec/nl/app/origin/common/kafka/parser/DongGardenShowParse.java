package com.jd.rec.nl.app.origin.common.kafka.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.service.common.parse.MessageParse;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;

/**
 * @author linmx
 * @date 2018/9/28
 */
public class DongGardenShowParse implements MessageParse<Message<String>> {

    private static final String TOPIC = "appsdk_click.AllGardens_Main_ProductExp_V2";

    @Override
    public String getMessageSource() {
        return TOPIC;
    }

    @Override
    public ImporterContext parse(Message<String> message) throws Exception {
        final String messageValue = message.getMessageValue();
        if (messageValue.isEmpty()) {
            throw new InvalidDataException("message is empty! tuple: " + messageValue);
        }

        JSONObject itemLogs = JSON.parseObject(messageValue);
        if (!itemLogs.containsKey("par") || itemLogs.getString("par").length() == 0
                || !itemLogs.containsKey("uid") || itemLogs.getString("uid").length() == 0) {
            throw new InvalidDataException("message doesn't have necessary information! tuple: " + messageValue);
        }
        long time = itemLogs.containsKey("rtm") ? itemLogs.getLong("rtm") : System.currentTimeMillis();

        String par = itemLogs.getString("par");
        JSONObject pars = JSON.parseObject(par);
        if (!pars.containsKey("themeId")) {
            throw new InvalidDataException("message doesn't have theme! tuple: " + messageValue);
        }
        long themeID = Long.parseLong(pars.getString("themeId"));

        String uuid = itemLogs.getString("uid");
        String pin=itemLogs.getString("pin");
        if (pin == null || pin.isEmpty()) {
            pin="";
        }
        ImporterContext importerContext = new ImporterContext();
        importerContext.setPin(pin);
        importerContext.setBehaviorField(BehaviorField.EXPOSURE);
        importerContext.setUid(uuid);
        importerContext.setTimestamp(time);
        importerContext.setThemeId(themeID);
        importerContext.setSource(TOPIC);
        return importerContext;
    }
}
