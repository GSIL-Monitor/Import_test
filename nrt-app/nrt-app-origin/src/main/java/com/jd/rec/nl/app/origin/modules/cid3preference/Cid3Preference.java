package com.jd.rec.nl.app.origin.modules.cid3preference;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.cid3preference.domain.Cid3ClickInfo;
import com.jd.rec.nl.app.origin.modules.cid3preference.domain.UserClickInfo;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.common.quartet.operator.SimpleUpdater;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import p13.recsys.UnifiedUserProfile2Layers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * cid3偏好计算
 *
 * @author wl
 * @date 2018/9/3
 */
public class Cid3Preference implements SimpleUpdater<Map<Integer, Double>> {

    private static final Logger LOGGER = getLogger(Cid3Preference.class);

    private String name = "cid3Preference";

    @Inject
    @Named("liveTime")
    private Duration liveTime;

    @Inject
    @Named("modelId")
    private int subType;


    @Inject
    @Named("saveSize")
    private int saveSize;

    private double[] decayCoefficients;

    @Inject
    private Cid3CofficientService cid3CofficientService;

    public Cid3Preference() {

    }

    @Inject
    public Cid3Preference(@Named("alpha") double alpha) {
        double[] decayCoefficients = {1D, Math.exp(alpha * 1), Math.exp(alpha * 2), Math.exp(alpha * 3), Math.exp(alpha * 4), Math
                .exp(alpha * 5), Math.exp(alpha * 6)};
        this.decayCoefficients = decayCoefficients;
    }

    private static long untilNow(Integer time) {
        LocalDate localDate = LocalDate.parse(time.toString(), DateTimeFormatter.ofPattern("yyyyMMdd"));
        return localDate.until(LocalDate.now(), ChronoUnit.DAYS);
    }

    @Override
    public String getKey(MapperContext context, Map<Integer, Double> ret) {
        return "nl_nrtup_".concat(context.getEventContent().getUid());
    }

    @Override
    public String getType() {
        return "USER_PROFILE_NRT";
    }

    @Override
    public int getSubType() {
        return subType;
    }

    @Override
    public Duration expireTime() {
        return liveTime;
    }

    @Override
    public byte[] serialize(MapperContext mapperContext, Map<Integer, Double> result) {
        UnifiedUserProfile2Layers.UnifiedUserProfile2layersProto.Builder builder = UnifiedUserProfile2Layers
                .UnifiedUserProfile2layersProto.newBuilder();
        builder.setUid(mapperContext.getEventContent().getUid());
        // 遍历topN的cid3得分
        result.entrySet().stream().map(entry -> {
            UnifiedUserProfile2Layers.LevelTwo.Builder levelIIBuilder = UnifiedUserProfile2Layers.LevelTwo.newBuilder();
            levelIIBuilder.setProper(entry.getKey().toString());
            levelIIBuilder.setValue(entry.getValue());
            return levelIIBuilder.build();
        }).forEach(levelTwo -> builder.addLevelTwo(levelTwo));
        return builder.build().toByteArray();
    }

