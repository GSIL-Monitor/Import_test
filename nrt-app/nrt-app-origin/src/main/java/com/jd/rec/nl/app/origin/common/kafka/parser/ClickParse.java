package com.jd.rec.nl.app.origin.common.kafka.parser;

import com.google.inject.Inject;
import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.utils.StringUtils;
import com.jd.rec.nl.service.common.parse.MessageParse;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * @author linmx
 * @date 2018/6/19
 */
public class ClickParse implements MessageParse<Message<String>> {
    private static final Logger LOGGER = getLogger(ClickParse.class);
    private static final String SEP_TAB = "\t";

    private static final String SKU_KEY = "clicks";

    private String TOPIC = "app.010001";

    @Inject(optional = true)
    @ENV("click.timeWindow")
    private long timeWindow = 24;


    @Override
    public String getMessageSource() {
        return TOPIC;
    }

    @Override
    public ImporterContext parse(Message<String> message) {
        long CURRENT_TIME_IN_MILLIS = new Date().getTime();
        long VALID_MSG_START = CURRENT_TIME_IN_MILLIS - timeWindow * 3600L * 1000L;

        final String value = message.getMessageValue();
        if (value == null || value.isEmpty()) {
            throw new InvalidDataException("Null or empty msg received");
        }

        String[] info = value.split(SEP_TAB);
        if (info.length < 10) {
            throw new InvalidDataException("Invalid fields number (" + info.length + "<" + 10 + ") msg: " + value);
        }

        final String uuid = info[3];
        if (uuid == null || uuid.isEmpty()) {
            throw new InvalidDataException("Empty UUID in msg: " + value);
        }
        final String time = info[0];
        final int timeLength = time.length();
        if (timeLength < 13) {
            throw new InvalidDataException("Invalid time field msg: " + value);
        }
        final String timeShot = time.substring(timeLength - 13);
        final long timeShotLong;
        try {
            timeShotLong = Long.parseLong(timeShot);
        } catch (NumberFormatException e) {
            throw new InvalidDataException("Invalid time field: " + timeShot + " in msg: " + value);
        }

        if (timeShotLong < VALID_MSG_START) {
            final Date date = new Date(timeShotLong);
            throw new InvalidDataException("Wrong time: " + date + " in msg: " + value);
        }

        String v = info.length == 10 ? info[8] : info[9];
        if (v == null || v.isEmpty()) {
            throw new InvalidDataException("Null or empty SKU field in :" + value);
        }
        final String sku;
        try {
            Map<String, String> pair = StringUtils.parseLog(v, "\\$", "\\=", SKU_KEY);
            sku = pair.get(SKU_KEY);

        } catch (UnsupportedEncodingException e) {
            throw new InvalidDataException("Unsupported encoding SKU string in: " + v);
        }
        if (sku == null || sku.isEmpty()) {
            throw new InvalidDataException("Empty SKU in msg: " + value);
        }

        Set<Long> skus = new HashSet<>();
        skus.add(Long.parseLong(sku));

        String pin=info[5];
        if (pin == null || pin.isEmpty()) {
           pin="";
        }
        ImporterContext context = new ImporterContext();
        context.setPin(pin);
        context.setUid(uuid);
        context.setSkus(skus);
        context.setTimestamp(timeShotLong);
        context.setBehaviorField(BehaviorField.CLICK);
        context.setSource(TOPIC);

//        LOGGER.warn("---parse--context.getUid()--" + context.getUid());
//        LOGGER.warn("---parse--context.getSkus().toString()--" + context.getSkus().toString());
        return context;
    }

}
