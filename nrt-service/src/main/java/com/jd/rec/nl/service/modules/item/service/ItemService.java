package com.jd.rec.nl.service.modules.item.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jd.rec.nl.core.cache.annotation.CacheKeys;
import com.jd.rec.nl.core.cache.annotation.CacheName;
import com.jd.rec.nl.core.exception.EnvironmentException;
import com.jd.rec.nl.service.infrastructure.Clerk;
import com.jd.rec.nl.service.infrastructure.Selector;
import com.jd.rec.nl.service.infrastructure.UserModel;
import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import com.jd.si.venus.core.CommenInfo;
import com.jd.si.venus.core.SkuAttributInfo;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class ItemService {

    private static final Logger LOGGER = getLogger(ItemService.class);

    @Inject
    private Clerk clerk;

    @Inject
    private UserModel userModel;

    @Inject
    private Selector selector;

    @CacheResult(cacheName = "itemProfile")
    public ItemProfile getProfile(@CacheKey Long sku) throws Exception {
        Map<Long, ItemProfile> profiles = clerk.getProfiles(Collections.singleton(sku));
        if (profiles == null || profiles.isEmpty()) {
            return null;
        }
        return profiles.get(sku);
    }

    public Map<Long, ItemProfile> getProfilesWithoutNull(final Collection<Long> skus) throws Exception {
        try {
            Map<Long, ItemProfile> itemProfiles = this.getProfiles(skus);
            for (long sku : skus) {
                if (itemProfiles.containsKey(sku) && itemProfiles.get(sku) == null) {
                    itemProfiles.remove(sku);
                }
            }
            return itemProfiles;
        } catch (EnvironmentException e) {
            LOGGER.debug(e.getMessage());
            return new HashMap<>();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return new HashMap<>();
        }
    }

    @CacheResult(cacheName = "itemProfile")
    public Map<Long, ItemProfile> getProfiles(@CacheKeys Collection<Long> skus) throws Exception {
        if (skus == null || skus.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ItemProfile> itemProfiles = clerk.getProfiles(skus);
        if (MapUtils.isNotEmpty(itemProfiles)) {
            Map<Long, SkuAttributInfo> userModelResponse = userModel.getUserModelResponse(skus);
            if (MapUtils.isNotEmpty(userModelResponse)) {
                userModelResponse.forEach((k, v) -> {
                    ItemProfile itemProfile = itemProfiles.get(k);
                    if (Objects.nonNull(itemProfile)) {
                        itemProfile.setExtendAttributes(v.getExtendAttributes());
                    }
                });
            }
        }
        return itemProfiles;
    }

    public Map<Long, CommenInfo> getItemExtraProfiles(Collection<String> keys, final String table) {
        try {
            return getFromUserModel(keys, table);
        } catch (EnvironmentException e) {
            LOGGER.debug(e.getMessage());
            return new HashMap<>();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return new HashMap<>();
        }
    }

    @CacheResult(cacheName = "userModel")
    public Map<Long, CommenInfo> getFromUserModel(@CacheKeys final Collection<String> keys,
                                                  @CacheName final String table) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, CommenInfo> itemProfiles =
                userModel.getUserModelResponse(keys.stream()
                        .map(key -> Long.parseLong(key)).collect(Collectors.toSet()), table);
        if (itemProfiles == null) {
            itemProfiles = Collections.emptyMap();
        }
        return itemProfiles;
    }

    @CacheResult(cacheName = "userModel")
    public Map<String, String> getFromSelector(@CacheKeys Collection<String> keys, @CacheName String table) {
        Map<String, String> materials = selector.getData(keys, table);
        if (materials == null) {
            materials = Collections.emptyMap();
        }
        return materials;
    }


}
