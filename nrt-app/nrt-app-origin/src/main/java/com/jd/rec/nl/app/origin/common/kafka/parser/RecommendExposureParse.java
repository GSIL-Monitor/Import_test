package com.jd.rec.nl.app.origin.common.kafka.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.parse.MessageParse;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/19
 */
public class RecommendExposureParse implements MessageParse<Message<String>> {

    private static final Logger LOGGER = getLogger(RecommendExposureParse.class);
    private String topic;

    public RecommendExposureParse(String topic) {
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
        String version = itemlogs.getString("apv");
        if (itemlogs.getString("rtm").isEmpty()) {  //上报时间
            throw new InvalidDataException("rtm, pin, clp is empty or don't contain these fields. tuple: " + itemlog);
        }

        String time = URLDecoder.decode(itemlogs.getString("rtm"), "UTF-8");//点击时间
        String clp = URLDecoder.decode(itemlogs.getString("clp"), "UTF-8");//类event_param
        //String pin = itemlogs.getString("pin");//user_log_acct
        String uid = itemlogs.getString("uid");//uid

        // expid is in clp
        List<String[]> skus = getSkuFromCLP(version, clp, time); // sku也是id

        if (skus.size() == 0) {
            // 不需要打印log，有一些为零的情况是因为重复上曝的日志被处理掉了。
            throw new InvalidDataException("sku or id is null or empty, return. " + itemlog);
        }

        Set<Long> countSku = new HashSet<>();
        for (String[] sku_info : skus) {
            // 发现部分异常数据，需要check sku为long
            // sku, source, expid, time, reqsig, empty
            // 如果时间是未来的时间（以超过当前batch endtime为标准），以及是2天前的数据，均过滤掉不进行统计
            if (Long.parseLong(sku_info[3]) > System.currentTimeMillis() / 1000) {
                //dropFutureEventNum += 1;
                sku_info[3] = String.valueOf(System.currentTimeMillis() / 1000);
                //continue;
            } else if (Long.parseLong(sku_info[3]) < System.currentTimeMillis() / 1000 - 3600 * 24 * 2) {
                continue;
            }
            try {
                countSku.add(Long.parseLong(sku_info[0]));
            } catch (NumberFormatException e) {
                continue;
            }
        }
        if (countSku.isEmpty()) {
//            LOGGER.error("input has no valid sku:{}", itemlog);
            throw new InvalidDataException("input has no valid sku: " + itemlog);
        }
        String pin=itemlogs.getString("pin");
        if (pin == null || pin.isEmpty()) {
            pin="";
        }
        ImporterContext context = new ImporterContext();
        context.setPin(pin);
        context.setUid(uid);
        context.setSkus(countSku);
        context.setTimestamp(Long.parseLong(time));
        context.setBehaviorField(BehaviorField.EXPOSURE);
        context.setSource(topic);
        return context;
    }

    public List<String[]> getSkuFromCLP(String version, String clp, String rtm) throws InvalidDataException {
        List<String[]> skus = new ArrayList<String[]>();
        //如果是曝光
        for (String clpInfo : clp.split("_")) {
            // *
            // SKU1曝光时间#SKU1#Reqsig1#sid1#flow1#source1#plus1#expid1#promotion1#reasonid1#page1#index1#skutype1#jumptype1
            // #基因id1#基因位置1#empty1#saleinfo1
            try {
                String time = "-";
                String sku = "-";
                String source = "null";
                String expid = "null";
                String reqsig = "null";
                String[] splitBy = clpInfo.split("#");

                if (version.compareTo("6.1.0") < 0) {
                    String[] tmp = clpInfo.split("-");
                    sku = tmp[0];
                    time = tmp[1];
                    try {
                        reqsig = splitBy[0].split("-")[2];
                    } catch (Exception e) {
                        // pass
                    }
                    if (splitBy.length >= 4 && (splitBy[3].equals("0") || splitBy[3].equals("1"))) {
                        source = splitBy[3];
                    }
                    if (splitBy.length >= 8) {
                        expid = splitBy[7];
                    }
                } else {
                    time = splitBy[0];
                    sku = splitBy[1];
                    reqsig = splitBy[2];   //请求唯一标识符
                    source = splitBy[5];   //请求来源，1： 广告， 0：推荐
                    expid = splitBy[7];    //实验id
                }
                if (sku.matches("[0-9]+")) {
                    skus.add(new String[]{sku, source, expid, time, reqsig});
                }
            } catch (Exception e) {
                throw new InvalidDataException("parse single clp failed: " + clpInfo);
            }
        }

        return skus;
    }

}
