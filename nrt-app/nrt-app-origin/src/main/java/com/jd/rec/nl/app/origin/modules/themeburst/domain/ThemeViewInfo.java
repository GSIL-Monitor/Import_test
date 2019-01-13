package com.jd.rec.nl.app.origin.modules.themeburst.domain;

import java.util.*;

/**
 * @author linmx
 * @date 2018/9/29
 */
public class ThemeViewInfo {

    long themeId;

    String featureKey;

    int maxPeriod = 0;

    /**
     * 前一个窗口的hc,用来计算score召回和feature输出
     */
    double preHc;

    /**
     * 当前窗口的hc
     */
    double hc;

    Map<Integer, List<Integer>> periodUVCount = new HashMap<>();

    List<Integer> periodClkCount = new ArrayList<>();

    /**
     * 初始化,主要是初始化各时间段的uv结构
     *
     * @param featureKey
     * @param featureList
     */
    public ThemeViewInfo(long themeId, String featureKey, Set<Integer> featureList) {
        this.themeId = themeId;
        this.featureKey = String.valueOf(themeId).concat(featureKey);
        featureList.forEach(period -> {
            if (period > maxPeriod) {
                maxPeriod = period;
            }
            periodUVCount.put(period, new ArrayList<>());
        });
        this.periodClkCount.add(0);
    }

    public double getPreHc() {
        return preHc;
    }

    public void setPreHc(double preHc) {
        this.preHc = preHc;
    }

    public double getHc() {
        return hc;
    }

    public void setHc(double hc) {
        this.hc = hc;
    }

    public Map<Integer, List<Integer>> getPeriodUVCount() {
        return periodUVCount;
    }

    public List<Integer> getPeriodClkCount() {
        return periodClkCount;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void uvAdd(int periodId) {
        List<Integer> periodInfo = periodUVCount.get(periodId);
        if (periodInfo.size() > 0) {
            int count = periodInfo.get(periodInfo.size() - 1);
            periodInfo.remove(periodInfo.size() - 1);
            periodInfo.add(++count);
        } else {
            periodInfo.add(1);
        }
    }


    public void clkAdd() {
        int count = periodClkCount.get(periodClkCount.size() - 1);
        periodClkCount.remove(periodClkCount.size() - 1);
        periodClkCount.add(++count);
    }

    /**
     * 获取某个时间段累计的uv值
     *
     * @param period
     * @return
     */
    public long getUV(int period) {
        return this.getPeriodUVCount().get(period).stream().mapToLong(c -> Integer.valueOf(c).longValue()).sum();
    }

    public long getClkCount(int period) {
        long clk = 0;
        int size = periodClkCount.size();
        // 循环次数
        int i = size > period ? period : size;
        while (i > 0) {
            clk += periodClkCount.get(size - i);
            i--;
        }
        return clk;
    }

    /**
     * 清理过期,并判断是否已经没有浏览记录
     *
     * @return 是否还有记录
     */
    public boolean clear() {
        // 清理uv记录
        int emptyPeriodNum = 0;
        for (int periodId : new HashSet<>(this.periodUVCount.keySet())) {
            List<Integer> uvCounts = this.periodUVCount.get(periodId);
            if (uvCounts.size() == periodId) {
                uvCounts.remove(0);
            }
            uvCounts.add(0);
            int total = uvCounts.stream().mapToInt(Integer::intValue).sum();
            if (total == 0) {
                emptyPeriodNum++;
            }
        }
        // 清理点击记录
        if (this.periodClkCount.size() == maxPeriod) {
            this.periodClkCount.remove(0);
        }
        this.periodClkCount.add(0);
        int total = this.periodClkCount.stream().mapToInt(Integer::intValue).sum();
        if (total == 0 && emptyPeriodNum == this.periodUVCount.size()) {
            // 说明点击和曝光都没有数据
            return true;
        } else {
            return false;
        }
    }
}
