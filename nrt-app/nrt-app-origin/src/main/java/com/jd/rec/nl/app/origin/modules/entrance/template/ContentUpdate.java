package com.jd.rec.nl.app.origin.modules.entrance.template;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.entrance.Service.EntranceDBSevice;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.modules.item.service.ItemService;
import com.jd.rec.nl.service.modules.misc.service.MiscService;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import com.jd.rec.nl.service.modules.user.domain.BehaviorInfo;
import com.jd.si.venus.core.CommenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * 内容id模板
 * floor->contentId
 * channle->contentId
 * @author wanlong3
 * @date 2018/12/1
 */
public abstract class ContentUpdate implements Updater {
    private static final Logger LOGGER = LoggerFactory.getLogger("entrance");
    @Inject
    @Named("floor_to_content_size_threshold")
    private int contentSizeThreshold;
    @Inject
    private EntranceDBSevice entranceDBService;

    @Inject
    @Named("tableName_rel")
    private String tableNameRel;

    @Inject
    @Named("tableName_sim")
    private String tableNameSim;

    @Inject
    @Named("tableName_userModel")
    private String tableNameUserModel;

    @Inject
    @Named("limit")
    private int limit;

    public abstract String getContentModelName();

    public abstract String getTableName();

    @Inject
    private MiscService miscService;

    @Inject
    private ItemService itemService;

    @Override
    public void update(MapperContext mapperContext, ResultCollection resultCollection) {
        String uid = mapperContext.getUid();
        Set<String> clickSkus = new HashSet<>();
        Map<Long, Set<Long>> poolToContents = entranceDBService.getPoolToContentIds(uid, getContentModelName());
        Map<BehaviorField, Set<BehaviorInfo>> behaviorFieldSetMap = mapperContext.getBehaviors();
        Set<BehaviorInfo> BehaviorInfos = behaviorFieldSetMap.get(BehaviorField.CLICK);
        if (poolToContents == null || poolToContents.isEmpty()) {
            poolToContents = new HashMap<>();
            if (BehaviorInfos.size() < 3) {
                return;
            }
            for (BehaviorInfo behaviorInfo : BehaviorInfos) {
                clickSkus.add(String.valueOf(behaviorInfo.getSku()));
            }
        } else {
            Set<Long> currentSkus = new HashSet<>(mapperContext.getEventContent().getSkus());
            currentSkus.forEach(sku -> clickSkus.add(String.valueOf(sku)));
        }
        Map<String, Map<Long, Float>> relatedSkus = miscService.getRelatedWithoutNull(clickSkus,
                tableNameRel, 50);
        Map<String, Map<Long, Float>> simSkus = miscService.getRelatedWithoutNull(clickSkus,
                tableNameSim, 12);
        Set<String> skus = mergeRelatedAndSimilarSkus(clickSkus, relatedSkus.values(), simSkus.values());
        Map<Long, CommenInfo> skuExtraProfiles = itemService.getItemExtraProfiles(skus, tableNameUserModel);
        Set<String> contentIds = getContentsId(skuExtraProfiles);
        Map<String, Map<Long, Float>> currentContentToPools = miscService.getRelatedWithoutNull(contentIds, getTableName(), limit);
        addContentToPool(poolToContents, currentContentToPools);
        if (!poolToContents.isEmpty()) {
            entranceDBService.setPoolToContentIds(uid, getContentModelName(), poolToContents);
        }
    }

    /**
     * 得到内容id   contentId
     *
     * @param skuExtraProfiles
     * @return
     */
    public Set<String> getContentsId(Map<Long, CommenInfo> skuExtraProfiles) {
        Set<String> contentIds = new HashSet<>();
        if (skuExtraProfiles != null && !skuExtraProfiles.isEmpty()) {
            for (Map.Entry<Long, CommenInfo> entry : skuExtraProfiles.entrySet()) {
                Map<String, String> attrs = entry.getValue().getCustomMap();
                if (attrs == null || attrs.isEmpty()) {
                    continue;
                }
                String contentStr = attrs.get("unique_contentid");
                if (contentStr == null || contentStr.isEmpty()) {
                    continue;
                }
                String[] contents = contentStr.split(",");
                Set<String> contentSet = new HashSet<>(contents.length);
                for (String contentStrng : contents) {
                    contentSet.add(contentStrng);
                }
                //如果内容超过50个的话就不需要加进来了
                contentIds.addAll(contentSet);
                if (contentIds.size() > 50) {
                    break;
                }
            }
        }
        return contentIds;
    }


    /**
     * 得到当前的sku,相似的sku,相关的sku，整合在一起
     *
     * @return
     */

    @Nonnull
    @SafeVarargs
    private final Set<String> mergeRelatedAndSimilarSkus(final Set<String> clickedSkus, final Collection<Map<Long, Float>>... args) {
        final Set<String> skus = new HashSet<>();
        skus.addAll(clickedSkus);
        for (final Collection<Map<Long, Float>> maps : args)
            for (final Map<Long, Float> map : maps)
                for (long sku : map.keySet()) {
                    skus.add(String.valueOf(sku));
                }
        return skus;
    }


    /**
     * 将全量contentId来刷新floor或者channel
     *
     * @param poolToContents
     * @param ,              Map<String, Map<Long, Float>> currentContentToPools,
     */
    public void addContentToPool(Map<Long, Set<Long>> poolToContents, Map<String, Map<Long, Float>> currentContentToPools) {
        if (currentContentToPools == null || currentContentToPools.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Map<Long, Float>> entry : currentContentToPools.entrySet()) {
            long recall = Long.valueOf(entry.getKey());
            Map<Long, Float> pools = entry.getValue();
            if (pools != null && !pools.isEmpty()) {
                for (long pool : pools.keySet()) {
                    Set<Long> recalls = poolToContents.get(pool);
                    if (recalls == null) {
                        recalls = new LinkedHashSet<>();
                        recalls.add(recall);
                        poolToContents.put(pool, recalls);
                    } else {
                        recalls.remove(recall);
                        if (recalls.size() >= contentSizeThreshold) {
                            final Iterator<Long> i = recalls.iterator();
                            i.next();
                            i.remove();
                        }
                        recalls.add(recall);
                    }
                }
            }
        }
    }
}
