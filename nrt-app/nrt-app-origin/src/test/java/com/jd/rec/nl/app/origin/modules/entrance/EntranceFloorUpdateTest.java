package com.jd.rec.nl.app.origin.modules.entrance;

import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.experiment.ExperimentVersionHolder;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import com.jd.si.venus.core.CommenInfo;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EntranceFloorUpdateTest {
    static EntranceFloorUpdate entranceUpdate;

    @Before
    public void prepareDebug() {
        Map config = new HashMap();
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.DBProxy", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Clerk", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Predictor", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Jimdb", true);
        //        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.UnifiedOutput", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Zeus", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.ILogKafka", true);
        config.put("quartet.windowUnit", "10seconds");
        config.put("burst.params.windowSize", "60seconds");
        com.typesafe.config.Config thread = ConfigFactory.parseMap(config);
        ConfigBase.setThreadConfig(thread);

        ExperimentVersionHolder experimentVersionHolder = new ExperimentVersionHolder();
        experimentVersionHolder.register(new EntranceFloorUpdate());
        experimentVersionHolder.build();
        entranceUpdate = (EntranceFloorUpdate) experimentVersionHolder.getNodeAllBranches("entrance_floor").get(0);

    }

    //模拟 MapperContext  中数据
    public MapperContext prepareContext(String uid, long sku, long time, long relSku, long simSku, long contentId, long floorId) {
        MapperContext input = new MapperContext();

        ImporterContext context = new ImporterContext();
        context.setUid(uid);
        context.setBehaviorField(BehaviorField.CLICK);
        context.setSkus(Collections.singleton(sku));
        context.setTimestamp(time);
        input.setEventContent(context);

        Map<String, Map<String, Map<Long, Float>>> simAndRelSkus = new HashMap<>();
        Map<String, Map<Long, Float>> relatedSkus = new HashMap<>();
        Map<Long, Float> related = new HashMap<>();
        related.put(relSku, 0.1f);
        relatedSkus.put(String.valueOf(sku), related);
        simAndRelSkus.put("r_sku_to_sku_rel_by_scan", relatedSkus);
        Map<String, Map<Long, Float>> simSkus = new HashMap<>();
        Map<Long, Float> sim = new HashMap<>();
        sim.put(simSku, 0.1f);
        simSkus.put(String.valueOf(sku), sim);
        simAndRelSkus.put("r_sku_to_sku_gbdt_and_cf_fusion_sim", simSkus);
        input.setSimAndRelSkus(simAndRelSkus);

        Map<String, Map<Long, CommenInfo>> skuExtraProfiles = new HashMap<>();
        Map<Long, CommenInfo> commenInfoMap = new HashMap<>();
        CommenInfo commenInfo = new CommenInfo();
        Map<String, String> map = new HashMap<>();
        map.put("unique_contentid", "111,222");
        commenInfo.setCustomMap(map);
        commenInfoMap.put(sku, commenInfo);
        skuExtraProfiles.put("sku_cont2", commenInfoMap);
        input.setSkuExtraProfiles(skuExtraProfiles);

        Map<String, Map<String, Map<Long, Float>>> miscInfo = new HashMap<>();
        Map<String, Map<Long, Float>> skuPW = new HashMap<>();
        Map<Long, Float> pwInfo = new HashMap<>();
        pwInfo.put(floorId, 0.04f);
        skuPW.put(String.valueOf(contentId), pwInfo);
        miscInfo.put("r_sku_to_content", skuPW);
        input.setMisc(miscInfo);

        return input;
    }

    @Test
    public void update() throws Exception {
        MapperContext mapperContext = prepareContext("uid1", 1411l, 1543474358736l, 11l, 111l, 111l, 111l);
        ResultCollection resultCollection = new ResultCollection();
        entranceUpdate.update(mapperContext, resultCollection);
        MapperContext mapperContext2 = prepareContext("uid2", 1511l, 1543474358738l, 11l, 111l, 111l, 111l);
        ResultCollection resultCollection2 = new ResultCollection();
        entranceUpdate.update(mapperContext2, resultCollection2);

    }

}