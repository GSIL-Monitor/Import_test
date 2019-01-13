package com.jd.rec.nl.app.origin.common.kafka.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.service.common.parse.MessageParse;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/9/20
 */
public class ActivityPVParse implements MessageParse<Message<String>> {

    private static final Logger LOGGER = getLogger(ActivityPVParse.class);

    private String topic;

    public ActivityPVParse(String topic) {
        this.topic = topic;
    }

    @Override
    public String getMessageSource() {
        return topic;
    }

    @Override
    public ImporterContext parse(Message<String> message) throws UnsupportedEncodingException {
        final String itemlog = message.getMessageValue();
        if (itemlog.isEmpty()) {
            throw new InvalidDataException("itemlog is empty! tuple: " + itemlog);
        }
        JSONObject itemlogs = JSON.parseObject(itemlog);
        if (!itemlogs.containsKey("clp") || itemlogs.getString("clp").isEmpty()
                || itemlogs.getString("clp").equals("null")) {
            throw new InvalidDataException("itemlogs doesn't contain clp or clp is null. tuple: " + itemlog);
        }

        if (itemlogs.getString("rtm").isEmpty()) {  //上报时间
            throw new InvalidDataException("rtm, pin, clp is empty or don't contain these fields. tuple: " + itemlog);
        }
        String time = URLDecoder.decode(itemlogs.getString("rtm"), "UTF-8");//点击时间
        String clp = URLDecoder.decode(itemlogs.getString("clp"), "UTF-8");//类event_param
        String uid = itemlogs.getString("uid");//uid
        String[] splitBy = clp.split("_");
        Long activityId;
        Long levelId;
        if (splitBy.length > 2) {
            activityId = Long.parseLong(splitBy[0]);
            levelId = Long.parseLong(splitBy[1]);
        } else
            throw new InvalidDataException("clp is not valid : " + clp);
        String pin=itemlogs.getString("pin");
        if (pin == null || pin.isEmpty()) {
            pin="";
        }
        ImporterContext context = new ImporterContext();
        context.setPin(pin);
        context.setUid(uid);
        context.setTimestamp(Long.parseLong(time));
        context.setActivityId(activityId);
        context.setLevelId(levelId);
        context.setSource(topic);
        context.setBehaviorField(BehaviorField.CLICK);
        return context;
    }
}