    @Override
    public Map<Integer, Double> update(MapperContext mapperContext) throws Exception {
        String uid = mapperContext.getUid();

        Set<Long> skus = mapperContext.getEventContent().getSkus();
        List<Integer> cid3s = new ArrayList<>();
        if (mapperContext.getSkuProfiles() != null) {
            Map<Long, ItemProfile> itemProfileMap = mapperContext.getSkuProfiles();
            for (long sku : skus) {
                ItemProfile itemProfile = itemProfileMap.get(sku);
                if (itemProfile == null) {
                    continue;
                }
                int cid3 = itemProfile.getCid3();
                cid3s.add(cid3);
            }
        }

        UserClickInfo userClickInfos = cid3CofficientService.userClickInfo(this.getNamespace(), uid);

        addOrRemoveUserClickInfo(cid3s, userClickInfos);
        int current = userClickInfos.getCurrent();
        Set<Integer> topCid3s = userClickInfos.getTopCid3();

        double sum = (userClickInfos.getTotalClickCount().entrySet().stream().map((clickNum) -> {
            int time = clickNum.getKey();
            int click = clickNum.getValue();
            return click * decayCoefficients[(int) untilNow(time)];
        }).mapToDouble(iScore -> Double.valueOf(iScore)).sum());


        Map<Integer, Double> cid3Score = new HashMap<>();
        Set<Integer> newTop = new HashSet<>(topCid3s);
        newTop.addAll(cid3s);
        //获取原top Cid3以及本次点击的cid3的得分
        for (Integer cid3 : newTop) {
            Map<Integer, Integer> clickInfo = userClickInfos.getCid3ClickInfos().get(cid3).getCid3ClickCount();
            if (clickInfo == null) {
                continue;
            }
            int todayClick = clickInfo.containsKey(current) ? clickInfo.get(current) : 0;
            double score = (todayClick + userClickInfos.getCid3ClickInfos().get(cid3).getPreScore()) / sum;
            cid3Score.put(cid3, score);
        }

        limitMaxSize(cid3Score, saveSize);


        //更新userClickInfo
        userClickInfos.setTopCid3(new HashSet<>(cid3Score.keySet()));


        cid3CofficientService.saveNewUserClickInfo(this.getNamespace(), uid, userClickInfos, liveTime);
        return cid3Score;
    }

    /**
     * 增加或删除userClickInfo
     *
     * @param userClickInfo 从缓存中获取UserClickInfo
     * @return 新的UserClickInfo
     */
    protected void addOrRemoveUserClickInfo(List<Integer> cid3s, UserClickInfo userClickInfo) {

        Map<Integer, Integer> cid3Count = computeCid3Num(cid3s);
        int now = conversionTime(System.currentTimeMillis());

        //保存在jimDB上的时间
        long survivalTime = liveTime.toDays();

        if (userClickInfo.getCurrent() != now) {
            //如果不是当前时间就做一个过期处理
            //增加当天的点击记录
            userClickInfo.setCurrent(now);

            //删除总的
            Map<Integer, Integer> totalClickCount = userClickInfo.getTotalClickCount();
            if (totalClickCount != null && !totalClickCount.isEmpty()) {
                totalClickCount = totalClickCount.entrySet().stream().filter(entry -> untilNow(entry.getKey()) < survivalTime)
                        .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
                userClickInfo.setTotalClickCount(totalClickCount);
            }
            totalClickCount.put(now, 0);

            //删除相关cid3的
            Map<Integer, Cid3ClickInfo> cid3ClickInfos = userClickInfo.getCid3ClickInfos();
            if (cid3ClickInfos != null && !cid3ClickInfos.isEmpty()) {
                Set<Integer> nullCid3s = new HashSet<>();
                for (Map.Entry<Integer, Cid3ClickInfo> map : cid3ClickInfos.entrySet()) {
                    //删除过期的cid3
                    int cid3 = map.getKey();
                    Cid3ClickInfo cid3ClickInfo = map.getValue();
                    Map<Integer, Integer> cid3ClickCount = cid3ClickInfo.getCid3ClickCount();
                    cid3ClickCount = cid3ClickCount.entrySet().stream()
                            .filter(entry -> untilNow(entry.getKey()) < survivalTime)
                            .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
                    if (cid3ClickCount.size() == 0) {
                        nullCid3s.add(cid3);
                        continue;
                    }
                    cid3ClickInfo.setCid3ClickCount(cid3ClickCount);
                    //重新计算preSrcore
                    double preScore = computePreScore(decayCoefficients, cid3ClickCount);
                    cid3ClickInfo.setPreScore(preScore);
                }
                nullCid3s.forEach(cid3 -> cid3ClickInfos.remove(cid3));
                //删除cid3
                userClickInfo.getTopCid3().removeAll(nullCid3s);
            }

        }
        //如果是当前时间就给当前的日期做 cid3累加
        Map<Integer, Cid3ClickInfo> cid3ClickInfos = userClickInfo.getCid3ClickInfos();
        Map<Integer, Integer> totalClickCount = userClickInfo.getTotalClickCount();
        totalClickCount.put(now, cid3s.size() + totalClickCount.get(now));
        for (Map.Entry<Integer, Integer> mapCid3 : cid3Count.entrySet()) {
            if (cid3ClickInfos.containsKey(mapCid3.getKey())) {
                //包含这个cid3
                Cid3ClickInfo cid3ClickInfo = cid3ClickInfos.get(mapCid3.getKey());
                Map<Integer, Integer> cid3ClickCount = cid3ClickInfo.getCid3ClickCount();
                //包含这个时间
                if (cid3ClickCount.containsKey(now)) {
                    cid3ClickCount.put(now, mapCid3.getValue() + cid3ClickCount.get(now));

                } else {
                    //不包含这个时间
                    cid3ClickCount.put(now, mapCid3.getValue());

                }
            } else {
                //如果不包含这个cid3的时候
                Map<Integer, Integer> cid3ClickCount = new HashMap<>();
                cid3ClickCount.put(now, mapCid3.getValue());
                Cid3ClickInfo cid3ClickInfo = new Cid3ClickInfo(cid3ClickCount, 0);
                cid3ClickInfos.put(mapCid3.getKey(), cid3ClickInfo);
            }
        }


    }

