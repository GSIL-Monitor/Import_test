package com.jd.rec.nl.app.origin.modules.statisticsuids;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.service.base.quartet.Schedule;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.ump.profiler.proxy.Profiler;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 统计所有uids的数量
 *
 * @author wl
 * @date 2018/10/11
 */
public class UidsCount implements Updater, Schedule {

    private static final Logger LOGGER = getLogger(UidsCount.class);
    /**
     * 使用BloomFilter
     */
    BloomFilter bloomFilter;
    private String name = "uidsCount";
    @Inject(optional = true)
    @Named("windowSize")
    private Duration windowDuration = Duration.ofMinutes(60);
    private int before = 0;

    @Inject
    public UidsCount(@Named("falsePositiveProbability") double falsePositiveProbability, @Named("expectedNumberOfElements") int
            expectedNumberOfElements) {
        bloomFilter = new BloomFilter(falsePositiveProbability, expectedNumberOfElements);
        before = conversionTime(System.currentTimeMillis());
    }

    public UidsCount() {

    }

    @Override
    public int intervalSize() {
        return Long.valueOf(windowDuration.getSeconds() / defaultInterval.getSeconds()).intValue();
    }

    @Override
    public void trigger(ResultCollection resultCollection) {
        Profiler.valueAccumulate("NRT_UIDSCOUNT", bloomFilter.count());
        int now = conversionTime(System.currentTimeMillis());
        //超过1天进行删除
        if (now > before) {
            bloomFilter.clear();
            before = now;
        }
    }

    @Override
    public void update(MapperContext mapperContext, ResultCollection resultCollection) {
        String uid = mapperContext.getUid();
        if (!bloomFilter.contains(uid)) {
            bloomFilter.add(uid);
        }
        LOGGER.debug("count:" + bloomFilter.count());
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
