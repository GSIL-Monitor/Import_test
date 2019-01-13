package com.jd.rec.nl.app.origin.modules.promotionalburst.dataprovider;


import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import org.slf4j.Logger;
import p13.recsys.UserProfilePriceGrade;

import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author wl
 * @date 2018/9/18
 */
public class UserPriceGrade extends UserProfileValue {

    private static final Logger LOGGER = getLogger(UserPriceGrade.class);

    @Override
    public Object getValue(MapperContext context, long sku) {
        if (context.getUserProfile() == null || context.getSkuProfiles() == null) {
            return null;
        }
        java.util.Optional<UserProfilePriceGrade.UnifiedUserProfilePriceGradeProto> modelMessage = context.getUserProfile()
                .getProfile(this.getId());
        if (modelMessage.isPresent()) {
            Map<Long, ItemProfile> itemProfileMap = context.getSkuProfiles();
            ItemProfile itemProfile = itemProfileMap.get(sku);
            if (itemProfile == null) {
                return null;
            }
            String cid3 = String.valueOf(itemProfile.getCid3());
            double weight = modelMessage.get().getUserPrcWeight();
            List<UserProfilePriceGrade.Cid3Proper> cid3Price = modelMessage.get().getCid3PrcWeightList();
            if (cid3Price != null) {
                for (int i = 0; i < cid3Price.size(); i++) {
                    if (cid3Price.get(i).getProper().equals(cid3)) {
                        weight = cid3Price.get(i).getWeight();
                        break;
                    }
                }
            }
            return priceValue(weight);
        } else {
            return null;
        }

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
