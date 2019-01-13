package com.jd.rec.nl.toolkit.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jd.rec.nl.core.utils.RuntimeMethodInvoker;
import com.jd.rec.nl.service.infrastructure.Zeus;
import com.jd.zeus.convert.model.ConvertInfo;
import org.slf4j.Logger;
import p13.recsys.UnifiedUserProfile1Layer;
import zeus.UserData;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/10/11
 */
@Singleton
public class ZeusPrint {

    public static final Map<String, ConvertInfo> colMap = ConvertInfo.InitializeUDPMappings().getColumnMap();
    private static final Logger LOGGER = getLogger(ZeusPrint.class);

    @Inject
    private Zeus zeus;

    public void print(String table, String key) throws InvalidProtocolBufferException {
        Map<String, ByteBuffer> value = zeus.getUserModels(key, Collections.singleton(table));
        ByteBuffer profile = value.get(table);
        if (profile == null) {
            LOGGER.info("{} -> {} hasn't value.", table, key);
        }
        ConvertInfo convertInfo = colMap.get(table);
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
                    return;
                }
                field = fieldDescriptorIterator.next();
                fieldName1 = field.getName();
            } while (!fieldName.equals(fieldName1));

            try {
                RuntimeMethodInvoker deserializer = RuntimeMethodInvoker.createInvoker(protoName, "parseFrom", byte[].class);
                Object message = deserializer.invoke(null, profile.array());
                LOGGER.info(message.toString());
            } catch (Exception e) {
                LOGGER.error("userProfile parseFrom method error, return byte[]", e);
            }

        } else {
            UnifiedUserProfile1Layer.UnifiedUserProfile1layerProto proto = UnifiedUserProfile1Layer
                    .UnifiedUserProfile1layerProto.parseFrom(profile.array());
            LOGGER.info(proto.toString());
        }
    }
}
