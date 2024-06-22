package com.wlk.discovery;

import com.wlk.Constant;
import com.wlk.discovery.impl.NacosRegistry;
import com.wlk.discovery.impl.ZookeeperRegistry;
import com.wlk.exceptions.DiscoveryException;

import java.util.Locale;

public class RegistryConfig {

    //连接地址 zookeeper://127.0.0.1:2181
    private final String connectString;
    public RegistryConfig(String connectString) {
        this.connectString = connectString;
    }

    public Registry getRegistry(){
        //1、获取注册中心的类型
        String registryType = getRegistryType(connectString, true).toLowerCase().trim();
        //2、通过类型获取具体的注册中心
        if(registryType.equals("zookeeper")){
            String host = getRegistryType(connectString, false);
            return new ZookeeperRegistry(host, Constant.TIME_OUT);
        }
        else if(registryType.equals("nacos")){
            String host = getRegistryType(connectString, false);
            return new NacosRegistry(host, Constant.TIME_OUT);
        }
        throw new DiscoveryException("未发现合适的注册中心。");
    }

    private String getRegistryType(String connectString, boolean ifType){
        String[] typeAndHost = connectString.split("://");
        if(typeAndHost.length != 2){
            throw new RuntimeException("给定的注册中心连接url不合法");
        }
        if(ifType){
            return typeAndHost[0];
        }
        else{
            return typeAndHost[1];
        }
    }
}
