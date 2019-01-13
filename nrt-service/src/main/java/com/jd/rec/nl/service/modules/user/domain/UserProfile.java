package com.jd.rec.nl.service.modules.user.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author linmx
 * @date 2018/7/17
 */
public class UserProfile implements Serializable {

    private Map<String, Object> profiles = new HashMap<>();

    public void addProfile(String model, Object profile) {
        profiles.put(model, profile);
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "profiles=" + profiles +
                '}';
    }

    public Optional getProfile(String profileName) {
        return Optional.ofNullable(profiles.get(profileName));
    }
}
