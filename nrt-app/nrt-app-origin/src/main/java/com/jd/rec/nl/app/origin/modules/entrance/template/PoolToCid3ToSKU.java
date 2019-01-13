package com.jd.rec.nl.app.origin.modules.entrance.template;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.app.origin.modules.entrance.Service.EntranceDBSevice;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.domain.ResultCollection;
import com.jd.rec.nl.service.common.quartet.domain.MapperContext;
import com.jd.rec.nl.service.modules.misc.service.MiscService;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import com.jd.rec.nl.service.modules.user.domain.BehaviorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;


/**
 * 池中对应cid3和sku模板
 * did->cid3->sku
 * tag->cid3->sku
 * @author wanlong3
 * @date 2018/12/1
 */
public abstract class PoolToCid3ToSKU implements Updater {

    private static final Logger LOGGER = LoggerFactory.getLogger("entrance");

    @Inject
    @Named("tableName_rel")
    private String tableNameRel;

    @Inject
    @Named("tableName_sim")
    private String tableNameSim;

    @Inject
    @Named("poolToCid3Size")
    private int poolToCid3SizeThreshold;

    @Inject
    @Named("cid3ToSkuSize")
    private int cid3ToSkuSizeThreshold;

    @Inject
    private EntranceDBSevice entranceDBSevice;

    public abstract String getPoolToCid3ToSkusModelName();

    public abstract String getTableName();

    public abstract Map<Long, List<Integer>> getBlackChannelToCid3s();

    @Inject
    @Named("limit")
    private int limit;

    @Inject
    private MiscService miscService;

    @Override
    public void update(MapperContext mapperContext, ResultCollection resultCollection) {
        String uid = mapperContext.getUid();
        Set<String> clickSkus = new HashSet<>();
        Map<Long, Map<Integer, Set<Long>>> poolToCid3ToSkus = entranceDBSevice.getPoolToCid3ToSkus(uid, getPoolToCid3ToSkusModelName());
        if (poolToCid3ToSkus == null || poolToCid3ToSkus.isEmpty()) {
            poolToCid3ToSkus = new HashMap<>();
            Map<BehaviorField, Set<BehaviorInfo>> behaviorFieldSetMap = mapperContext.getBehaviors();
            Set<BehaviorInfo> BehaviorInfos = behaviorFieldSetMap.get(BehaviorField.CLICK);
            if (BehaviorInfos.size() < 3) {
                return;
            }
            //加入7天的sku
            for (BehaviorInfo behaviorInfo : BehaviorInfos) {
                clickSkus.add(String.valueOf(behaviorInfo.getSku()));
            }
        } else {
            //查询当前的sku
            Set<Long> currentSkus = new HashSet<>(mapperContext.getEventContent().getSkus());
            currentSkus.forEach(sku -> clickSkus.add(String.valueOf(sku)));
        }

        Map<String, Map<Long, Float>> relatedSkus = miscService.getRelatedWithoutNull(clickSkus,
                tableNameRel, 50);
        Map<String, Map<Long, Float>> simSkus = miscService.getRelatedWithoutNull(clickSkus,
                tableNameSim, 12);
        Set<String> skus = mergeRelatedAndSimilarSkus(clickSkus, relatedSkus.values(), simSkus.values());

        Set<Long> orderSkus = getTotalOrderSkus(clickSkus, relatedSkus, simSkus);

        Map<String, Map<Long, Float>> currentContentToPools = miscService.getRelatedWithoutNull(skus, getTableName(), limit);

        refreshPoolToCid3ToSku(currentContentToPools, poolToCid3ToSkus, orderSkus);

        if (!poolToCid3ToSkus.isEmpty()) {
            entranceDBSevice.setPoolToCid3ToSkus(uid, getPoolToCid3ToSkusModelName(), poolToCid3ToSkus);
        }

    }

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
     * 将点击和相似的和相关的整合在一起，得出一个大的skus
     *
     * @param relatedSkus
     * @param similarSkus
     * @param clickSkus
     * @return
     */
    public Set<Long> getTotalOrderSkus(Set<String> clickSkus, Map<String, Map<Long, Float>> relatedSkus, Map<String, Map<Long, Float>> similarSkus) {
        if (relatedSkus == null || relatedSkus.isEmpty()) {
            relatedSkus = new HashMap<>();
        }
        if (similarSkus == null || similarSkus.isEmpty()) {
            similarSkus = new HashMap<>();
        }
        Set<Long> skus = new HashSet<>();
        clickSkus.forEach(sku -> skus.add(Long.valueOf(sku)));
        Set<Long> tobeRecalled = new LinkedHashSet<>();
        for (long sku : skus) {
            //更新点击、相似、相关
            if (tobeRecalled.contains(sku)) {
                tobeRecalled.remove(sku);
            }
            tobeRecalled.add(sku);
            final Map<Long, Float> similar = similarSkus.get(sku);
            if (similar != null && !similar.isEmpty()) {
                tobeRecalled.removeAll(similar.keySet());
                tobeRecalled.addAll(similar.keySet());
            }
            final Map<Long, Float> related = relatedSkus.get(sku);
            if (related != null && !related.isEmpty()) {
                tobeRecalled.removeAll(related.keySet());
                tobeRecalled.addAll(related.keySet());
            }
        }
        return tobeRecalled;
    }

