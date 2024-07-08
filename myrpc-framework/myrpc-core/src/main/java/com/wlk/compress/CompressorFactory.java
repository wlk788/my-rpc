package com.wlk.compress;

import com.wlk.compress.impl.GzipCompressor;
import com.wlk.serialize.SerializerWrapper;
import com.wlk.serialize.impl.HessianSerializer;
import com.wlk.serialize.impl.JdkSerializer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CompressorFactory {

    private final static ConcurrentHashMap<Byte, CompressorWrapper> COMPRESSOR_CACHE = new ConcurrentHashMap<>();
    public final static ConcurrentHashMap<String, Byte> COMPRESSOR_CACHE_CODE = new ConcurrentHashMap<>();

    static {
        CompressorWrapper jdk = new CompressorWrapper((byte) 1, "gzip", new GzipCompressor());

        COMPRESSOR_CACHE.put((byte) 1, jdk);

        COMPRESSOR_CACHE_CODE.put("gzip", (byte) 1);
    }

    public static CompressorWrapper getCompressor(String compressType){
        Byte compressCode = COMPRESSOR_CACHE_CODE.get(compressType);
        CompressorWrapper compressorWrapper = COMPRESSOR_CACHE.get(compressCode);
        if(compressorWrapper == null){
            log.error("未找到您配置的【{}】压缩工具，默认选用gzip的压缩方式。",compressType);
            return COMPRESSOR_CACHE.get((byte) 1);
        }
        return compressorWrapper;
    }

    public static CompressorWrapper  getCompressor(Byte compressCode){
        CompressorWrapper  compressorWrapper = COMPRESSOR_CACHE.get(compressCode);
        if(compressorWrapper == null){
            log.error("未找到您配置的【{}】压缩工具，默认选用gzip的压缩方式。",compressCode);
            return COMPRESSOR_CACHE.get((byte) 1);
        }
        return compressorWrapper;
    }
}
