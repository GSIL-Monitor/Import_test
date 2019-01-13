package com.jd.rec.nl.service.modules.user.service;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jd.zeus.convert.model.ConvertInfo;
import org.junit.Test;
import p13.nearline.NlEntranceSkuExposure;
import p13.recsys.UserProfilePriceGrade;

import java.nio.ByteBuffer;

//import recsys.prediction_service.NlBurstFeature;

/**
 * @author linmx
 * @date 2018/7/20
 */
public class UserServiceTest {

    @Test
    public void testDe() {
        long start = 12345;
        NlEntranceSkuExposure.SkuExposureList.Builder list = NlEntranceSkuExposure.SkuExposureList.newBuilder();
        for (int i = 0; i < 200; i++) {
            long sku = start + i;
            long time = System.currentTimeMillis();
            NlEntranceSkuExposure.Exposure.Builder builder = NlEntranceSkuExposure.Exposure.newBuilder();
            builder.setCount(1);
            //            builder.setTime(time);
            //            builder.setSku(sku);
            list.addExposures(builder);
            //            exposureService.expireExposure(list);
            //            System.out.println(exposureService.listSkuExposure(list.build().toByteArray()));
            //            Thread.sleep(70L);
        }
        byte[] out = list.build().toByteArray();
        NlEntranceSkuExposure.SkuExposureList ret = (NlEntranceSkuExposure.SkuExposureList) UserService.deserialize
                ("nl_entrance_sku_exposure", ByteBuffer.wrap(out));
        System.out.println(ret.getExposuresCount());
        long time = ret.getExposures(0).getTime();
        long sku = ret.getExposures(0).getSku();
    }

    @Test
    public void testModel() {
        //        UserService.colMap.entrySet().stream().forEach(convertInfo -> {
        //            if (convertInfo.getValue().getProtoName().contains("UnifiedUserProfile2layersProto")) {
        //                System.out.println(convertInfo.getKey());
        //            }
        //        });
        ConvertInfo convertInfo = UserService.colMap.get("recsys_p_pin_cheat");
        System.out.println(convertInfo.getProtoName());

        convertInfo = UserService.colMap.get("recsys_up_price");
        System.out.println(convertInfo.getProtoName());

        convertInfo = UserService.colMap.get("recsys_p_uid_to_gender_lr");
        System.out.println(convertInfo.getProtoName());

        convertInfo = UserService.colMap.get("recsys_up_price");
        System.out.println(convertInfo.getProtoName());

        UserProfilePriceGrade.UnifiedUserProfilePriceGradeProto.Builder mainBuilder = UserProfilePriceGrade
                .UnifiedUserProfilePriceGradeProto.newBuilder();
        Descriptors.FieldDescriptor descriptor = UserProfilePriceGrade.UnifiedUserProfilePriceGradeProto
                .getDescriptor().findFieldByName("cid3PrcWeight");
        Object value = null;
        if (descriptor.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
            DescriptorProtos.FieldDescriptorProto.Builder builder = descriptor.toProto().toBuilder();
            Descriptors.FieldDescriptor subDescriptor = descriptor.getMessageType().findFieldByName("proper");
            builder.setField(subDescriptor, "test");
            value = builder.build();
        }
        mainBuilder.setField(descriptor, value);
        UserProfilePriceGrade.UnifiedUserProfilePriceGradeProto proto = mainBuilder.build();

    }

    @Test
    public void terst() throws InvalidProtocolBufferException {
        byte[] value = {10, 16, 8, -34, -34, -120, -62, 37, 16, 1, 24, 19, 32, -69, -127, -112, -37, 5};
      /*  NlBurstFeature.BurstFeatureList builder = NlBurstFeature.BurstFeatureList.parseFrom(value);
        System.out.println(builder.getBurstFeatureInfoCount());*/
    }
}