package com.jd.rec.nl.app.origin.common.kafka.parser;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/11/16
 */
public class Search000001ParseTest {
    private static final Pattern urlPattern = Pattern.compile("\\S+\\?keyword=([^&]*)&\\S+"); //
   // private static final Pattern urlPattern = Pattern.compile("\\S+"); //
    public final static String CHARSET="utf-8";

    /**
     * 从url中截取参数keyword的值
     * http://search.jd.com/Search?keyword=%E5%BA%8A%E5%9E%AB%E6%A3%95%E5%9E%AB%E5%B8%AD%E6%A2%A6%E6%80%9D1.2%E7%B1%B3&enc=utf-8&wq=%E5%BA%8A%E5%9E%AB%E6%A3%95%E5%9E%AB%E5%B8%AD%E6%A2%A6%E6%80%9D1.2%E7%B1%B3&pvid=tqkb3tli.53gk5s
     *  %E5%BA%8A%E5%9E%AB%E6%A3%95%E5%9E%AB%E5%B8%AD%E6%A2%A6%E6%80%9D1.2%E7%B1%B3
     */
    @Test
    public void getKeywordFromUrl() throws Exception{
        String  url ="http://search.jd.com/Search?keyword=%E5%BA%8A%E5%9E%AB%E6%A3%95%E5%9E%AB%E5%B8%AD%E6%A2%A6%E6%80%9D1.2%E7%B1%B3&enc=utf-8&wq=%E5%BA%8A%E5%9E%AB%E6%A3%95%E5%9E%AB%E5%B8%AD%E6%A2%A6%E6%80%9D1.2%E7%B1%B3&pvid=tqkb3tli.53gk5s";
       // String  url ="abd";
        Matcher matcher = urlPattern.matcher(url);
        if (true) {
           // String parseKey = matcher.group(1);
            String parseKey ="253D%2525CE%2525DE%2525C9%2525F9%2525CA%2525F3%2525B1%2525EA%2524";

            parseKey = URLDecoder.decode(parseKey, CHARSET);
            System.out.println(parseKey);
        }

    }
}
