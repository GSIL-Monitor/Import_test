package com.jd.rec.nl.service.common.quartet.domain;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jd.rec.nl.service.infrastructure.domain.DataSource;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import com.jd.rec.nl.service.modules.user.domain.RequiredBehavior;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/13
 */
public class RequiredDataInfo implements com.jd.rec.nl.core.domain.Named, Serializable {

    private static final Logger LOGGER = getLogger(RequiredDataInfo.class);

    private String name;

    private List<RequiredBehavior> behaviors = new ArrayList<>();

    private Set<String> userProfiles = new HashSet<>();

    private List<RelatedSkuRequired> relatedSkus = new ArrayList();

    private Set<String> sources = new HashSet<>();

    @Inject(optional = true)
    @Named("requiredData.itemProfile")
    private boolean itemProfile;

    private List<ItemExtraProfile> itemExtraProfiles = new ArrayList();

    private List<MiscInfoRequired> miscInfo = new ArrayList();

    @Inject(optional = true)
    public void setBehaviors(@Named("requiredData.behaviors") List behaviors) {
        behaviors.forEach(item -> {
            Map itemMap = (Map) item;
            RequiredBehavior requiredBehavior =
                    new RequiredBehavior(BehaviorField.valueOf((String) itemMap.get("type")),
                            (String) itemMap.get("period"), (Integer) itemMap.get("limit"));
            if (!this.behaviors.contains(requiredBehavior)) {
                this.behaviors.add(requiredBehavior);
            }
        });
    }

    @Inject
    public void setImport(@Named("import") Map sources) {
        sources.forEach((key, value) -> this.sources.add((String) ((Map) value).get("topic")));
    }

    @Inject(optional = true)
    public void setUserProfiles(@Named("requiredData.userProfile") List userProfiles) {
        this.userProfiles.addAll(userProfiles);
    }

    @Inject(optional = true)
    public void setRelatedSkus(@Named("requiredData.related") List relatedSkus) {
        relatedSkus.forEach(item -> {
            Map itemMap = (Map) item;
            RelatedSkuRequired relatedSkuRequired = new RelatedSkuRequired((String) itemMap.get("tableName")
                    , (int) itemMap.get("limit"));
            if (!this.relatedSkus.contains(relatedSkuRequired)) {
                this.relatedSkus.add(relatedSkuRequired);
            }
        });
    }

    @Inject(optional = true)
    public void setItemExtraProfile(@Named("requiredData.itemExtraProfile") List itemExtraProfile) {
        itemExtraProfile.forEach(item -> {
            Map itemMap = (Map) item;
            ItemExtraProfile extraProfile =
                    new ItemExtraProfile((String) itemMap.get("key"), (String) itemMap.get("tableName"));
            if (itemMap.containsKey("source")) {
                extraProfile.setSource(DataSource.valueOf((String) itemMap.get("source")));
            }
            if (!this.itemExtraProfiles.contains(extraProfile)) {
                this.itemExtraProfiles.add(extraProfile);
            }
        });
    }

