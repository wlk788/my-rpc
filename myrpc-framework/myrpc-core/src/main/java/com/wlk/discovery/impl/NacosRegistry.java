package com.wlk.discovery.impl;

import com.wlk.ServiceConfig;
import com.wlk.discovery.Registry;

import java.net.InetSocketAddress;
import java.util.List;

public class NacosRegistry implements Registry {

    private String host;
    private int timeOut;

    public NacosRegistry(String host, int timeOut) {
        this.host = host;
        this.timeOut = timeOut;
    }
    @Override
    public void registry(ServiceConfig<?> serviceConfig) {

    }

    @Override
    public List<InetSocketAddress> lookup(String serviceName, String group) {
        return null;
    }
}
