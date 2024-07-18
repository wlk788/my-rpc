package com.wlk.config;

import com.wlk.compress.Compressor;
import com.wlk.compress.CompressorFactory;
import com.wlk.loadbalancer.LoadBalancer;
import com.wlk.serialize.Serializer;
import com.wlk.serialize.SerializerFactory;
import com.wlk.spi.SpiHandler;

import java.util.List;

public class SpiResolver {
    public void loadFromSpi(Configuration configuration){
        // 我的spi的文件中配置了很多实现（自由定义，只能配置一个实现，还是多个）
        List<ObjectWrapper<LoadBalancer>> loadBalancerWrappers = SpiHandler.getList(LoadBalancer.class);
        // 将其放入工厂
        if(loadBalancerWrappers != null && loadBalancerWrappers.size() > 0){
            configuration.setLoadBalancer(loadBalancerWrappers.get(0).getImpl());
        }

        List<ObjectWrapper<Compressor>> objectWrappers = SpiHandler.getList(Compressor.class);
        if(objectWrappers != null){
            objectWrappers.forEach(CompressorFactory::addCompressor);
        }

        List<ObjectWrapper<Serializer>> serializerObjectWrappers = SpiHandler.getList(Serializer.class);
        if (serializerObjectWrappers != null){
            serializerObjectWrappers.forEach(SerializerFactory::addSerializer);
        }
    }

    public static void main(String[] args) {

    }
}
