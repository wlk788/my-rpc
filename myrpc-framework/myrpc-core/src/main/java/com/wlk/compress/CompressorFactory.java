package com.wlk.compress;

import com.wlk.compress.impl.GzipCompressor;
import com.wlk.config.ObjectWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CompressorFactory {

    private final static ConcurrentHashMap<Byte, ObjectWrapper<Compressor>> COMPRESSOR_CACHE = new ConcurrentHashMap<>();
    public final static ConcurrentHashMap<String, Byte> COMPRESSOR_CACHE_CODE = new ConcurrentHashMap<>();

    static {
        ObjectWrapper<Compressor> gzip = new ObjectWrapper<>((byte) 1, "gzip", new GzipCompressor());

        COMPRESSOR_CACHE.put((byte) 1, gzip);

        COMPRESSOR_CACHE_CODE.put("gzip", (byte) 1);
    }

    public static ObjectWrapper<Compressor> getCompressor(String compressType){
        Byte compressCode = COMPRESSOR_CACHE_CODE.get(compressType);
        ObjectWrapper<Compressor> compressorWrapper = COMPRESSOR_CACHE.get(compressCode);
        if(compressorWrapper == null){
            log.error("未找到您配置的【{}】压缩工具，默认选用gzip的压缩方式。",compressType);
            return COMPRESSOR_CACHE.get((byte) 1);
        }
        return compressorWrapper;
    }

    public static ObjectWrapper<Compressor>  getCompressor(Byte compressCode){
        ObjectWrapper<Compressor>  compressorWrapper = COMPRESSOR_CACHE.get(compressCode);
        if(compressorWrapper == null){
            log.error("未找到您配置的【{}】压缩工具，默认选用gzip的压缩方式。",compressCode);
            return COMPRESSOR_CACHE.get((byte) 1);
        }
        return compressorWrapper;
    }

    /**
     * 给工厂中新增一个压缩方式
     * @param compressorWrapper 压缩类型的包装
     */
    public static void addCompressor(ObjectWrapper<Compressor> compressorWrapper){
        COMPRESSOR_CACHE.put(compressorWrapper.getCode(),compressorWrapper);
        COMPRESSOR_CACHE_CODE.put(compressorWrapper.getName(),compressorWrapper.getCode());
    }
}
