package com.jd.rec.nl.service.common;

import com.jd.rec.nl.core.utils.KryoUtils;
import com.jd.rec.nl.service.modules.user.domain.BehaviorInfo;
import io.grpc.Codec.Gzip;
import org.junit.Test;
import org.xerial.snappy.Snappy;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class KryoUtilsTest {

    @Test
    public void testCompress() throws IOException {
        List<BehaviorInfo> infos = new ArrayList<>();
        int i = 0;
        while (i++ < 50) {
            BehaviorInfo behaviorInfo = new BehaviorInfo(i, i, i);
            infos.add(behaviorInfo);
        }
        byte[] value = KryoUtils.serialize(infos);
        System.out.println(value.length);
        byte[] compressed = Snappy.compress(value);
        System.out.println(compressed.length);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Gzip gzip = new Gzip();
        OutputStream compress = gzip.compress(outputStream);
        compress.write(value);
        compress.flush();
        compressed = outputStream.toByteArray();
        System.out.println(compressed.length);

        ByteArrayInputStream uncompress = new ByteArrayInputStream(compressed);
        InputStream inputStream = gzip.decompress(uncompress);
        value = new byte[inputStream.available()];
        inputStream.read(value);
        infos = KryoUtils.deserialize(value);
        System.out.println(infos.size());
    }

}