package com.jd.rec.nl.app.origin.modules.promotionalburst.domain;

import javassist.*;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

/**
 * @author linmx
 * @date 2018/7/17
 */
public class SegmentationConfigTest {

    @Test
    public void testReflect() throws Throwable {
        final MethodHandles.Lookup original = MethodHandles.lookup();
        final Field internal = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        internal.setAccessible(true);
        final MethodHandles.Lookup trusted = (MethodHandles.Lookup) internal.get(original);
        final MethodHandles.Lookup caller = trusted.in(SegmentationConfig.class);
        Method method = SegmentationConfig.class.getMethod("getModelId");
        method.setAccessible(false);
        MethodHandle methodHandle = caller.unreflect(method);

        SegmentationConfig segmentationConfig = new SegmentationConfig();
        long start = System.currentTimeMillis();
        int i = 0;
        while (i < 1000000000) {
            int modelId = (int) methodHandle.invoke(segmentationConfig);
            i++;
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);

        i = 0;
        start = System.currentTimeMillis();
        while (i < 1000000000) {
            i++;
        }
        end = System.currentTimeMillis();
        System.out.println(end - start);

        //        System.out.println(modelId);
    }

    public void testASM() {
        ClassWriter cw = new ClassWriter(0);
        String[] interfaces = {TestGet.class.getName()};
        cw.visit(V1_1, ACC_PUBLIC, "Example", "java/lang/Integer", "java/lang/Object", interfaces);

        //生成默认的构造方法
        MethodVisitor mw = cw.visitMethod(ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null);

        //生成构造方法的字节码指令
        mw.visitVarInsn(ALOAD, 0);
        mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mw.visitInsn(RETURN);
        mw.visitMaxs(1, 1);
        mw.visitEnd();


        mw = cw.visitMethod(ACC_PUBLIC,
                "getValue",
                "()java/lang/Integer",
                "java/lang/Integer",
                null);
        //
        //        //生成main方法中的字节码指令
        //        mw.visitFieldInsn(GETSTATIC,
        //                "java/lang/System",
        //                "out",
        //                "Ljava/io/PrintStream;");
        //
        //        mw.visitLdcInsn("Hello world!");
        //        mw.visitMethodInsn(INVOKEVIRTUAL,
        //                "java/io/PrintStream",
        //                "println",
        //                "(Ljava/lang/String;)V");
        //        mw.visitInsn(RETURN);
        //        mw.visitMaxs(2, 2);
        //        mw.visitEnd();
    }

    @Test
    public void testJavasisst() throws NotFoundException, CannotCompileException, IllegalAccessException, InstantiationException {

        SegmentationConfig segmentationConfig = new SegmentationConfig();

        long start = System.currentTimeMillis();
        int i = 0;
        while (i < 1000000000) {
            i++;
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);


        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass("com.jd.rec.nl.app.userprofile.modules.promotionalburst.domain.Example");
        CtClass interfaceClass = pool.getCtClass(TestGet.class.getName());
        CtClass[] interfaces = new CtClass[]{interfaceClass};
        ctClass.setInterfaces(interfaces);


        CtField field = new CtField(pool.get(SegmentationConfig.class.getName()), "config", ctClass);
        field.setModifiers(Modifier.PRIVATE);
        ctClass.addField(field);


        CtMethod superGet = ctClass.getMethod("getValue", interfaceClass.getDeclaredMethod("getValue").getSignature());
        CtMethod get = CtNewMethod.copy(superGet, ctClass, null);
        get.setBody("{return Integer.valueOf($0.config.getModelId());}");
        ctClass.addMethod(get);

        CtMethod superSet = ctClass.getMethod("setParam", interfaceClass.getDeclaredMethod("setParam").getSignature());
        CtMethod set = CtNewMethod.copy(superSet, ctClass, null);

        set.setBody("{$0.config = (com.jd.rec.nl.app.userprofile.modules.promotionalburst.domain.SegmentationConfig) $args[0];}");
        ctClass.addMethod(set);

        ctClass.setModifiers(ctClass.getModifiers() & ~Modifier.ABSTRACT);

        TestGet testGet = (TestGet) ctClass.toClass().newInstance();
        testGet.setParam(segmentationConfig);

        //        System.out.println("test:" + testGet.getValue());
        //        System.out.println("actual:" + segmentationConfig.getModelId());


        start = System.currentTimeMillis();
        i = 0;
        while (i < 1000000000) {
            testGet.getValue();
            i++;
        }
        end = System.currentTimeMillis();
        System.out.println(end - start);

    }

    interface TestGet {

        void setParam(Object param);

        Object getValue();
    }

}