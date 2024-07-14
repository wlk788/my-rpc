package com.wlk.loadbalancer;

import com.wlk.MyrpcBootstrap;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractLoadBalancer implements LoadBalancer{

    private Map<String, Selector> cache = new ConcurrentHashMap<>(8);

    @Override
    public InetSocketAddress selectServiceAddress(String serviceName) {
        //1 获得选择器
        Selector selector = cache.get(serviceName);

        //2 如果没有则创建一个
        if (selector == null){
            List<InetSocketAddress> serviceList = MyrpcBootstrap.getInstance().getRegistry().lookup(serviceName);

            selector = getSelector(serviceList);

            cache.put(serviceName, selector);
        }
        return selector.getNext();
    }

    public abstract Selector getSelector(List<InetSocketAddress> serviceList);
}
