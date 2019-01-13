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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/11/15
 * search000001是网页搜索框的topic
 */
public class Search000001Parse implements MessageParse<Message<String>> {
    private static final Logger LOGGER = getLogger(Search000001Parse.class);
    private static final String SEP_TAB = "\t";
    private static final Pattern urlPattern = Pattern.compile("\\S+\\?keyword=([^&]*)&\\S+"); //
    private static final String REC_TYPE = "rec_type";
    public final static String CHARSET = "utf-8";
    private String TOPIC = "search.000001";

    @Override
    public String getMessageSource() {
        return TOPIC;
    }

    @Override
    public ImporterContext parse(Message<String> message) throws Exception {
        final String value = message.getMessageValue();
        if (value == null || value.isEmpty()) {
            throw new InvalidDataException("Null or empty msg received");
        }
        String[] msg = value.split(SEP_TAB);
        if (msg.length < 9 || !isSearchLegal(value, msg)) {
            throw new InvalidDataException("Invalid fields number (" + msg.length + "<" + 9 + ") msg: " + value);
        }
        String recType = getCustomValue(msg[9], REC_TYPE).get(REC_TYPE);
        if (!isBlank(recType) && (recType.equals("0") || recType.equals("10"))) {  //0和10(无结果的搜索)是搜索行为，1和11是搜索结果页点击
            String pin = URLDecoder.decode(msg[5], CHARSET);
            String url = URLDecoder.decode(msg[7], CHARSET);
       //     String uuid = URLDecoder.decode(msg[10], CHARSET);
            String keyword = getKeywordFromUrl(url);
            if (!"".equals(keyword)) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("validate pc log -- pin=" + pin + ", keyword=" + keyword);
            }
            ImporterContext context = new ImporterContext();
            context.setPin(pin);
          //  context.setUid(uuid);//不确定是不是uuid
            context.setBehaviorField(BehaviorField.SEARCH);
            context.setSource(TOPIC);
//            if (pin.equals("zec123456")) {
//                for (int i = 0; i < 30; i++) {
//                    LOGGER.error(" pin:  " + pin + " msg:  " + value);
//
//                }
//                if (value.contains("530d8163a0f6c9f35a46ab4b5a01aea0c5e9d7d9")) {
//                    LOGGER.error("msg contains uid");
//                }
//            }
            return context;
        } else
            throw new InvalidDataException("Wrong recType: " + recType);
    }

    private boolean isSearchLegal(String orgStr, String[] msg) {
        String pin = msg[5];
        String url = msg[7];
        String v = msg[9];
        boolean result = true;
        if (url.contains("keyword") && url.contains("enc") && url.contains("pvid") && url.contains("wq")) {
            if (isBlank(pin) || isBlank(url) || pin.contains("%uE") || url.contains("%uE")) { // 表示pin|url使用 escape 编码字符，无法正常解析因此直接退出
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("invalidate pc log. url=[" + url + "] pin=[" + pin + "]");
                }
                result = false;
            } else {
                if (isBlank(v)) {
                    result = false;
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("msg v field is empty, msg length:" + msg.length + "--" + orgStr);
                    }
                }
            }
        } else {
            result = false;
        }
        return result;
    }

    /**
     * 从url中截取参数keyword的值
     *
     * @param url http://search.jd.com/Search?keyword=%E5%BA%8A%E5%9E%AB%E6%A3%95%E5%9E%AB%E5%B8%AD%E6%A2%A6%E6%80%9D1.2%E7%B1%B3&enc=utf-8&wq=%E5%BA%8A%E5%9E%AB%E6%A3%95%E5%9E%AB%E5%B8%AD%E6%A2%A6%E6%80%9D1.2%E7%B1%B3&pvid=tqkb3tli.53gk5s
     * @return %E5%BA%8A%E5%9E%AB%E6%A3%95%E5%9E%AB%E5%B8%AD%E6%A2%A6%E6%80%9D1.2%E7%B1%B3
     */
    public static String getKeywordFromUrl(String url) throws UnsupportedEncodingException {
        Matcher matcher = urlPattern.matcher(url);
        if (matcher.matches()) {
            String parseKey = matcher.group(1);
            parseKey = URLDecoder.decode(parseKey, CHARSET);
            return parseKey;
        }
        return "";
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
        map = new HashMap<String, String>();
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