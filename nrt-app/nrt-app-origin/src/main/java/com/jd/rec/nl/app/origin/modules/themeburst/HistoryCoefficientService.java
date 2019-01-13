package com.jd.rec.nl.app.origin.modules.themeburst;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.DimensionValue;
import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.HistoryCUV;
import com.jd.rec.nl.app.origin.modules.themeburst.domain.ThemeViewInfo;
import com.jd.rec.nl.service.modules.db.service.DBService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/9/28
 */
@Singleton
public class HistoryCoefficientService {

    @Inject
    private DBService dbService;

    public void getAndRefreshHc(String namespace, DimensionValue userDimensionValue, HashMap<Long, ThemeViewInfo> viewInfos,
                                double alpha, Duration ttl)
            throws Exception {
        Map<Long, String> keys = new HashMap<>();
        viewInfos.forEach((themeId, themeInfo) -> {
            keys.put(themeId, String.valueOf(themeId).concat(".").concat(userDimensionValue.toString()));
        });
        Map<String, HistoryCUV> historyCUVs = dbService.batchQuery(namespace, keys.values());
        long time = System.currentTimeMillis();
        viewInfos.forEach((themeId, themeInfo) -> {
            String key = keys.get(themeId);
            HistoryCUV historyCUV = historyCUVs.get(key);
            double hc = 0;
            // 获取前一个hc
            double preHc = 0;
            if (historyCUV == null) {
                historyCUV = new HistoryCUV();
                historyCUV.setTimestamp(time);
                historyCUVs.put(key, historyCUV);
            } else {
                preHc = historyCUV.getCoefficient();
                historyCUV.setTimestamp(time);
            }
            // 计算当前的hc
            long uv = themeInfo.getUV(1);
            hc = BigDecimal.valueOf(preHc).multiply(BigDecimal.valueOf(alpha))
                    .add((BigDecimal.valueOf(1).subtract(BigDecimal.valueOf(alpha))).multiply(BigDecimal.valueOf(uv)))
                    .doubleValue();
            historyCUV.setCoefficient(hc);
            themeInfo.setHc(hc);
            themeInfo.setPreHc(preHc);
        });
        // 保存当前的hc
        dbService.batchSave(namespace, historyCUVs, ttl);
    }
}
