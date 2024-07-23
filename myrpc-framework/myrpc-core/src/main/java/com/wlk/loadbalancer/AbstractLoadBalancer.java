package com.wlk.loadbalancer;

import com.wlk.MyrpcBootstrap;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractLoadBalancer implements LoadBalancer{

    private Map<String, Selector> cache = new ConcurrentHashMap<>(8);

    @Override
    public InetSocketAddress selectServiceAddress(String serviceName, String group) {
        //1 获得选择器
        Selector selector = cache.get(serviceName);

        //2 如果没有则创建一个
        if (selector == null){
            List<InetSocketAddress> serviceList = MyrpcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry().lookup(serviceName, group);

            selector = getSelector(serviceList);

            cache.put(serviceName, selector);
        }
        return selector.getNext();
    }

    @Override
    public void reLoadBalance(String serviceName, List<InetSocketAddress> addresses) {
        // 我们可以根据新的服务列表生成新的selector
        cache.put(serviceName,getSelector(addresses));
    }

    public abstract Selector getSelector(List<InetSocketAddress> serviceList);
}
