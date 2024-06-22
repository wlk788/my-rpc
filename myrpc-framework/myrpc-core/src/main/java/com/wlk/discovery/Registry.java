package com.wlk.discovery;

import com.wlk.ServiceConfig;

import java.net.InetSocketAddress;
import java.util.List;

public interface Registry {
    /**
     * 注册服务
     * @param serviceConfig 服务的配置内容
     */
    void registry(ServiceConfig<?> serviceConfig);

    /**
     * 从注册中心拉取服务列表
     * @param serviceName 服务的名称
     * @param group
     * @return 服务地址
     */
    List<InetSocketAddress> lookup(String serviceName, String group);
}
