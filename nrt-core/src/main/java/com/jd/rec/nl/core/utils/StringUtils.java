package com.jd.rec.nl.core.utils;

import com.typesafe.config.impl.ConfigImplUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 提供一些比较个性化的字符串处理的工具类
 *
 * @author linmx
 * @date 2018/5/25
 */
public abstract class StringUtils {

    /**
     * 对log进行处理,log格式一般为: key1=value1$key2=value2.....
     *
     * @param log     日志内容,不建议太大
     * @param split   键值对之间的分隔符
     * @param pairSep 键值之间的分隔符
     * @param keys    需要获取的key值
     * @return
     * @throws UnsupportedEncodingException
     */
    public static Map<String, String> parseLog(String log, String split, String pairSep, String... keys) throws
            UnsupportedEncodingException {
        Map<String, String> ret = new HashMap<>();
        if (keys == null || keys.length == 0) {
            return ret;
        }
        List<String> keyList = Arrays.asList(keys);
        String source = URLDecoder.decode(log, "UTF-8");
        String[] sourceSplit = source.split(split);
        for (String sourceKeypair : sourceSplit) {
            String[] keypair = sourceKeypair.split(pairSep);
            if (keypair.length == 2 && keyList.contains(keypair[0])) {
                ret.put(keypair[0], keypair[1]);
            }
        }
        return ret;
    }

    public static Duration parseTypesafeDuration(String input) {
        String s = ConfigImplUtil.unicodeTrim(input);
        String originalUnitString = getUnits(s);
        String unitString = originalUnitString;
        String numberString = ConfigImplUtil.unicodeTrim(s.substring(0, s.length()
                - unitString.length()));
        TimeUnit units = null;

        // this would be caught later anyway, but the error message
        // is more helpful if we check it here.
        if (numberString.length() == 0)
            throw new RuntimeException("No number in duration value '" + input + "'");

        if (unitString.length() > 2 && !unitString.endsWith("s"))
            unitString = unitString + "s";

        // note that this is deliberately case-sensitive
        if (unitString.equals("") || unitString.equals("ms") || unitString.equals("millis")
                || unitString.equals("milliseconds")) {
            units = TimeUnit.MILLISECONDS;
        } else if (unitString.equals("us") || unitString.equals("micros") || unitString.equals("microseconds")) {
            units = TimeUnit.MICROSECONDS;
        } else if (unitString.equals("ns") || unitString.equals("nanos") || unitString.equals("nanoseconds")) {
            units = TimeUnit.NANOSECONDS;
        } else if (unitString.equals("d") || unitString.equals("days")) {
            units = TimeUnit.DAYS;
        } else if (unitString.equals("h") || unitString.equals("hours")) {
            units = TimeUnit.HOURS;
        } else if (unitString.equals("s") || unitString.equals("seconds")) {
            units = TimeUnit.SECONDS;
        } else if (unitString.equals("m") || unitString.equals("minutes")) {
            units = TimeUnit.MINUTES;
        } else {
            throw new RuntimeException("Could not parse time unit '" + originalUnitString + "' (try ns, us, ms, s, m, h, d)");
        }

        try {
            long nanos;
            // if the string is purely digits, parse as an integer to avoid
            // possible precision loss;
            // otherwise as a double.
            if (numberString.matches("[+-]?[0-9]+")) {
                nanos = units.toNanos(Long.parseLong(numberString));
            } else {
                long nanosInUnit = units.toNanos(1);
                nanos = (long) (Double.parseDouble(numberString) * nanosInUnit);
            }
            return Duration.ofNanos(nanos);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not parse duration number '" + numberString + "'");
        }
    }

    private static String getUnits(String s) {
        int i = s.length() - 1;
        while (i >= 0) {
            char c = s.charAt(i);
            if (!Character.isLetter(c))
                break;
            i -= 1;
        }
        return s.substring(i + 1);
    }
}
