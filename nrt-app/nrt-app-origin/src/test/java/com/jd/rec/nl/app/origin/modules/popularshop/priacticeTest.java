package com.jd.rec.nl.app.origin.modules.popularshop;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class priacticeTest {

    @Test
    public void test1(){
        List<Double> list = new ArrayList<>();
        list.add(1.0);
        list.add(2.0);
        list.add(3.0);
        list.add(4.0);
        int listSize = list.size();
        System.out.println(listSize);
        if(listSize % 2 ==0){
            double x = list.get(listSize/2);
            double y = list.get(listSize/2 - 1);
            //保留2位
           // double media = new BigDecimal((float)(x+y)/2).setScale(2,BigDecimal.ROUND_DOWN).floatValue();
            double media = (x+y) / 2;
            System.out.println(x+":"+y+":"+"media:"+media);
        }else{
            double media  = list.get(listSize / 2);
            System.out.println("media:"+media);
        }
    }
}
