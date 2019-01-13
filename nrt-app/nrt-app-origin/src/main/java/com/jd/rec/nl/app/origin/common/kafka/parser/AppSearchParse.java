package com.jd.rec.nl.app.origin.common.kafka.parser;


import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.service.common.parse.MessageParse;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/11/16
 */
public class AppSearchParse implements MessageParse<Message<String>> {
    private String TOPIC = "mapp.000001";
    private static final Logger LOGGER = getLogger(AppSearchParse.class);
    public final static String SPLIT = "\t";
    public final static String CHARSET = "utf-8";
    private static final Pattern vPattern = Pattern.compile("key_word=([^\\$]*)\\$\\S+");

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
//        LOGGER.error("input msg is:  " + value);
//        String[] msg = value.split(SPLIT);
//        LOGGER.error("msg.length: " + msg.length);
//        if (msg.length==10) {
//            for (int i = 0; i <10 ; i++) {
//                LOGGER.error("msg "+i+ " is :"+msg[i]);
//            }
//        }

        String[] msg = value.split(SPLIT);
        if (msg.length < 10) {
            throw new InvalidDataException("Invalid fields number (" + msg.length + "<" + 10 + ") msg: " + value);
        }

        String v = msg.length == 10 ? msg[8] : msg[9];
        if (v != null && v.contains("key_word") && v.contains("filt_type") && !v.contains("expand_name")) {
            v = URLDecoder.decode(v, CHARSET);
            int index = v.indexOf("key_word");
            if (!v.substring((index + 8)).startsWith("=")) {
                v = URLDecoder.decode(v, CHARSET);
            }
            String keyword = getKeywordFromMobileV(v);
            if ("".equals(keyword)) {
                throw new InvalidDataException("Empty keyword in msg: " + value);
            }

            String pin = msg[5];
            if (isBlank(pin) || pin.contains("%uE")) { // 表示pin使用 escape 编码字符，无法正常解析因此直接退出
                throw new InvalidDataException("pin is blank or contains %uE,in msg: " + value);
            }
            pin = URLDecoder.decode(pin, CHARSET);

            long timeStamp=Long.parseLong(msg[0]);
            String uuid = msg[3];
            ImporterContext context = new ImporterContext();
            context.setTimestamp(timeStamp);
            context.setUid(uuid);
            context.setPin(pin);
            context.setKeyWord(keyword);
            context.setBehaviorField(BehaviorField.SEARCH);
            context.setSource(TOPIC);
            return context;
        }else
            throw new InvalidDataException("Invalid String v: " + v);
    }


    public static String getKeywordFromMobileV(String v) throws UnsupportedEncodingException {
        Matcher matcher = vPattern.matcher(v);
        if (matcher.matches()) {
            String parseKey = matcher.group(1);
            parseKey = URLDecoder.decode(parseKey, "GB18030");
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


}
