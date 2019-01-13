package com.jd.rec.nl.core.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author linmx
 * @date 2018/8/27
 */
public abstract class KryoUtils {

    public static <T> byte[] serialize(T object) {
        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        kryo.writeClassAndObject(output, object);
        output.flush();
        return byteArrayOutputStream.toByteArray();
    }

    public static <T> T deserialize(byte[] bytes, Class<T> clazz) {
        Kryo kryo = new Kryo();
        Input input = new Input(new ByteArrayInputStream(bytes));
        try {
            return (T) kryo.readClassAndObject(input);
        } catch (Throwable e) {
            kryo = new Kryo();
            input = new Input(bytes);
            return kryo.readObject(input, clazz);
        }
    }

    public static <T> T deserialize(byte[] bytes) {
        Kryo kryo = new Kryo();
        Input input = new Input(new ByteArrayInputStream(bytes));
        return (T) kryo.readClassAndObject(input);
    }

}
