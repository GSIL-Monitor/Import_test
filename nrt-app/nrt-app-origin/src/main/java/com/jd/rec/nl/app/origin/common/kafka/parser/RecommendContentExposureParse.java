package com.jd.rec.nl.app.origin.common.kafka.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.parse.MessageParse;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;

import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/21
 */
public class RecommendContentExposureParse implements MessageParse<Message<String>> {

    private String topic;

    public RecommendContentExposureParse(String topic) {
        this.topic = topic;
    }

    @Override
    public String getMessageSource() {
        return topic;
    }

    @Override
    public ImporterContext parse(Message<String> message) throws Exception {
        final String itemlog = message.getMessageValue();
        if (itemlog.isEmpty()) {
            throw new InvalidDataException("itemlog is empty! tuple: " + itemlog);
        }

        JSONObject itemlogs = JSON.parseObject(itemlog);

        if (!itemlogs.containsKey("clp") || itemlogs.getString("clp").isEmpty()
                || itemlogs.getString("clp").equals("null")) {
            throw new InvalidDataException("itemlogs doesn't contain clp or clp is null. tuple: " + itemlog);
        }
        String version = itemlogs.getString("apv");
        if (itemlogs.getString("rtm").isEmpty()) {  //上报时间
            throw new InvalidDataException("rtm, pin, clp is empty or don't contain these fields. tuple: " + itemlog);
        }

        String time = URLDecoder.decode(itemlogs.getString("rtm"), "UTF-8");//点击时间
        String clp = URLDecoder.decode(itemlogs.getString("clp"), "UTF-8");//类event_param
        //String pin = itemlogs.getString("pin");//user_log_acct
        String uid = itemlogs.getString("uid");//uid

        Set<String> niceGoodsIds = new HashSet<>();

        //Tab名称#Tab位置#Tab总数#素材id1$素材类型名称1$testid1$biinfo1_素材id2$素材类型名称2$testid2$biinfo2_..._素材idn$素材类型名称n$testidn$biinfon
        if (clp.split("#")[0].contains("推荐")) {
            int index1 = clp.indexOf("#");
            int index2 = clp.indexOf("#", index1 + 1);
            int index3 = clp.indexOf("#", index2 + 1);

            String niceGoods = clp.substring(index3 + 1);

            for (String clpInfo : niceGoods.split("_")) {
                if (clpInfo.split("\\$").length >= 4) {
                    String niceGoodsId = clpInfo.split("\\$")[0];
                    niceGoodsIds.add(niceGoodsId);
                }
            }
        }

        if (niceGoodsIds.isEmpty()) {
            LOGGER.error("input has no valid nice goodsId:{}", itemlog);
            throw new InvalidDataException("input has no valid nice goodsId: " + itemlog);
        }

        String pin=itemlogs.getString("pin");
        if (pin == null || pin.isEmpty()) {
            pin="";
        }
        ImporterContext context = new ImporterContext();
        context.setPin(pin);
        context.setUid(uid);
        context.setContentIds(niceGoodsIds);
        context.setTimestamp(Long.parseLong(time));
        context.setBehaviorField(BehaviorField.EXPOSURE);
        context.setSource(topic);
        return context;
    }

    private static final Logger LOGGER = getLogger(RecommendContentExposureParse.class);

}