    /**
     * 刷新标签池中的cid3和sku
     *
     * @param currentSkuToPoolAndCid3 使用misc查询(predict)出来的sku对应着的tagpool对应着的cid3
     * @param poolToCid3ToSkus
     * @param tobeRecalled
     */
    public void refreshPoolToCid3ToSku(Map<String, Map<Long, Float>> currentSkuToPoolAndCid3, Map<Long, Map<Integer, Set<Long>>> poolToCid3ToSkus, Set<Long> tobeRecalled) {
        if (currentSkuToPoolAndCid3 == null || currentSkuToPoolAndCid3.isEmpty()) {
            return;
        }
        for (long sku : tobeRecalled) {
            Map<Long, Float> poolAndCid3 = currentSkuToPoolAndCid3.get(sku);
            if (poolAndCid3 != null && !poolAndCid3.isEmpty()) {
                List<Float> cid3List = new ArrayList<>(poolAndCid3.values());
                float cid3F = cid3List.get(0);
                int cid3 = (int) cid3F;
                for (long pool : poolAndCid3.keySet()) {
                    List<Integer> blackCid3s = getBlackChannelToCid3s().get(pool);
                    if (blackCid3s != null && blackCid3s.contains(cid3))
                        continue;
                    Map<Integer, Set<Long>> cid3ToSkus = poolToCid3ToSkus.get(pool);
                    if (cid3ToSkus == null) {
                        Set<Long> skus = new LinkedHashSet<>();
                        skus.add(sku);
                        cid3ToSkus = new LinkedHashMap<>();
                        cid3ToSkus.put(cid3, skus);
                        poolToCid3ToSkus.put(pool, cid3ToSkus);
                    } else {
                        Set<Long> skus = cid3ToSkus.get(cid3);
                        if (skus == null) {
                            skus = new LinkedHashSet<>();
                            skus.add(sku);
                            cid3ToSkus.put(cid3, skus);
                            int cid3Size = cid3ToSkus.size();
                            if (cid3Size > poolToCid3SizeThreshold) {
                                List<Integer> cid3s = new ArrayList<>(cid3ToSkus.keySet());
                                cid3ToSkus.remove(cid3s.get(0));
                            }
                        } else {
                            if (skus.contains(sku)) {
                                skus.remove(sku);
                            } else if (skus.size() >= cid3ToSkuSizeThreshold) {
                                final Iterator<Long> i = skus.iterator();
                                i.next();
                                i.remove();
                            }
                            skus.add(sku);
                        }
                    }
                }
            }
        }
    }

}