    @Inject(optional = true)
    public void setMiscInfo(@Named("requiredData.misc") List miscInfo) {
        miscInfo.forEach(item -> {
            Map itemMap = (Map) item;
            MiscInfoRequired miscInfoRequired = new MiscInfoRequired((String) itemMap.get("key"),
                    (String) itemMap.get("tableName"), (int) itemMap.get("limit"));
            if (!this.miscInfo.contains(miscInfoRequired)) {
                this.miscInfo.add(miscInfoRequired);
            }
        });
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public RequiredDataInfo merge(RequiredDataInfo requiredDataInfo) {
        RequiredDataInfo newDataInfo = new RequiredDataInfo();
        newDataInfo.setName(this.getName().concat("-").concat(requiredDataInfo.getName()));
        newDataInfo.behaviors = mergeArray(this.behaviors, requiredDataInfo.behaviors);
        newDataInfo.relatedSkus = mergeArray(this.relatedSkus, requiredDataInfo.relatedSkus);
        newDataInfo.miscInfo = mergeArray(this.miscInfo, requiredDataInfo.miscInfo);
        newDataInfo.itemProfile = this.itemProfile || requiredDataInfo.itemProfile;
        newDataInfo.itemExtraProfiles = mergeArray(this.itemExtraProfiles, requiredDataInfo.itemExtraProfiles);
        newDataInfo.userProfiles.addAll(requiredDataInfo.userProfiles);
        newDataInfo.userProfiles.addAll(this.userProfiles);
        return newDataInfo;
    }

    private <T extends Comparable> List<T> mergeArray(List<T> arrayList1, List<T> arrayList2) {
        List newSet = new ArrayList();
        List<T> compareArray = new ArrayList<>(arrayList2);
        arrayList1.forEach(element -> {
            if (compareArray.contains(element)) {
                int i = compareArray.indexOf(element);
                T element2 = compareArray.get(i);
                newSet.add(element.compareTo(element2) > 0 ? element : element2);
                compareArray.remove(element2);
            } else {
                newSet.add(element);
            }
        });
        newSet.addAll(compareArray);
        return newSet;
    }

    public MapperConf createConfig() {
        MapperConf mapperConf = new MapperConf();
        mapperConf.setRequiredBehaviors(this.behaviors);

        mapperConf.setModelList(this.userProfiles);

        List<PredictorConf> relatedSkus = new ArrayList<>();
        for (RelatedSkuRequired item : this.relatedSkus) {
            PredictorConf relatedConfig = new PredictorConf();
            relatedConfig.setTableName(item.getTableName());
            relatedConfig.setLimit(item.getLimit());
            relatedSkus.add(relatedConfig);
        }
        mapperConf.setRequiredRelatedSkus(relatedSkus);

        mapperConf.setIfGetItemProfile(this.itemProfile);

        List<PredictorConf> requiredExtraItemProfiles = this.itemExtraProfiles.stream().map(itemExtraProfile -> {
            PredictorConf extraProfile = new PredictorConf();
            extraProfile.setKey(itemExtraProfile.getKey());
            extraProfile.setTableName(itemExtraProfile.getTableName());
            extraProfile.setSource(itemExtraProfile.getSource());
            return extraProfile;
        }).collect(Collectors.toList());
        mapperConf.setRequiredExtraItemProfiles(requiredExtraItemProfiles);

        List<PredictorConf> misc = new ArrayList<>();
        for (MiscInfoRequired item : this.miscInfo) {
            PredictorConf miscConfig = new PredictorConf();
            miscConfig.setTableName(item.getTableName());
            miscConfig.setLimit(item.getLimit());
            miscConfig.setKey(item.getKey());
            misc.add(miscConfig);
        }
        mapperConf.setRequiredMiscInfo(misc);

        return mapperConf;
    }

    class RelatedSkuRequired implements Comparable<RelatedSkuRequired> {

        String tableName;

        int limit;

        public RelatedSkuRequired(String tableName, int limit) {

            this.tableName = tableName;
            this.limit = limit;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        @Override
        public int hashCode() {
            return tableName.hashCode();
        }

        @Override
        public int compareTo(RelatedSkuRequired o) {
            return this.limit - o.getLimit();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RelatedSkuRequired)) {
                return false;
            }
            return ((RelatedSkuRequired) obj).getTableName().equals(this.getTableName());
        }


    }

    class MiscInfoRequired implements Comparable<MiscInfoRequired> {
        String key;
        String tableName;
        int limit;

        public MiscInfoRequired(String key, String tableName, int limit) {
            this.key = key;
            this.tableName = tableName;
            this.limit = limit;
        }

        public String getKey() {
            return key;
        }

        public String getTableName() {
            return tableName;
        }

        public int getLimit() {
            return limit;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MiscInfoRequired)) {
                return false;
            }
            return ((MiscInfoRequired) obj).getKey().equals(this.getKey()) &&
                    ((MiscInfoRequired) obj).getTableName().equals
                            (this.getTableName());
        }

        @Override
        public int compareTo(MiscInfoRequired o) {
            return this.getLimit() - o.getLimit();
        }
    }

    class ItemExtraProfile implements Comparable<ItemExtraProfile> {

        String key;

        String tableName;

        private DataSource source = DataSource.userModel;

        public ItemExtraProfile(String key, String tableName) {
            this.tableName = tableName;
            this.key = key;
        }

        public DataSource getSource() {
            return source;
        }

        public void setSource(DataSource source) {
            this.source = source;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }


        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ItemExtraProfile)) {
                return false;
            }
            return ((ItemExtraProfile) obj).source == this.source &&
                    ((ItemExtraProfile) obj).getTableName().equals(this.getTableName()) &&
                    ((ItemExtraProfile) obj).getKey()
                            .equals(this.getKey());
        }

        @Override
        public int hashCode() {
            return source.hashCode() + tableName.hashCode() + key.hashCode();
        }

        @Override
        public int compareTo(ItemExtraProfile o) {
            return tableName.compareTo(o.getTableName());
        }
    }

}
