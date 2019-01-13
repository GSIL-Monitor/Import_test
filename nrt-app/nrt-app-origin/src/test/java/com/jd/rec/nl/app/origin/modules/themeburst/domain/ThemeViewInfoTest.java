package com.jd.rec.nl.app.origin.modules.themeburst.domain;

import com.google.gson.Gson;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author linmx
 * @date 2018/11/5
 */
public class ThemeViewInfoTest {

    @Test
    public void test() {
        Set<Integer> feature = new HashSet<>();
        feature.add(1);
        ThemeViewInfo info = new ThemeViewInfo(111, "aaa", feature);
        Gson gson = new Gson();
        System.out.println(gson.toJson(info));
    }
}