    /**
     * 将时间转换为int型
     *
     * @param time
     * @return
     */
    private int conversionTime(long time) {
        String BeforeTime = DateFormatUtils.format(new Date(time), "yyyyMMdd");
        int AfterTime = Integer.valueOf(BeforeTime);
        return AfterTime;
    }

    /**
     * 计算cid3的数量
     *
     * @param cid3s 每天的cid3的数量
     * @return
     */
    private Map<Integer, Integer> computeCid3Num(final Collection<Integer> cid3s) {
        final Map<Integer, Integer> cid3Num = new HashMap<>();
        for (Integer cid3 : cid3s) {
            Integer count = cid3Num.get(cid3);
            cid3Num.put(cid3, count == null ? 1 : count + 1);
        }
        return cid3Num;
    }

    /**
     * 计算当天以前的点击得分，每天更新一次
     *
     * @param decayCoefficients 衰减系数
     * @return 返回这些天的得分情况
     */
    private double computePreScore(double[] decayCoefficients, Map<Integer, Integer> cid3ClickCount) {
        double preScore = 0;
        for (Map.Entry<Integer, Integer> map : cid3ClickCount.entrySet()) {
            preScore += map.getValue() * decayCoefficients[(int) untilNow(map.getKey())];
        }
        return preScore;
    }

    /**
     * 计算当天以前的点击得分，每天更新一次
     *
     * @param decayCoefficients 衰减系数
     * @param current           当天的时间
     * @return 返回当天以前这些天的得分情况
     */
    private double computeTotalScore(double[] decayCoefficients, Map<Integer, Integer> totalClickCount, int current) {
        double preScore = 0;
        for (Map.Entry<Integer, Integer> map : totalClickCount.entrySet()) {
            preScore += map.getValue() * decayCoefficients[current - map.getKey()];
        }
        return preScore;
    }

    /**
     * 流程处理结果
     *
     * @param cid3Score
     * @param size
     * @return
     */
    protected void limitMaxSize(Map<Integer, Double> cid3Score, final int size) {
        if (cid3Score.size() <= size) {
            return;
        }
        PriorityQueue<Map.Entry<Integer, Double>> topQueue = new PriorityQueue<>(Comparator.comparing(Map.Entry::getValue));
        topQueue.addAll(cid3Score.entrySet());
        while (cid3Score.size() > size) {
            cid3Score.remove(topQueue.poll().getKey());
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }


    //    @Override
    //    public boolean test(MapperContext mapperContext) {
    //        //        String uid = mapperContext.getEventContent().getUid();
    //        //        if (ConfigBase.getSystemConfig().getStringList("trace.uid").contains(uid)) {
    //        //            return true;
    //        //        }
    //        //        return false;
    //        return true;
    //    }
}
