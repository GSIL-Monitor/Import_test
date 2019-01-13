package com.jd.rec.nl.app.origin.modules.popularshop;

import com.jd.rec.nl.app.origin.modules.popularshop.dataprovider.ShopUserPriceGrade;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class CheckShopUVTest2 {

    Map<Long, Set<String>> shopUVorUV5min = new HashMap<>();
    List<Map<Long, Set<String>>> shopSkuUVOrUV1h = new ArrayList<>();
    Map<Long, Set<String>> shopUVorUV5min1 = new HashMap<>();

    @Before
    public void prepareDebug() {

        Set<String> set = new HashSet<>();
        set.add("012");
        set.add("123");
        set.add("456");
        set.add("789");
        shopUVorUV5min.put(1l, set);
        shopUVorUV5min1.put(2L, set);
        shopSkuUVOrUV1h.add(shopUVorUV5min1);

    }

    @Test
    public void testJudgment5m() {
        CheckShopUV checkShopUV = new CheckShopUV();
        //boolean x = checkShopUV.judgment5m(shopSkuUVOrUV1h,shopUVorUV5min,2l,"012");
        System.out.println(1);
    }

    @Test
    public void test2() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        int x = list.get(list.size() - 1);
        x++;
        list.remove(list.get(list.size() - 1));
        list.add(x);
        System.out.println(list);
    }

    @Test
    public void test3() {
        List<Integer> list1 = new ArrayList<>();
        list1.add(1);
        list1.add(2);
        list1.add(3);
        List<Integer> list2 = new ArrayList<>();
        list2.add(1);
        list2.add(3);
        List<Integer> listAll = new ArrayList<>();
        listAll.addAll(list1);
        listAll.addAll(list2);
        System.out.println(listAll);
    }

    @Test
    public void test4() {
        ShopUserPriceGrade shopUserPriceGrade = new ShopUserPriceGrade();
        double x = (double) shopUserPriceGrade.getValue(new CheckShopUVTest().prepareContext("uid1", 111L, 11L, 1, 1, 0.8D, 1, 2), 11L);
        System.out.println(x);
    }

    @Test
    public void test5() {
        int num = 0;
        List<Integer> list1 = new ArrayList<>();
        list1.add(1);
        list1.add(2);
        list1.add(3);
        for (int i = 0; i < list1.size(); i++) {
            num = num + list1.get(i);
        }
        System.out.println(num);
        num = 0;
        List<Integer> list2 = new ArrayList<>();
        list2.add(1);
        list2.add(3);
        for (int i = 0; i < list2.size(); i++) {
            num = num + list2.get(i);
        }
        System.out.println("num:" + num);
    }

    @Test
    public void test6() {
        List<Integer> list1 = new ArrayList<>();
        if (list1.size() == 0) {
            list1.add(0);
        }
        int num = list1.get(list1.size()-1);
        num++;
        list1.remove(list1.size()-1);
        list1.add(num);
        System.out.println(list1);
    }

    @Test
    public void test7(){
        String s = "123";
        Long l = Long.valueOf(s);
        System.out.println(l);
    }

    @Test
    public void test8(){
        List<Map<Long, Set<String>>> shop1h = new ArrayList<>();
        Long shopId = 11L;
        String uid = "uid1";
        for (int i = 0; i < shop1h.size() - 1; i++) {
            if (shop1h.get(i).containsKey(shopId)) {
                System.out.println("false");
            }
        }
        if (shop1h.size() == 0) {
            shop1h.add(new HashMap<>());
        }
        Set<String> uids = shop1h.get(shop1h.size() - 1).get(shopId);
        if (uids == null) {
            uids = new HashSet<>();
            uids.add(uid);
            shop1h.get(shop1h.size() - 1).put(shopId, uids);
            System.out.println("true");
        }
        if (!uids.contains(uid)) {
            uids.add(uid);
            System.out.println("true");
        }
    }

}