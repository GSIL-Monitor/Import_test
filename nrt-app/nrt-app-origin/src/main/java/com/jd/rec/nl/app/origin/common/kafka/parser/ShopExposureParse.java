package com.jd.rec.nl.app.origin.common.kafka.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.service.common.parse.MessageParse;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;

import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 解析热点店铺
 *
 * @author wl
 * @date 2018/9/20
 */
public class ShopExposureParse implements MessageParse<Message<String>> {

    private static final Logger LOGGER = getLogger(ShopExposureParse.class);

    private String topic;

    /**
     * 这个是在applicationConfig中使用.
     * 例如：ApplicationConfig中的RecommendExposureParse一样
     *
     * @param topic
     */
    public ShopExposureParse(String topic) {
        this.topic = topic;
    }

    @Override
    public String getMessageSource() {
        return topic;
    }

    @Override
    public ImporterContext parse(Message<String> message) throws Exception {
        try {
            final String value = message.getMessageValue();
            if (value == null || value.isEmpty()) {
                throw new InvalidDataException("Null or empty msg received");
            }
            JSONObject itemlogs = JSON.parseObject(value);
            if (itemlogs.getString("rtm").isEmpty()) {
                throw new InvalidDataException("rtm, is empty or don't contain these fields. tuple: " + itemlogs);
            }
            String uid = itemlogs.getString("uid");
            String shopId = itemlogs.getString("shp");
            if (shopId == null || shopId.length() == 0 || Long.valueOf(shopId) <= 0 ) {
                throw new InvalidDataException("rtm,shopId is empty or don't contain these fields. tuple: " + itemlogs);
            }
            String time = URLDecoder.decode(itemlogs.getString("rtm"), "UTF-8");
            String pin=itemlogs.getString("pin");

            if (pin == null || pin.isEmpty()) {
                pin="";
            }
            //店铺Id
            Set<Long> shopIds = new HashSet<>();
            shopIds.add(Long.valueOf(shopId));
            ImporterContext context = new ImporterContext();
            context.setPin(pin);
            context.setUid(uid);
            context.setShopIds(shopIds);
            context.setTimestamp(Long.parseLong(time));
            context.setBehaviorField(BehaviorField.EXPOSURE);
            context.setSource(topic);
            return context;
        } catch (InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidDataException(e);
        }
    }
}
