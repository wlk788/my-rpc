package com.wlk.loadbalancer;

import java.net.InetSocketAddress;

public interface LoadBalancer {

    /**
     * 根据服务名获取一个可用的服务
     * @param serviceName 服务名称
     * @return 服务地址
     */
    InetSocketAddress selectServiceAddress(String serviceName);
}
