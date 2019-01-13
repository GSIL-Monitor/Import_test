package com.jd.rec.nl.origin.standalone.mock;

import com.google.inject.Inject;
import com.jd.rec.nl.core.debug.Mock;
import com.jd.rec.nl.service.infrastructure.Zeus;
import p13.recsys.UserProfileAge;
import p13.recsys.UserProfileGender;
import p13.recsys.UserProfilePriceGrade;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/7/23
 */
@Mock(Zeus.class)
public class MockZeus extends Zeus {

    @Inject
    public MockZeus() {
        super();
    }

    @Override
    public Map<String, ByteBuffer> getUserModels(String uid, Collection<String> models) {
        Map<String, ByteBuffer> userModels = new HashMap<>();
        models.forEach(model -> {
            if (model.equals("recsys_p_uid_to_age_lr") || model.equals("recsys_p_uid_to_age_lr_4label")) {
                UserProfileAge.RecsysUserAgeProto.Builder builder = UserProfileAge.RecsysUserAgeProto.newBuilder();
                builder.setUid(uid);
                builder.setAgeId(2);
                userModels.put(model, ByteBuffer.wrap(builder.build().toByteArray()));
            } else if (model.equals("recsys_p_uid_to_gender_lr")) {
                UserProfileGender.RecsysUserGenderProto.Builder builder = UserProfileGender.RecsysUserGenderProto.newBuilder();
                builder.setUid(uid);
                builder.setGender(1);
                userModels.put(model, ByteBuffer.wrap(builder.build().toByteArray()));
            } else if (model.equals("recsys_up_price")) {
                UserProfilePriceGrade.UnifiedUserProfilePriceGradeProto.Builder builder = UserProfilePriceGrade
                        .UnifiedUserProfilePriceGradeProto.newBuilder();
                builder.setUid(uid);
                builder.setUserPrcWeight(0.3);
                UserProfilePriceGrade.Cid3Proper.Builder cid3Proper = UserProfilePriceGrade.Cid3Proper.newBuilder();
                cid3Proper.setProper("0");
                cid3Proper.setWeight(0.15D);
                builder.addCid3PrcWeight(cid3Proper);
                userModels.put(model, ByteBuffer.wrap(builder.build().toByteArray()));
            }
        });
        return userModels;
    }
}
