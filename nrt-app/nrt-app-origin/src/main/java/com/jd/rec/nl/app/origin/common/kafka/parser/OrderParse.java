package com.jd.rec.nl.app.origin.common.kafka.parser;

import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.service.common.parse.MessageParse;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/11/23
 */
public class OrderParse implements MessageParse<Message<String>> {
    private static final Logger LOGGER = getLogger(OrderParse.class);
    private static final String SPLIT = "\t";
    public final static String CHARSET = "utf-8";
    public final static String ORDER_TYPE_TAG = "orderdetail";
    private String TOPIC = "order.100000";

    @Override
    public String getMessageSource() {
        return TOPIC;
    }

    @Override
    public ImporterContext parse(Message<String> message) throws Exception {

        Set<Long> skuSet = new HashSet<>();

        final String value = message.getMessageValue();
        if (value == null || value.isEmpty()) {
            throw new InvalidDataException("Null or empty msg received");
        }

        String[] msgs = value.split(SPLIT);
        if (msgs.length < 10) {
            throw new InvalidDataException("Invalid fields number (" + msgs.length + "<" + 10 + ") msg: " + value);
        }

        final String time = msgs[0];
        final int timeLength = time.length();
        if (timeLength < 13) {
            throw new InvalidDataException("Invalid time field msg: " + value);
        }

        String pin = msgs[5];
        pin = URLDecoder.decode(pin, CHARSET);

        if (isBlank(pin)) {
            throw new InvalidDataException("Invalid pin in msg: " + value);
        }

        String src = msgs.length == 10 ? msgs[8] : msgs[9];
        String orderDetail = getCustomValue(src, ORDER_TYPE_TAG).get(ORDER_TYPE_TAG);
        if (orderDetail != null) {
            String[] skus = orderDetail.split(",");
            if (skus.length > 0) {
                for (String s : skus) {
                    String sku = s.split(":")[0];
                    if (!"".equals(sku)) {
                        try{
                            skuSet.add(Long.valueOf(sku));
                        }catch (NumberFormatException e){
                            throw new InvalidDataException("order中的sku字段不合法");
                        }

                    }
                }
            }
        }

        ImporterContext context = new ImporterContext();
        context.setTimestamp(Long.valueOf(time));
        context.setSkus(skuSet);
        context.setUid(pin);
        context.setPin(pin);
        context.setBehaviorField(BehaviorField.ORDER);
        context.setSource(TOPIC);
        return context;
    }

    /**
     * 判断pin是否未空
     *
     * @param source
     * @return
     */
    public static boolean isBlank(String source) {
        return source == null || "".equals(source.trim()) || "-".equals(source.trim());
    }

    /**
     * the min length is two
     * first  is orignal String to be parse
     * second third fourth .... is the key
     *
     * @param value
     * @return
     */

    public static Map<String, String> getCustomValue(String... value) throws UnsupportedEncodingException {
        Map<String, String> map = null;
        if (value.length < 2) {
            LOGGER.error("the getCustomValue parameter's min length is 2 ");
            return map;
        }
        map = new HashMap<>();
        String st = value[0];
        st = URLDecoder.decode(st, "UTF-8");
        String[] ciin = null;
        String[] wtsends = st.split("\\$");
        for (String i : wtsends) {
            for (int c = 1; c < value.length; c++) {
                if (i.startsWith(value[c])) {
                    ciin = i.split("\\=");
                    if (ciin.length == 2) {
                        map.put(value[c], ciin[1]);
                    }
                }
            }
        }
        return map;
    }
}
