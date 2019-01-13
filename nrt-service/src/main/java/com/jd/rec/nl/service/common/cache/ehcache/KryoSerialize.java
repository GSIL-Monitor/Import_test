package com.jd.rec.nl.service.common.cache.ehcache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.jd.rec.nl.core.utils.KryoUtils;
import org.ehcache.impl.internal.util.ByteBufferInputStream;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

import java.nio.ByteBuffer;

/**
 * @author wl
 * @date 2018/9/14
 */
public class KryoSerialize<Object> implements Serializer<Object> {
    @Override
    public ByteBuffer serialize(Object object) throws SerializerException {
        if (object == null) {
            return ByteBuffer.wrap(new byte[0]);
        }
        return ByteBuffer.wrap(KryoUtils.serialize(object));
    }

    @Override
    public Object read(ByteBuffer binary) throws ClassNotFoundException, SerializerException {
        ByteBufferInputStream bin = new ByteBufferInputStream(binary);
        Kryo kryo = new Kryo();
        Input input = new Input(bin);
        return (Object) kryo.readClassAndObject(input);
    }

    @Override
    public boolean equals(Object object, ByteBuffer binary) throws ClassNotFoundException, SerializerException {
        return object.equals(read(binary));
    }
}
