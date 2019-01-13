package com.jd.rec.nl.service.modules.user.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Descriptors;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.core.guice.annotation.ENV;
import com.jd.rec.nl.core.utils.RuntimeMethodInvoker;
import com.jd.rec.nl.service.infrastructure.Zeus;
import com.jd.rec.nl.service.modules.user.domain.BehaviorField;
import com.jd.rec.nl.service.modules.user.domain.BehaviorInfo;
import com.jd.rec.nl.service.modules.user.domain.RequiredBehavior;
import com.jd.rec.nl.service.modules.user.domain.UserProfile;
import com.jd.zeus.convert.model.ConvertInfo;
import org.slf4j.Logger;
import zeus.UserData;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;
import java.nio.ByteBuffer;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class UserService {

    public static final Map<String, ConvertInfo> colMap = ConvertInfo.InitializeUDPMappings().getColumnMap();
    public static final String behaviorCacheName = "userBehavior";
    public static final String userProfileCacheName = "userProfile";
    private static final Logger LOGGER = getLogger(UserService.class);
    private static final String userBehaviorCachePrefix = "behavior_";

    @Inject
    @ENV("userBehavior.behavior_maxSize")
    private int BEHAVIOR_MAX_SIZE;

    @Inject
    private com.jd.rec.nl.service.infrastructure.Behavior userBehavior;

    @Inject
    private Zeus zeus;

    public static Object deserialize(String model, ByteBuffer modelByte) {
        ConvertInfo convertInfo = colMap.get(model);
        if (convertInfo != null) {
            String fieldName = convertInfo.getFieldName();
            String protoName = convertInfo.getProtoName();
            List<Descriptors.FieldDescriptor> obj = UserData.UserDataProto.getDescriptor()
                    .getFields();
            Iterator<Descriptors.FieldDescriptor> fieldDescriptorIterator = obj.iterator();
            Descriptors.FieldDescriptor field;
            String fieldName1;
            do {
                if (!fieldDescriptorIterator.hasNext()) {
                    return null;
                }
                field = fieldDescriptorIterator.next();
                fieldName1 = field.getName();
            } while (!fieldName.equals(fieldName1));

            try {
                RuntimeMethodInvoker deserializer =
                        RuntimeMethodInvoker.createInvoker(protoName, "parseFrom", byte[].class);
                Object message = deserializer.invoke(null, modelByte.array());
                return message;
            } catch (Exception e) {
                LOGGER.error("userProfile parseFrom method error, return byte[]", e);
                return modelByte;
            }

        } else {
            LOGGER.debug(String.format("model \"%s\" is not exists, return byte code for custom deserialize.", model));
            return modelByte;
        }
    }

    /**
     * 获取并刷新用户的行为记录
     *
     * @param uid            用户uid
     * @param skus           当前行为的sku set
     * @param timestamp      当前行为的时间戳
     * @param thisBehavior   当前的行为类型
     * @param queryBehaviors 需要查询的行为类型 key:行为类型 value:时间跨度(毫秒)
     * @return
     */
    public Map<BehaviorField, Set<BehaviorInfo>> refreshAndQueryBehavior(String uid, Set<Long> skus, long timestamp,
                                                                         BehaviorField
                                                                                 thisBehavior,
                                                                         List<RequiredBehavior> queryBehaviors) {
        try {
            // 缓存使用的key
            String key = userBehaviorCachePrefix.concat(uid);
            // 获取历史记录
            Map<BehaviorField, Set<BehaviorInfo>> behaviors =
                    new HashMap<>(this.getClickedSkuSet(uid, key, queryBehaviors));
            for (long sku : skus) {
                BehaviorInfo thisBehaviorInfo = new BehaviorInfo(sku, timestamp, 1);
                // 更新行为记录
                if (behaviors.containsKey(thisBehavior)) {
                    Set<BehaviorInfo> behaviorsHistory = behaviors.get(thisBehavior);
                    for (BehaviorInfo behaviorInfo : behaviorsHistory) {
                        if (behaviorInfo.getSku() == sku) {
                            thisBehaviorInfo.setCount(behaviorInfo.getCount() + 1);
                            behaviorsHistory.remove(behaviorInfo);
                            break;
                        }
                    }
                    behaviorsHistory.add(thisBehaviorInfo);
                    if (behaviorsHistory.size() > this.BEHAVIOR_MAX_SIZE) {
                        // 超过容量大小则删除第一个
                        Iterator i = behaviorsHistory.iterator();
                        i.next();
                        i.remove();
                    }
                } else {
                    Set<BehaviorInfo> thisBehaviorSet = new LinkedHashSet<>();
                    thisBehaviorSet.add(thisBehaviorInfo);
                    behaviors.put(thisBehavior, thisBehaviorSet);
                }
            }
            // 更新缓存操作
            this.updateCachedClickedSkuSet(key, behaviors);
            return behaviors;
        } catch (EnvironmentException e) {
            LOGGER.debug(e.getMessage());
            return new HashMap<>();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return new HashMap<>();
        }
    }

    @CacheResult(cacheName = behaviorCacheName)
    public Map<BehaviorField, Set<BehaviorInfo>> getClickedSkuSet(String uid, @CacheKey String key,
                                                                  List<RequiredBehavior> queryBehaviors) {
        Map<String, Set<BehaviorInfo>> behaviors = userBehavior.getBehaviors(uid, queryBehaviors);
        if (behaviors == null || behaviors.size() == 0) {
            LOGGER.info("No click-behaviors for " + uid);
            return Collections.emptyMap();
        }

        Map<BehaviorField, Set<BehaviorInfo>> multiBehaviors = new HashMap<>();
        final long now = new Date().getTime();
        for (RequiredBehavior required : queryBehaviors) {
            BehaviorField behaviorField = required.getType();
            Set<BehaviorInfo> userBehaviors = behaviors.get(behaviorField.getFieldName());
            // 先控制大小
            while (userBehaviors.size() > this.BEHAVIOR_MAX_SIZE) {
                Iterator i = userBehaviors.iterator();
                i.next();
                i.remove();
            }
            // 再控制时间
            //            Long behaviorStart = now - required.getLimit();
            //            final Set<BehaviorInfo> skus = new LinkedHashSet<>();
            //            for (BehaviorInfo userBehavior : userBehaviors) {
            //                if (userBehavior.getTimestamp() > behaviorStart)
            //                    skus.add(userBehavior);
            //            }
            multiBehaviors.put(behaviorField, userBehaviors);
        }
        return multiBehaviors;
    }

    @CachePut(cacheName = behaviorCacheName)
    public void updateCachedClickedSkuSet(@CacheKey String key,
                                          @CacheValue Map<BehaviorField, Set<BehaviorInfo>> behaviors) {
        // 只是作为触发cacheput操作,没有实际处理
    }

    /**
     * 根据模型列表查询用户画像
     *
     * @param uid       uid(设备的)
     * @param modelList 模型列表
     * @return
     */
    public UserProfile getUserProfiles(String uid, Set<String> modelList) {
        UserProfile userProfile = new UserProfile();
        // 构造key,用于缓存
        try {
            Map<String, Object> userModels = queryUserProfiles(uid, modelList);
            userModels.forEach((modelName, modelValue) -> userProfile.addProfile(modelName, modelValue));
        } catch (EnvironmentException e) {
            LOGGER.debug(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return userProfile;
    }

    @CacheResult(cacheName = userProfileCacheName)
    public Map<String, Object> queryUserProfiles(@CacheKey String uid, Set<String> modelList) {
        // 调用zeus查询
        Map<String, ByteBuffer> userModels = zeus.getUserModels(uid, modelList);
        Map<String, Object> cachedModels = new HashMap<>();
        // 进行反序列化并返回
        userModels.forEach((modelName, modelByte) -> cachedModels.put(modelName, deserialize(modelName, modelByte)));
        return cachedModels;
    }
}
