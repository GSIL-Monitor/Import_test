package com.jd.rec.nl.app.origin.common.filter;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import com.jd.rec.nl.service.common.quartet.filter.MethodFilter;
import com.jd.rec.nl.service.common.quartet.processor.MapProcessor;
import com.jd.rec.nl.service.modules.user.domain.UserProfile;
import com.jd.rec.nl.service.modules.user.service.UserService;
import com.jd.ump.profiler.proxy.Profiler;
import org.slf4j.Logger;
import p13.recsys.UnifiedUserProfile1Layer;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/10/11
 */
public class CheatUserFilter implements MethodFilter {

    public static final String cheatTable = "recsys_p_pin_cheat";
    private static final Logger LOGGER = getLogger(CheatUserFilter.class);
    @Inject
    private UserService userService;
    private Method method;

    public CheatUserFilter() throws NoSuchMethodException {
        this.method = MapProcessor.class.getMethod("map", ImporterContext.class);
    }

    @Override
    public Method filterMethod() {
        return method;
    }

    @Override
    public FilterType filterType() {
        return FilterType.pre;
    }

    @Override
    public String invalidMessage() {
        return "cheat user filter!";
    }

    @Override
    public boolean preFilter(Object... args) {
        ImporterContext context = (ImporterContext) args[0];
        String pin = context.getPin();
        LOGGER.debug("start filter cheater:{}", pin);
        String uid = context.getUid();
        if (uid != null && !uid.isEmpty() && ConfigBase.getSystemConfig().getStringList("trace.uid").contains(uid)) {
            return true;
        }
        if (pin != null && !pin.isEmpty()) {
            UserProfile profile = userService.getUserProfiles(pin, Collections.singleton(cheatTable));
            LOGGER.debug("filter info:{}", profile);
            if (profile.getProfile(cheatTable).isPresent()) {
                UnifiedUserProfile1Layer.UnifiedUserProfile1layerProto proto;
                try {
                    proto = UnifiedUserProfile1Layer
                            .UnifiedUserProfile1layerProto
                            .parseFrom(((ByteBuffer) profile.getProfile(cheatTable).get()).array());
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("cheat info:{}", proto.toString());
                    }
                    if (Objects.isNull(proto) || "0".equals(proto.getValue())) {
                        return true;
                    }
                } catch (InvalidProtocolBufferException e) {
                    return true;
                }
                Profiler.countAccumulate("NRT_CHEATER_FILTER");
                LOGGER.debug("{} is a cheater!", pin);
                return false;
            }
        }
        return true;
    }
}
