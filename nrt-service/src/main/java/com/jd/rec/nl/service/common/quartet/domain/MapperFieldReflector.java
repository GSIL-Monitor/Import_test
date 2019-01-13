package com.jd.rec.nl.service.common.quartet.domain;

import com.jd.rec.nl.service.modules.item.domain.ItemProfile;
import javassist.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 使用静态代理的方式反射获取MapperContext中的数据字段,目前仅支持获取用户画像和指定商品的商品画像
 *
 * @author linmx
 * @date 2018/7/20
 */
public abstract class MapperFieldReflector {

    private static volatile Map<String, MapperFieldReflector> cached = new HashMap<>();

    /**
     * 生成反射器类
     *
     * @param fieldReg field参数表达式
     *                 用户画像: user:${field name}
     *                 商品画像: item:${field name}
     * @return
     */
    public static MapperFieldReflector create(String fieldReg) throws Exception {
        if (cached.containsKey(fieldReg) && cached.get(fieldReg) != null) {
            return cached.get(fieldReg);
        }
        ClassPool pool = ClassPool.getDefault();
        CtClass proxy = pool.makeClass(MapperContext.class.getName().concat(".proxy.").concat(fieldReg.replaceAll(":", "_")));
        CtClass supperClass = pool.getCtClass(MapperFieldReflector.class.getName());
        proxy.setSuperclass(supperClass);

        CtMethod superMethod = proxy.getMethod("get", supperClass.getDeclaredMethod("get").getSignature());
        CtMethod method = CtNewMethod.copy(superMethod, proxy, null);

        String[] field = fieldReg.split(":");
        Type type = Type.valueOf(field[0]);
        String name = field[1];

        StringBuilder body = new StringBuilder("{ ");
        if (type == Type.user) {
            body.append("return $1.getUserProfile().getProfile(\"").append(name).append("\");");
        } else if (type == Type.item) {
            name = "get".concat(name.substring(0, 1).toUpperCase().concat(name.substring(1)));
            Method rawMethod = ItemProfile.class.getMethod(name);
            Class retType = rawMethod.getReturnType();

            body.append(retType.getTypeName()).append(" ret = ((com.jd.rec.nl.service.modules.item.domain.ItemProfile)" +
                    "$1.getSkuProfiles().get($2)).").append(name).append("();\n");
            if (retType.isPrimitive()) {
                if (retType == int.class) {
                    body.append("return new Integer(ret);\n");
                } else if (retType == long.class) {
                    body.append("return new Long(ret);\n");
                } else if (retType == short.class) {
                    body.append("return new Short(ret);\n");
                } else if (retType == byte.class) {
                    body.append("return new Byte(ret);\n");
                } else if (retType == double.class) {
                    body.append("return new Double(ret);\n");
                } else if (retType == float.class) {
                    body.append("return new Float(ret);\n");
                } else if (retType == boolean.class) {
                    body.append("return new Boolean(ret);\n");
                }
            } else {
                body.append("return ret;");
            }
            //            body.append(boxPrimitiveValue());
        } else {
            throw new UnsupportedOperationException("only support get user/item profile");
        }

        body.append("}");
        //        StringBuilder body = new StringBuilder("{int a = 3;  System.out.println(a);\n return a;}");


        method.setBody(body.toString());
        method.setModifiers(superMethod.getModifiers() & ~Modifier.ABSTRACT);
        proxy.addMethod(method);

        proxy.setModifiers(proxy.getModifiers() & ~Modifier.ABSTRACT);

        synchronized (cached) {
            if (cached.containsKey(fieldReg)) {
                return cached.get(fieldReg);
            }
            MapperFieldReflector invoker = (MapperFieldReflector) proxy.toClass().newInstance();
            cached.put(fieldReg, invoker);
            return invoker;
        }
    }

    private static String boxPrimitiveValue() {
        StringBuilder body = new StringBuilder("if (ret.getClass().isPrimitive()){\n");
        body.append("if(ret.getClass() == int.class){\n return new Integer(String.valueOf(ret));\n}")
                .append("else if(ret.getClass() == long.class){\n return new Long(String.valueOf(ret));\n}")
                .append("else if(ret.getClass() == short.class){\n return new Short(String.valueOf(ret));\n}")
                .append("else if(ret.getClass() == byte.class){\n return new Byte(String.valueOf(ret));\n}")
                .append("else if(ret.getClass() == double.class){\n return new Double(String.valueOf(ret));\n}")
                .append("else if(ret.getClass() == float.class){\n return new Float(String.valueOf(ret));\n}")
                .append("else if(ret.getClass() == boolean.class){\n return new Boolean(String.valueOf(ret));\n}")
                .append("else{\nthrow new com.jd.rec.nl.core.exception.WrongConfigException(\"not a primitive class\");\n}\n");
        body.append("}else{\nreturn ret;\n}\n");
        return body.toString();
    }

    /**
     * 获取对应的值
     *
     * @param context 当前的mapper context
     * @param sku     需要查询的sku,如果是查用户画像,此字段可送个null
     * @return
     */
    public abstract Object get(MapperContext context, Long sku);

    private enum Type {
        user, item
    }
}
