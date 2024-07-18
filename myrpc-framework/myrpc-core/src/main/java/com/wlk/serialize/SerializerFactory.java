package com.wlk.serialize;

import com.wlk.compress.Compressor;
import com.wlk.config.ObjectWrapper;
import com.wlk.serialize.impl.HessianSerializer;
import com.wlk.serialize.impl.JdkSerializer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SerializerFactory {
    private final static ConcurrentHashMap<Byte, ObjectWrapper<Serializer>> SERIALIZER_CACHE = new ConcurrentHashMap<>();
    public final static ConcurrentHashMap<String, Byte> SERIALIZER_CACHE_CODE = new ConcurrentHashMap<>();

    static {
        ObjectWrapper<Serializer> jdk = new ObjectWrapper<>((byte) 1, "jdk", new JdkSerializer());
        ObjectWrapper<Serializer> hessian = new ObjectWrapper<>((byte) 2, "hessian", new HessianSerializer());

        SERIALIZER_CACHE.put((byte) 1, jdk);
        SERIALIZER_CACHE.put((byte) 2, hessian);

        SERIALIZER_CACHE_CODE.put("jdk", (byte) 1);
        SERIALIZER_CACHE_CODE.put("hessian", (byte) 2);
    }

    public static ObjectWrapper<Serializer> getSerializer(String serializeType){
        Byte serializeCode = SERIALIZER_CACHE_CODE.get(serializeType);
        ObjectWrapper<Serializer> serializerWrapper = SERIALIZER_CACHE.get(serializeCode);
        if(serializerWrapper == null){
            log.error("未找到您配置的【{}】序列化工具，默认选用jdk的序列化方式。",serializeType);
            return SERIALIZER_CACHE.get((byte) 1);
        }
        return serializerWrapper;
    }

    public static ObjectWrapper<Serializer> getSerializer(Byte serializeCode){
        ObjectWrapper<Serializer> serializerWrapper = SERIALIZER_CACHE.get(serializeCode);
        if(serializerWrapper == null){
            log.error("未找到您配置的【{}】序列化工具，默认选用jdk的序列化方式。",serializeCode);
            return SERIALIZER_CACHE.get((byte) 1);
        }
        return serializerWrapper;
    }

    /**
     * 给工厂中新增一个序列化方式
     * @param serializerWrapper 压缩类型的包装
     */
    public static void addSerializer(ObjectWrapper<Serializer> serializerWrapper){
        SERIALIZER_CACHE.put(serializerWrapper.getCode(),serializerWrapper);
        SERIALIZER_CACHE_CODE.put(serializerWrapper.getName(),serializerWrapper.getCode());
    }
}
