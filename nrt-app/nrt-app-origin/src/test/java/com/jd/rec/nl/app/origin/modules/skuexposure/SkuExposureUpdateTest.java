package com.jd.rec.nl.app.origin.modules.skuexposure;

import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.service.common.quartet.ApplicationLoader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.junit.Test;
import recsys.prediction_service.BurstFeature;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author linmx
 * @date 2018/6/19
 */
public class SkuExposureUpdateTest {

    @Test
    public void test() throws InvocationTargetException, IllegalAccessException {
        ApplicationLoader loader = new ApplicationLoader();
        loader.load();
        System.out.println(loader.getSourceInfo().getConfigs().toString());
    }

    @Test
    public void testConfig() {
        Config config = ConfigBase.getAppConfig("skuExposureAccumulate").getConfig("params");
        SkuExposureUpdater update = (SkuExposureUpdater) bindParameters(SkuExposureUpdater.class, config);
    }

    private Named bindParameters(Class<? extends Named> clazz, Config config) {
        Injector injector = InjectorService.getCommonInjector().createChildInjector(binder -> {
            if (config != null) {
                for (Map.Entry<String, ConfigValue> configValueEntry : config.entrySet()) {
                    Object value = configValueEntry.getValue();
                    LinkedBindingBuilder builder = binder.bind(TypeLiteral.get(value.getClass()))
                            .annotatedWith(Names.named(configValueEntry.getKey()));
                    builder.toInstance(value);
                }
                //                binder.convertToTypes(Matchers.any().and(Matchers.annotatedWith(com.google.inject.name.Named
                // .class)), new
                //                        TypeConverter() {
                //                    @Override
                //                    public Object convert(String value, TypeLiteral<?> toType) {
                //                        return null;
                //                    }
                //                });
            }
        });
        Named obj = injector.getInstance(clazz);
        // just in time binging 需要再inject一次时才能生效
        injector.injectMembers(obj);
        return obj;
    }

    @Test
    public void test1() {
        double a = 0.001;
        int b = new Double(a * 10000).intValue();
        System.out.println(b);
        BurstFeature.BurstFeatureInfo.Builder builder = BurstFeature.BurstFeatureInfo.newBuilder();
        builder.setTime(System.currentTimeMillis() / 1000);
        builder.setHc(new Double(a * 10000).intValue());
        builder.setUv(1);
        builder.setItemId(1111L);
        System.out.println(builder.build());
    }

//    @Test
//    public void testSerialize() {
//        ExposureAccumulator accumulator = new ExposureAccumulator();
//        accumulator.setSku(1);
//        Kryo kryo = new Kryo();
//        Output output = new Output(new ByteBufferOutputStream());
//        kryo.writeObject(output, accumulator);
//        byte[] value = output.toBytes();
//        Input input = new Input(value);
//        ExposureAccumulator i = kryo.readObject(input, ExposureAccumulator.class);
//        System.out.println(i.getSku());
//    }
//
//    @Test
//    public void testDeserialize() {
//        Kryo kryo = new Kryo();
//        byte[] result1 = {20};
//        Input input1 = new Input(result1);
//        int i = kryo.readObject(input1, Integer.class);
//        System.out.println(i);
//
//        byte[] result = {1, 0, 0, 2};
//        Input input = new Input(result);
//        ExposureAccumulator i1 = kryo.readObject(input, ExposureAccumulator.class);
//        System.out.println(i1.getSku());
//    }

    @Test
    public void testSet(){
        Set<Long> set = new HashSet<Long>();
        set.add(123456L);
        set.add(645646654L);
        set.add(6546456L);
        set.add(785687867867L);
        Set<Long> set1 = new HashSet<>(set);
        System.out.println("111111111:"+set1);


    }

}