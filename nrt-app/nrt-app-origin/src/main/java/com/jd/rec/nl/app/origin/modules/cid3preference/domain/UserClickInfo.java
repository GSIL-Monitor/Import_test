package com.jd.rec.nl.app.origin.modules.cid3preference.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 用户最近7天的点击数据
 * @author wl
 * @date 2018/9/3
 */
public class UserClickInfo {
    int current;
    Map<Integer,Cid3ClickInfo> cid3ClickInfos = new HashMap<>();
    Set<Integer> topCid3 = new HashSet<>();
    Map<Integer,Integer> totalClickCount = new HashMap<>();
    public UserClickInfo(){

    }
    public UserClickInfo(int current, Map<Integer, Cid3ClickInfo> cid3ClickInfos, Set<Integer> topCid3, Map<Integer, Integer> totalClickCount) {
        this.current = current;
        this.cid3ClickInfos = cid3ClickInfos;
        this.topCid3 = topCid3;
        this.totalClickCount = totalClickCount;

    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public Map<Integer, Cid3ClickInfo> getCid3ClickInfos() {
        return cid3ClickInfos;
    }

    public void setCid3ClickInfos(Map<Integer, Cid3ClickInfo> cid3ClickInfos) {
        this.cid3ClickInfos = cid3ClickInfos;
    }

    public Set<Integer> getTopCid3() {
        return topCid3;
    }

    public void setTopCid3(Set<Integer> topCid3) {
        this.topCid3 = topCid3;
    }

    public Map<Integer, Integer> getTotalClickCount() {
        return totalClickCount;
    }

    public void setTotalClickCount(Map<Integer, Integer> totalClickCount) {
        this.totalClickCount = totalClickCount;
    }


    @Override
    public String toString() {
        return "UserClickInfo{" +
                "current=" + current +
                ", cid3ClickInfos=" + cid3ClickInfos +
                ", topCid3=" + topCid3 +
                ", totalClickCount=" + totalClickCount +
                '}';
    }
}
