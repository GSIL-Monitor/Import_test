package com.jd.rec.nl.app.origin.modules.activityburst.dataprovider;


import com.jd.rec.nl.app.origin.modules.promotionalburst.dataprovider.UserProfileValue;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author zec
 * @date 2018/9/25
 */
public class ActivityUserPriceGrade extends UserProfileValue {

    private static final Logger LOGGER = getLogger(ActivityUserPriceGrade.class);

    @Override
    public Object getValue(MapperContext context, long activityId) {
       double price=(double)super.getValue(context,activityId);
       return priceValue(price);
    }

    /**
     * 根据weight获取区间
     *
     * @param weight
     * @return
     */
    public Integer priceValue(double weight) {
        if (weight > 1 || weight < 0) {
            LOGGER.error("weight out of range:" + weight);
            return null;
        } else if (weight >= 0 && weight < 0.2) {
            return 1;
        } else if (weight >= 0.2 && weight < 0.4) {
            return 2;
        } else if (weight >= 0.4 && weight < 0.6) {
            return 3;
        } else if (weight >= 0.6 && weight < 0.8) {
            return 4;
        } else {
            return 5;
        }
    }
}
